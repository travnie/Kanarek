import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";

// Inspect the staged portable page, then exercise its pinned source API.
const html = readFileSync(0, "utf8");
const has = (id) => html.includes(`id="${id}"`);
if (has("files-workspace")) {
  const source = process.argv[2];
  assert.ok(source, "Files-first Docbench requires a pinned source module path");
  assert.match(html, /nativeIngress\s*=\s*createFileIngress\s*\(\s*store\s*,/);
  assert.match(html, /globalThis\.DocBenchFiles\s*=\s*Object\.freeze\s*\(\s*\{\s*\.\.\.nativeIngress\s*,/);
  const { createFileIngress, createFileStore } = await import(pathToFileURL(source).href);
  const store = createFileStore({ limit: 1 });
  let activated = false;
  const ingress = createFileIngress(store, {
    resetView: () => {},
    activateFiles: () => { activated = true; },
  });
  const file = { name: "notes.txt", size: 4, type: "text/plain", slice: () => new Blob(["test"]) };
  const result = ingress.importFile(file);
  assert.equal(ingress.version, 1, "Pinned Docbench Files ingress version mismatch");
  assert.equal(result.ok, true, "Pinned Docbench Files ingress rejected a normal file");
  assert.equal(store.activeId(), result.id);
  assert.equal(activated, true);
  assert.equal(ingress.importFile(file).ok, false, "Full Files queue should reject imports");
  console.log("Docbench Files ingress v1 verified");
} else {
  assert.ok(!has("files-tab"), "Files-first Docbench cannot use legacy picker fallback");
  for (const id of ["file-input", "document-tab", "pdf-input", "docx-to-pdf-input", "pdf-tab"]) {
    assert.ok(has(id), `Pinned legacy Docbench lost import control: ${id}`);
  }
  console.log("Legacy Docbench picker ingress verified");
}
