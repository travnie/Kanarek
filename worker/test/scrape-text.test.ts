import { afterEach, describe, expect, it, vi } from "vitest";
import { extractItems } from "../src/index";

class FakeElement {
  private end: (() => void) | null = null;
  constructor(private readonly attrs: Record<string, string> = {}) {}
  getAttribute(name: string) { return this.attrs[name] ?? null; }
  onEndTag(callback: () => void) { this.end = callback; }
  close() { this.end?.(); }
}

class FakeHTMLRewriter {
  protected handlers = new Map<string, any>();
  on(selector: string, handler: any) { this.handlers.set(selector, handler); return this; }
  transform(response: Response) {
    const item = new FakeElement();
    const link = new FakeElement({ href: "/story" });
    const heading = new FakeElement();
    const paragraph = new FakeElement();
    this.handlers.get(".card")?.element?.(item);
    this.handlers.get(".card a")?.element?.(link);
    const h = this.handlers.get(".card h2");
    h?.element?.(heading);
    for (const text of ["Before ", "bold", " after"]) h?.text?.({ text, lastInTextNode: true });
    heading.close();
    const p = this.handlers.get(".card p");
    p?.element?.(paragraph);
    for (const text of ["First ", "emphasis", " last"]) p?.text?.({ text, lastInTextNode: true });
    paragraph.close();
    item.close();
    return response;
  }
}


class FakeOmittedParagraphEndRewriter extends FakeHTMLRewriter {
  transform(response: Response) {
    const handlers = (this as any).handlers as Map<string, any>;
    const item = new FakeElement();
    const link = new FakeElement({ href: "/story" });
    const first = new FakeElement();
    const second = new FakeElement();
    handlers.get(".card")?.element?.(item);
    handlers.get(".card a")?.element?.(link);
    handlers.get(".card a")?.text?.({ text: "Story", lastInTextNode: true });
    const p = handlers.get(".card p");
    p?.element?.(first);
    p?.text?.({ text: "One", lastInTextNode: true });
    p?.element?.(second);
    p?.text?.({ text: "Two", lastInTextNode: true });
    second.close();
    first.close();
    item.close();
    return response;
  }
}

afterEach(() => vi.unstubAllGlobals());

describe("scraper nested text", () => {
  it("captures text until the heading and paragraph end tags", async () => {
    vi.stubGlobal("HTMLRewriter", FakeHTMLRewriter);
    const items = await extractItems("<ignored>", ".card", "https://example.com/page");
    expect(items).toEqual([{ title: "Before bold after", link: "https://example.com/story", summary: "First emphasis last", image: null }]);
  });

  it("stops the first summary when another paragraph starts without an end callback", async () => {
    vi.stubGlobal("HTMLRewriter", FakeOmittedParagraphEndRewriter);
    const items = await extractItems("<ignored>", ".card", "https://example.com/page");
    expect(items[0]?.summary).toBe("One");
  });
});
