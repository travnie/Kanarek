[Polski](README_pl.md) · **English** · [简体中文](README_zh.md)

<div align="center">

<img src="assets/kanarek.svg" alt="Kanarek" width="96">

# Kanarek

**Android news reader and widget with a background radio/IPTV player.**

[![android CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/android-ci.yml?label=android%20CI&logo=android&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/android-ci.yml)
[![worker CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/worker-ci.yml?label=worker%20CI&logo=cloudflare&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/worker-ci.yml)
[![last commit](https://img.shields.io/github/last-commit/travnie/kanarek?color=FFC107&logo=git&logoColor=111&style=flat-square)](https://github.com/travnie/kanarek/commits/main)
[![license](https://img.shields.io/github/license/travnie/kanarek?color=FFC107&style=flat-square)](LICENSE)<br>
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white) ![Cloudflare Worker](https://img.shields.io/badge/Worker-F38020?style=flat-square&logo=cloudflareworkers&logoColor=white)<br>
<a href="https://deepwiki.com/travnie/kanarek"><img src="https://deepwiki.com/badge.svg" alt="DeepWiki"></a>

</div>

Kanarek brings three surfaces into one native Android app:

- **News:** RSS/Atom reader, offline articles, notifications and a home-screen slideshow widget.
- **Radio & TV:** background streaming, playlists, Cast and a player widget.
- **Tools:** Codebench, Docbench and Streambench for QR codes, documents and playlists, with bundled offline web tools in CI-built APKs.

An optional Cloudflare Worker accelerates fetching and provides additional network features. It is not required: ordinary RSS/Atom feeds can be parsed directly on the device.

## Highlights

### News

- resizable slideshow widget with manual navigation and per-widget settings,
- custom RSS 2.0 and Atom sources with OPML import and export,
- local search, source filters, and an optional headline-ranking mode,
- read state, saved articles, and optional offline clean text,
- clean article preview when a trusted Worker backend is configured,
- optional new-story notifications with quiet hours,
- last-known-good stories stay visible when an individual source temporarily fails.

### Reader data and recovery

- storage screen with feed/image cache sizes and manual cleanup,
- per-article read/hidden/saved history with optional offline content,
- portable backup and restore for reader settings, feeds, stations and saved articles (read/hidden history is not included).

### Radio and IPTV

- background Media3/ExoPlayer playback with system media controls,
- M3U/M3U8 import, export, and editing,
- radio, television, channel groups, favorites, and now-playing metadata,
- station discovery through the Radio Browser directory,
- missing channel logos filled through iptv-org and favicon fallbacks,
- playlist-provided `User-Agent` and `Referer` support,
- Google Cast in the `play` flavor; the `foss` flavor is Google-services-free.

### Tools and Benches

- **Codebench:** QR/barcode creation and scanning, including a Quick Settings scan tile and confirmation before external actions.
- **Docbench:** edit, convert and export supported documents and text files.
- **Streambench:** inspect playlists, XMLTV and stream diagnostics without replacing Kanarek's player or station store.
- Three pinned portable Bench builds ship inside CI release/preview APKs and open via isolated HTTPS-like WebView asset origins. Each Bench also offers an optional hosted mode for network-dependent features.
- Open supported files from Android's **Open with** or **Share** flow, choose the destination Bench, and export results using the system document picker. Native imports are bounded and read-only; the original file is not overwritten.

Offline availability applies to bundled tools, not remote streams, online providers or hosted mode. See [Tools integration](docs/TOOLS_INTEGRATION.md) for limitations and trust boundaries.

## Install

[APK](https://github.com/travnie/kanarek/releases).

- `play`: includes Google Cast support,
- `foss`: GMS-free build intended for FOSS and F-Droid environments.

The minimum supported system is Android 8.0 (API 26).

## Quick start

1. Install the preferred APK flavor.
2. Open Kanarek and choose **News**, **Radio & TV**, or **Tools**.
3. Add RSS/Atom sources or import an OPML file.
4. Add stations manually, search the radio directory, or import an M3U/M3U8 playlist.
5. Open **Tools** to use a Bench, or share a supported file to Kanarek; add the **Scan** Quick Settings tile for QR scanning.
6. Long-press the Android home screen, open **Widgets**, and add a Kanarek news or player widget.

The Backend URL may remain blank: regular feed refreshes stay on-device, while feed discovery, station search, and logo lookup can use Kanarek's built-in default service. Set your own Worker URL only when you want normal feed refreshes routed through that Worker or operator-controlled features such as clean-reader extraction and synchronized state.

## Documentation

- [Tools, Benches and file sharing](docs/TOOLS_INTEGRATION.md)
- [Architecture and integration boundaries](docs/ARCHITECTURE.md)
- [Development, tests and releases](docs/DEVELOPMENT.md)
- [Cloudflare Worker API](docs/WORKER.md)
- [Project history](docs/HISTORY.md)
- [F-Droid submission notes](docs/FDROID.md)

Kanarek was developed inside [trvny/feeds](https://github.com/trvny/feeds) until August 2026 and
was extracted into this repository with its full history.

## Development

The Gradle wrapper scripts are intentionally not committed. On a fresh clone, install the exact Gradle version declared by `gradle/wrapper/gradle-wrapper.properties`, then bootstrap the wrapper before using it:

```bash
GRADLE_VERSION=$(sed -n 's#^distributionUrl=.*/gradle-\([0-9][A-Za-z0-9.-]*\)-\(bin\|all\)\.zip$#\1#p' gradle/wrapper/gradle-wrapper.properties)
command -v gradle >/dev/null || { echo "Install Gradle $GRADLE_VERSION first" >&2; exit 1; }
gradle --version | grep -F "Gradle $GRADLE_VERSION" >/dev/null || { echo "Use Gradle $GRADLE_VERSION to bootstrap the wrapper" >&2; exit 1; }
gradle wrapper --gradle-version "$GRADLE_VERSION" --no-daemon
./gradlew assembleDebug
./gradlew testPlayDebugUnitTest
```

To include the bundled offline Benches in a local APK, install Node 24, git, Bash 4+ and `sha256sum`, then stage the pinned assets **before** assembling:

```bash
bash .github/scripts/stage-benches.sh
./gradlew assembleDebug
```

## [License](LICENSE)

[![License](https://www.shieldcn.dev/github/license/travnie/kanarek.svg?variant=branded&size=xm&mode=light&theme=neutral&font=jetbrains-mono)](https://spdx.org/licenses/MIT)

---
## 💬 Quote from the drawer
<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝The first ever hard disk drive was made in 1979, and could hold only 5MB of data.❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Recently on the air
<!--README_FEED:START-->
- [Trust but Verify: Implementing Frontier AI Policy in California and Beyond](https://carnegieendowment.org/research/2026/11/trust-but-verify-implementing-frontier-ai-policy-in-california-and-beyond)
- [Bridging the Gap Between Climate Mobility Modeling and Policy Action](https://carnegieendowment.org/research/2026/11/bridging-the-gap-between-climate-mobility-modeling-and-policy-action)
- [The Long War: Rethinking Defence Strategy in Prolonged Conflict](https://www.rusi.org/news-and-comment/video-commentary/long-war-rethinking-defence-strategy-prolonged-conflict)
- [COP31 and the Future of Climate Clubs](https://carnegieendowment.org/emissary/2026/11/cop31-climate-clubs-us-withdraw)
- [Jaworzno przygotowuje się do zimy. Ponad 800 ton soli i 16 pojazdów w gotowości - jaw.pl](https://news.google.com/atom/articles/CBMiY0FVX3lxTE9qOWcybGgxQVpOMjJIMVpja0RQVkUySnVHXzNCZk9SRGFZY2M1R2RBUFhRRjVTanJlZndONkFUNEhsZHFfVWlVU2pUOFBaaVFTZjVvMWdrdGxjNmxUTHcxOWtfVQ?oc=5)
- [Qualcomm's smartphone chip sales miss estimates, shares fall](https://www.reuters.com/business/media-telecom/qualcomm-forecasts-upbeat-sales-strength-non-smartphone-segments-2026-11-04/)
<!--README_FEED:END-->
