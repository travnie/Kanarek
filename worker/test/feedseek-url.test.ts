import { describe, it, expect } from "vitest";
import { feedseekFetchUrl } from "../src/index";

describe("feedseekFetchUrl", () => {
  it("maps raw main snapshots and legacy Pages names to short Pages URLs", () => {
    expect(feedseekFetchUrl("https://raw.githubusercontent.com/trvny/feedseek/main/feeds/feed_pap.xml"))
      .toBe("https://trvny.github.io/feedseek/pap.xml");
    expect(feedseekFetchUrl("https://trvny.github.io/feedseek/feed_wykop.xml"))
      .toBe("https://trvny.github.io/feedseek/wykop.xml");
  });

  it("leaves other URLs untouched", () => {
    for (const url of [
      "https://antyweb.pl/feed/",
      "https://trvny.github.io/feedseek/pap.xml",
      "https://raw.githubusercontent.com/other/repo/main/feeds/feed_x.xml",
    ]) expect(feedseekFetchUrl(url)).toBe(url);
  });
});
