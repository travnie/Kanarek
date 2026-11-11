import { describe, expect, it } from "vitest";
import { readLimitedJson } from "../src/state-guards";

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
