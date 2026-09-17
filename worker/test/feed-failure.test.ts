import { afterEach, describe, expect, it, vi } from "vitest";
import { handleFeeds } from "../src/index";

const GOOD_RSS = `<?xml version="1.0"?><rss version="2.0"><channel><title>Good</title>
  <item><title>Fresh</title><link>https://good.test/article</link></item>
</channel></rss>`;

function context() {
  const waits: Promise<unknown>[] = [];
  return {
    waits,
    ctx: {
      waitUntil(promise: Promise<unknown>) { waits.push(promise); },
      passThroughOnException() {},
    } as unknown as ExecutionContext,
  };
}

function cacheStub() {
  const put = vi.fn(async () => undefined);
  const match = vi.fn(async () => undefined);
  vi.stubGlobal("caches", { default: { match, put } });
  return { match, put };
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("feed refresh failure contract", () => {
  it("returns 502 and does not cache when every upstream fails", async () => {
    const cache = cacheStub();
    vi.stubGlobal("fetch", vi.fn(async () => new Response("down", { status: 503 })));
    const { ctx, waits } = context();
    const url = new URL("https://worker.test/?feeds=https://a.test/feed,https://b.test/feed");

    const response = await handleFeeds(new Request(url), url, {}, ctx);
    const body = await response.json() as { error: string; sources: Array<{ url: string; status: string }> };
    await Promise.all(waits);

    expect(response.status).toBe(502);
    expect(response.headers.get("cache-control")).toBe("no-store");
    expect(body.error).toBe("all feeds failed");
    expect(body.sources.map((source) => source.status)).toEqual(["error", "error"]);
    expect(cache.put).not.toHaveBeenCalled();
  });

  it("returns and caches partial results with per-source status", async () => {
    const cache = cacheStub();
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input));
      return url.hostname === "good.test"
        ? new Response(GOOD_RSS, { status: 200 })
        : new Response("down", { status: 503 });
    }));
    const { ctx, waits } = context();
    const url = new URL("https://worker.test/?feeds=https://good.test/feed,https://bad.test/feed");

    const response = await handleFeeds(new Request(url), url, {}, ctx);
    const body = await response.json() as { count: number; sources: Array<{ url: string; status: string }> };
    await Promise.all(waits);

    expect(response.status).toBe(200);
    expect(body.count).toBe(1);
    expect(body.sources).toEqual([
      { url: "https://good.test/feed", status: "ok" },
      { url: "https://bad.test/feed", status: "error" },
    ]);
    expect(cache.put).toHaveBeenCalledTimes(1);
  });

  it("keeps a genuinely empty successful feed as a 200", async () => {
    const cache = cacheStub();
    const emptyRss = `<?xml version="1.0"?><rss version="2.0"><channel><title>Empty</title></channel></rss>`;
    vi.stubGlobal("fetch", vi.fn(async () => new Response(emptyRss, { status: 200 })));
    const { ctx, waits } = context();
    const url = new URL("https://worker.test/?feeds=https://empty.test/feed");

    const response = await handleFeeds(new Request(url), url, {}, ctx);
    const body = await response.json() as { count: number; sources: Array<{ status: string }> };
    await Promise.all(waits);

    expect(response.status).toBe(200);
    expect(body.count).toBe(0);
    expect(body.sources).toEqual([{ url: "https://empty.test/feed", status: "ok" }]);
    expect(cache.put).toHaveBeenCalledTimes(1);
  });
});
