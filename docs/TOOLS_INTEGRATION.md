# Tools and Benches

Kanarek's **Tools** destination embeds Codebench, Docbench and Streambench while keeping the upstream web products and their Workers in [travnie/twojstar/benches](https://github.com/travnie/twojstar/tree/main/benches). This is the implementation guide, not an unmerged-PR plan.

## What works

| Tool | Purpose | Native integration |
| --- | --- | --- |
| Codebench | QR/barcode generation and scanning | Camera permission; scan result preview; Quick Settings Scan tile |
| Docbench | PDF/DOCX and text/config editing or conversion | Open/share document and export using Android picker |
| Streambench | M3U inspection, XMLTV and stream diagnostics | Open/share playlists; provider/relay operations remain online |

The third page is in the existing Compose navigation. Playback stays in Kanarek's `PlayerService` and stored stations remain Kanarek-owned; Streambench is a workshop, **not** another player or station database.

## Offline and online

`.github/scripts/stage-benches.sh` builds verified single-file portable assets from **one pinned twojstar commit**. Android CI and release pipelines stage them before APK packaging. Assets are kept out of Git. Locally, stage first with Node 24, npm, git, Bash 4+ and `sha256sum`; without the assets, Kanarek opens the hosted Bench pages directly (no offline/hosted switch).

Each bundled Bench has its own HTTPS-like `WebViewAssetLoader` origin, e.g. `https://codebench.appassets.androidplatform.net/bench/portable.html`. Embedded content has restricted navigation and network requests. The user may explicitly switch to hosted mode when bundled; without bundled assets there is no switch or confirmation. Hosted pages are network-dependent and do not provide the same local-only privacy boundary. Streambench online provider/relay functionality uses its existing constrained Worker.

## Files and exports

- Android `ACTION_VIEW` / `ACTION_SEND` imports use temporary URI grants and a **native chooser**, then send only the chosen content to the selected Bench.
- Small imported files are read-only and limited to **4 MiB**; plain-text shares are accepted. File types route to the corresponding Bench, with a user choice for ambiguous files such as M3U or XML.
- After the user selects a destination, a bounded import script is injected via `evaluateJavascript` only into that Bench's trusted bundled or hosted HTTPS origin. Hosted pages may make their own network requests; using them does not guarantee local-only handling.
- Bundled Blob exports use a chunked receiver with a **32 MiB** limit and `ACTION_CREATE_DOCUMENT`. The user picks where to save; the source URI is not silently overwritten.
- The scanner interprets common QR payloads (URLs, Wi-Fi, contacts, calendar events, calls/messages, location and text). External actions need an explicit tap. Sensitive Wi-Fi passwords are masked in the preview.
- The Scan Quick Settings tile uses a one-use token before automatic camera opening. No QR content is silently stored or sent to a Worker.

For first-party native imports, the companion upstream change
[travnie/twojstar#460](https://github.com/travnie/twojstar/pull/460)
introduces Docbench v1 `DocBenchFiles.importFile(File)`: a synchronous acceptance result
`{ ok, id? }` that opens the canonical Files workspace. The import is
browser-local and does not select an editor or silently overwrite existing
content. Older pinned portable builds use their legacy input IDs, with
the Documents/PDF tab activated before dispatch. The staging script checks
the pinned portable HTML against this compatibility boundary on each build.
A Files-first portable update without the v1 contract must not ship.

The native WebView bridge is deliberately constrained: `KanarekExport` is bundled-main-frame-only; Codebench's `KanarekScan` accepts its bundled or exact hosted origin. No general-purpose JS interface for unrelated websites, broad filesystem permissions or unbounded downloads. The bundled path follows AndroidX's `WebViewAssetLoader` HTTPS-origin model.

## Constraints and follow-up validation

**Export or save before leaving a Bench.** Switching to News or Radio & TV, or returning to the Tools hub, destroys the Bench WebView and discards unsaved in-memory edits.

- Bundled tools can work offline; remote stream playback, Streambench providers/relay and hosted mode require connectivity.
- Keep upstream Bench source, deployment and test ownership in `twojstar`. Update the single staging pin and all hashes together, never the generated HTML by hand.
- Import is not an implicit merge into Kanarek's stations. Preserve original playlist directives; Kanarek's M3U codec is not a lossless Streambench replacement.
- Real-device checks are still needed for QR camera, PDF/DOCX export, playlist fidelity, WebView memory, back navigation, sharing and both release flavors.
- A shortcut/widget for opening Tools or documents, and richer cross-Bench handoffs, are future work; not advertised as shipped features.
- CI's `kanarek-preview-apk` artifact is `com.kanarek.preview` and can be installed beside the signed release. The ordinary `kanarek-debug-apk` cannot update a differently signed release.

Relevant code: `HomeActivity.kt`, `ToolsScreen.kt`, `OfflineBenches.kt`, `IncomingBenchFile.kt`, `BenchExports.kt`, `ScanTileService.kt` and `.github/scripts/stage-benches.sh`. See [Architecture](ARCHITECTURE.md) and [Development](DEVELOPMENT.md).
