import { afterEach, describe, expect, it, vi } from "vitest";
import { allowPairClaim, readLimitedJson } from "../src/state-guards";

describe("bounded state JSON", () => {
  it("rejects UTF-8 bodies by bytes rather than characters", async () => {
    const request = new Request("https://worker.test/state/read", { method: "POST", body: "éé" });
    await expect(readLimitedJson(request, 3)).rejects.toThrow("payload too large");
  });
  it("decodes normal JSON and preserves empty bodies", async () => {
    const json = new Request("https://worker.test/state/subs", { method: "PUT", body: '{"feeds":["a"]}' });
    expect(await readLimitedJson(json, 100)).toEqual({ feeds: ["a"] });
    const empty = new Request("https://worker.test/state/read", { method: "POST" });
    expect(await readLimitedJson(empty, 100)).toEqual({});
  });
});

/** Simulates D1's atomic conditional UPSERT without a Cloudflare runtime. */
function rateLimitDb() {
  const state = new Map<string, { bucket: number; count: number }>();
  const sqls: string[] = [];
  const db = {
    prepare(sql: string) {
      sqls.push(sql);
      return {
        bind(...args: unknown[]) {
          return {
            async first() {
              const [key, bucket, max] = args as [string, number, number];
              const previous = state.get(key);
              if (!previous || previous.bucket < bucket) {
                state.set(key, { bucket, count: 1 });
                return { attempts: 1 };
              }
              if (previous.bucket === bucket && previous.count < max) {
                previous.count++;
                return { attempts: previous.count };
              }
              return null;
            },
            async run() { return {}; },
          };
        },
      };
    },
  } as unknown as D1Database;
  return { db, sqls };
}

describe("pair claim attempts", () => {
  afterEach(() => vi.restoreAllMocks());

  it("limits guesses per client and resets in the next window", async () => {
    const clock = vi.spyOn(Date, "now").mockReturnValue(1_000);
    vi.spyOn(Math, "random").mockReturnValue(1);
    const { db } = rateLimitDb();
    const req = new Request("https://worker.test/pair/ABCDEF", {
      headers: { "cf-connecting-ip": "203.0.113.40" },
    });
    for (let i = 0; i < 3; i++) expect(await allowPairClaim(req, db, 300_000, 3)).toBe(true);
    expect(await allowPairClaim(req, db, 300_000, 3)).toBe(false);
    clock.mockReturnValue(301_000);
    expect(await allowPairClaim(req, db, 300_000, 3)).toBe(true);
  });

  it("does not allow a delayed previous-window claim to reset a newer counter", async () => {
    const clock = vi.spyOn(Date, "now").mockReturnValue(301_000);
    vi.spyOn(Math, "random").mockReturnValue(1);
    const { db, sqls } = rateLimitDb();
    const req = new Request("https://worker.test/pair/ABCDEF", {
      headers: { "cf-connecting-ip": "203.0.113.41" },
    });
    expect(await allowPairClaim(req, db, 300_000, 1)).toBe(true);
    clock.mockReturnValue(1_000);
    expect(await allowPairClaim(req, db, 300_000, 1)).toBe(false);
    expect(sqls[0]).toContain("window_bucket < excluded.window_bucket");
  });
});
