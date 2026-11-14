[Polski](README_pl.md) · **English** · [简体中文](README_zh.md)

<div align="center">

<img src="assets/kanarek.svg" alt="Kanarek" width="96">

# Kanarek

**Android news reader and widget with a background radio/IPTV player.**

[![android CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/android-ci.yml?label=android%20CI&logo=android&logoColor=white&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/android-ci.yml)
[![worker CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/worker-ci.yml?label=worker%20CI&logo=cloudflare&logoColor=white&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/worker-ci.yml)
[![last commit](https://img.shields.io/github/last-commit/travnie/kanarek?color=FFC107&logo=git&logoColor=111&style=flat-square)](https://github.com/travnie/kanarek/commits/main)
[![license](https://img.shields.io/github/license/travnie/kanarek?color=FFC107&logo=opensourceinitiative&logoColor=white&style=flat-square)](https://spdx.org/licenses/MIT)<br>
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/) [![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/) [![Cloudflare Worker](https://img.shields.io/badge/Worker-F38020?style=flat-square&logo=cloudflareworkers&logoColor=white)](https://developers.cloudflare.com/workers/)<br>
<a href="https://deepwiki.com/travnie/kanarek"><picture><source srcset="https://img.shields.io/badge/DeepWiki-travnie%2FKanarek-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==" type="image/svg+xml"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></picture></a>
![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/travnie/Kanarek?utm_source=oss&utm_medium=github&utm_campaign=travnie%2FKanarek&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

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
<i>❝“If you think your users are idiots, only idiots will use it.”— Linus Torvalds❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Recently on the air
<!--README_FEED:START-->
- [How Climate Change Is Reshaping India’s Demographic Dividend](https://carnegieendowment.org/research/2026/11/how-climate-change-is-reshaping-indias-demographic-dividend)
- [Naresh Bhatt Convicted: The Road to Justice for Mamta's Family | Case by Case | "48 Hours" Podcast](https://www.cbsnews.com/video/111326-48-hours-case-by-case-episode-30/)
- [FAA issues nationwide ground stop, stranding thousands](https://www.cbsnews.com/news/faa-issues-nationwide-ground-stop/)
- [Flights grounded across US after air traffic communications outage](https://www.reuters.com/world/us/faa-issues-national-ground-stop-due-extraordinary-circumstance-reasons-safety-2026-11-13/)
- [Passengers rescued after tanker hit river cruise in New Orleans](https://www.cbsnews.com/news/tanker-cruise-collision-mississippi-river-new-orleans/)
- [EXCLUSIVE: Investors flock to Anthropic IPO but question durability of AI lead](https://www.reuters.com/business/finance/investors-flock-anthropic-ipo-question-durability-ai-lead-2026-11-13/)
<!--README_FEED:END-->
