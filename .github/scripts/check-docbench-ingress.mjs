import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// Source-of-truth boundary: old two-tab Docbench or v1 Files ingress.
const html = readFileSync("app/src/main/assets/benches/docbench/portable.html", "utf8");
const has = (id) => html.includes(`id="${id}"`);
const v1 = html.includes("DocBenchFiles = Object.freeze({")
  && html.includes("importFile: (file) => importLocalFile(store, file,")
  && html.includes("version: 1");
if (!v1) {
  for (const id of ["file-input", "document-tab", "pdf-input", "docx-to-pdf-input", "pdf-tab"]) {
    assert.ok(has(id), `Pinned Docbench lost legacy import control: ${id}`);
  }
}
if (has("files-tab")) {
  assert.ok(v1, "Files-first Docbench must expose the canonical importFile v1 contract.");
}
console.log(v1 ? "Docbench native ingress v1 verified" : "Legacy Docbench picker ingress verified");
