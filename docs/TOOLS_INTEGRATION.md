# Tools and Bench integration

Status: in progress. This is the canonical plan for the Kanarek Tools surface.
Bench web sources and their Cloudflare Workers remain in
[travnie/twojstar/benches](https://github.com/travnie/twojstar/tree/main/benches).
Kanarek is a consumer, not a fork or a replacement deployment.

## Goal

One Android application with News, Radio & TV, and Tools. Keep existing Kanarek
Media3 playback, reader state, widgets, play/foss variants and Kotlin Multiplatform
domain boundary. Expose Codebench, Docbench and Streambench with minimal changes
to their existing browser UIs. Native Compose owns navigation and system services.

## Responsibilities

| Owner | Responsibilities |
| --- | --- |
| Kanarek | News; Media3 player, stations, favorites, background playback and Cast; Android integration, widgets, file picker, share targets |
| Codebench | QR/barcode creation and scanning |
| Docbench | PDF/DOCX and local text/config editing, transformations and export |
| Streambench | Diagnostics, XMLTV, providers and fidelity-preserving M3U editing and inspection |
| twojstar | Source, tests, portable builds and three independent Workers |

Streambench and Kanarek overlap on streaming and playlists. Preserve one
player and one Android station store: Kanarek's PlayerService is authoritative.
Streambench acts as a workshop. Import selected entries into Kanarek only with
user confirmation. Do not round-trip arbitrary playlists through Kanarek's
lossy M3uCodec when Streambench can preserve original directives. Do not
replace Streambench's allowlisted relay with an unrestricted proxy.

## Delivery slices

### 1. Native Tools hub (initial PR)

- Add a third Compose destination reachable via the existing pager, bottom bar
  and drawer; keep reader/player navigation and MediaSession lifetime intact.
- Open each existing **hosted** Bench inside an isolated WebView on selection.
  Enable Android camera permission for trusted Codebench origin and OS file
  picking for user-selected inputs. No JavaScript interface or broad storage
  permission. Block mixed content and external in-WebView navigation.
- Hosted mode is a transitional preview: it requires a network connection;
  browser downloads and all workflows are not yet guaranteed inside WebView.
  Switching away unloads the preview, including unsaved in-memory edits, to
  stop hidden camera/media activity. Save or export first; this tradeoff can be
  revisited when bundled sandboxes and native file state are implemented.
  Do not describe it as offline-capable or native-export-complete.
- Build both `play` and `foss` variants, exercise navigation/back, picker,
  scanner permissions and offline reader/player regression checks.

### 2. Bundled portable web assets

- Reuse each Bench's existing `public/portable.html` build; maintain one
  source of truth in twojstar. Pin a source revision/artifact manifest in
  Kanarek build configuration; never pull a mutable `latest` silently.
- Stage packages through verified CI input, serve bundled resources under a
  dedicated HTTPS-like app origin via AndroidX WebViewAssetLoader rather than
  `file://`. Keep each Bench's storage namespace isolated.
- Test JS modules/import maps, workers, font and WASM assets in actual Android
  WebView. Load Docbench lazily to bound memory and avoid degrading the player.
- Online Streambench provider/relay routes continue to use its existing
  restricted Worker. The other Bench Workers remain untouched.

### 3. Native file and sharing contract

First pass on `feat/open-with-benches`:

- Android `ACTION_VIEW` / `ACTION_SEND` accepts user-shared `content://`
  file grants and shows a native route chooser. Plain-text shares without file
  streams are handled as temporary text documents. No broad storage access.
  Consumed intents are cleared so recreation does not replay imports.
- Images route to Codebench's existing decode input, text/config files to
  Docbench's document input, M3U/M3U8 to Streambench's playlist input;
  M3U can also open as raw text in Docbench. XML can optionally be treated as
  XMLTV after selecting Streambench, never silently. PLS/XSPF are currently
  editable in Docbench; Streambench's parser does not yet support them.
- The hosted preview loads at most 4 MiB in memory, passes base64-encoded data
  only into the selected first-party HTTPS origin after explicit confirmation,
  then drops the temporary data. No backend upload is performed by Kanarek;
  the hosted page itself remains part of the user's trust decision.
- PDF and DOCX have dedicated Docbench inputs for small previews. Larger files
  and files unsupported by the Android MIME chooser still use Bench's own
  system picker. Robust large-file/offline handoff requires bundled assets and
  native document streams.
- The original file is read-only during import. Changes in the Bench are not
  silently written back to the source URI; export/save is a separate action.



- Complete Android Storage Access Framework open/create/save without blanket
  disk permission. External `ACTION_VIEW` / `ACTION_SEND` with URI grants is
  covered by the current preview importer; in-place save/export is not.
- A versioned, typed and explicitly allowlisted bridge between *bundled*
  Bench pages and Android. No unrestricted `addJavascriptInterface` exposed
  to hosted or third-party pages; verify main frame origin, payload size,
  type and user intent. No silent data transfer to Workers.
- Explicit export/save flow, including Blob downloads. Persist permission only
  when the picker and URI flags allow it. Cancel/cleanup safely.
- Workflows: scan -> copy/save .txt -> Docbench; document/URL -> Codebench QR;
  M3U from Docbench -> Streambench inspect -> selected stations -> Kanarek;
  current Kanarek station -> Streambench diagnostics or QR.
- Validate URL schemes, scan outputs and MIME/content independently; never
  auto-open scanned links or import untrusted remote playlists.

### 4. Android shortcuts

Implementation underway on `feat/scan-tile-actions`:
- Quick Tile uses a one-use short-lived token to authorize camera autostart,
  so forged external Activity intents cannot silently request capture.
- Quick Tile launches Codebench scan and previews structured results via an
  origin-restricted WebMessageListener, not unrestricted JS interfaces.
- Native parser classifies WIFI, vCard/MECARD, VEVENT, HTTPS links, phone, SMS,
  mailto, geo, playlists, and unknown text without taking action.
- Intent actions require an explicit tap: Wi-Fi invokes Android's network
  confirmation UI on API 30+ (settings fallback otherwise); contacts and
  events open editable system insert screens; SMS and calls never auto-send/dial;
  URLs are limited to HTTPS and never open on detection; raw text may be
  copied or exported to a user-chosen document.
- Sensitive Wi-Fi passwords are hidden in the scan preview. No QR payload
  is persisted or sent to a Worker. Stop camera after a scan.
- Test pure parsing in shared KMP tests and verify both Android flavors.

### Planned shortcuts

- Small launcher widget with Scan / Open document / Tools shortcuts, implemented
  with safe RemoteViews and widget inflation tests.
- Do not block startup or start a background media service for the scanner.

### 5. UI refinements and consolidation

- Compose Material 3 shell/theme only; no forced React, Capacitor or migration
  of existing Bench UIs. Floating UI may enhance *Bench-side* tooltips,
  dropdowns and popovers where positioning is genuinely needed.
- Keep Streambench browser standalone and its Worker intact. In Kanarek,
  route final playback to Media3; avoid duplicate station collections,
  player instances, history or favorites.
- Test dark/light mode, small screens, process death, rotation, offline
  behavior, memory use, playback handoff and Play/FOSS CI.

## Acceptance criteria

- One Kanarek APK for each existing flavor. Benches still work at their
  independent hosted URLs and can be built/released separately in twojstar.
- Existing news and radio features regress neither offline nor in background.
- User-selected local files stay on device unless deliberately shared.
- QR scanner works from app and Quick Tile after explicit camera permission.
- File transfers preserve original content and playlist directives; confirmed
  imports never overwrite Kanarek's collection silently.
- Bench UI failures do not crash or erase Kanarek state; no secrets,
  unconstrained Workers relay or duplicate station/player source of truth.

## Related code and references

- `app/src/main/java/com/kanarek/HomeActivity.kt`: native navigation
- `app/src/main/java/com/kanarek/ui/ToolsScreen.kt`: Tools surface
- `app/src/main/java/com/kanarek/player/PlayerService.kt`: single player
- `shared/src/commonMain/kotlin/com/kanarek/data/M3uCodec.kt`: Kanarek subset
- [twojstar Benches](https://github.com/travnie/twojstar/tree/main/benches)
- [Kanarek architecture](ARCHITECTURE.md)
