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

Kanarek combines two tools in one native application:

- an RSS/Atom reader with an auto-rotating home-screen news widget,
- a background internet radio and IPTV player with its own transport-control widget.

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

### Radio and IPTV

- background Media3/ExoPlayer playback with system media controls,
- M3U/M3U8 import, export, and editing,
- radio, television, channel groups, favorites, and now-playing metadata,
- station discovery through the Radio Browser directory,
- missing channel logos filled through iptv-org and favicon fallbacks,
- playlist-provided `User-Agent` and `Referer` support,
- Google Cast in the `play` flavor; the `foss` flavor is Google-services-free.

## Install

[APK](https://github.com/travnie/kanarek/releases).

- `play`: includes Google Cast support,
- `foss`: GMS-free build intended for FOSS and F-Droid environments.

The minimum supported system is Android 8.0 (API 26).

## Quick start

1. Install the preferred APK flavor.
2. Open Kanarek and choose **News** or **Radio & TV**.
3. Add RSS/Atom sources or import an OPML file.
4. Add stations manually, search the radio directory, or import an M3U/M3U8 playlist.
5. Long-press the Android home screen, open **Widgets**, and add either Kanarek widget.

The Backend URL may remain blank: regular feed refreshes stay on-device, while feed discovery, station search, and logo lookup can use Kanarek's built-in default service. Set your own Worker URL only when you want normal feed refreshes routed through that Worker or operator-controlled features such as clean-reader extraction and synchronized state.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Build, tests, and CI](docs/DEVELOPMENT.md)
- [Cloudflare Worker and API](docs/WORKER.md)
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

## [License](LICENSE)

[![License](https://www.shieldcn.dev/github/license/travnie/kanarek.svg?variant=branded&size=xm&mode=light&theme=neutral&font=jetbrains-mono)](https://spdx.org/licenses/MIT)

---
## 💬 Quote from the drawer
<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝Go put your creed into the deed. Nor speak with double tongue. — Ralph Emerson❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Recently on the air
<!--README_FEED:START-->
- [Untangling the Nuclear Knot: A Constructive Agenda for Managing China’s Nuclear Relations with the United States and Its Allies](https://carnegieendowment.org/research/2026/09/untangling-the-nuclear-knot-a-constructive-agenda-for-managing-chinas-nuclear-relations-with-the-united-states-and-its-allies)
- [Ponad 620 tys. zł przepadło. 33-latek uwierzył oszustom - Przelom.pl - portal ziemi chrzanowskiej](https://news.google.com/atom/articles/CBMiowFBVV95cUxOb2U5dGY5S2tzcXR6WWdwZHBFN2ZNTUhhMFQydnpuNDF3blFPNG0tS3dTdjlOeHVHUjRPdTd4T29wUXRST09ORGZHMjZ2bG9mSXRlbG1qYVdrSllCeDhKX2ZFWnRyWWRjUml0eHhaNmxMTFZfZU9SX196aHJ2NUh4YnR6aWNEUHJtaG9fdmZnMW5BX0I0eUFTS2dtVlhyMzk3X1FR?oc=5)
- [Incident across several services](https://www.githubstatus.com/incidents/8zc63m64hy36)
- [More ways to request and configure Copilot code reviews](https://github.blog/changelog/2026-09-23-copilot-code-review-more-ways-to-request-and-configure-reviews)
- [Warcraft III: Forsaken Kingdom - to już nie jest RTS](https://antyweb.pl/warcraft-iii-forsaken-kingdom-to-juz-nie-jest-rts)
- [EXCLUSIVE: Hacked FBI data has sensitive information about employees’ intelligence roles](https://www.reuters.com/world/hacked-fbi-data-has-sensitive-information-about-employees-intelligence-roles-2026-09-23/)
<!--README_FEED:END-->
