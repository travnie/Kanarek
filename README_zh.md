[Polski](README_pl.md) · [English](README.md) · **简体中文**

<div align="center">

<img src="assets/kanarek.svg" alt="Kanarek" width="96">

# Kanarek

**Android 新闻阅读器与桌面小组件，同时提供后台广播/IPTV 播放。**

[![android CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/android-ci.yml?label=android%20CI&logo=android&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/android-ci.yml)
[![worker CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/worker-ci.yml?label=worker%20CI&logo=cloudflare&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/worker-ci.yml)
[![last commit](https://img.shields.io/github/last-commit/travnie/kanarek?color=FFC107&logo=git&logoColor=111&style=flat-square)](https://github.com/travnie/kanarek/commits/main)
[![license](https://img.shields.io/github/license/travnie/kanarek?color=FFC107&style=flat-square)](LICENSE)<br>
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white) ![Cloudflare Worker](https://img.shields.io/badge/Worker-F38020?style=flat-square&logo=cloudflareworkers&logoColor=white)<br>
<a href="https://deepwiki.com/travnie/kanarek"><img src="https://deepwiki.com/badge.svg" alt="DeepWiki"></a>

</div>

Kanarek 把两类工具放进一个原生应用：

- RSS/Atom 阅读器，以及可自动轮播的主屏新闻小组件；
- 后台网络广播与 IPTV 播放器，并带有独立的播放控制小组件。

可选的 Cloudflare Worker 能加速抓取并提供额外网络功能，但并非必需。普通 RSS/Atom 源可以直接在设备上解析。

## 亮点

### 新闻

- 可调整大小的轮播小组件，支持手动切换和每个小组件独立设置；
- 自定义 RSS 2.0 / Atom 源，并支持 OPML 导入与导出；
- 本地搜索、来源过滤，以及可选的标题排序模式；
- 已读状态、收藏文章，以及可选的离线纯文本；
- 配置可信 Worker 后可使用简洁文章预览；
- 可选的新文章通知与静默时段；
- 单个来源暂时故障时，仍保留最后一次成功获取的内容。

### 广播与 IPTV

- 基于 Media3/ExoPlayer 的后台播放，并接入系统媒体控制；
- M3U/M3U8 导入、导出和编辑；
- 广播、电视、频道分组、收藏与正在播放的元数据；
- 通过 Radio Browser 目录发现电台；
- 缺失频道 logo 时使用 iptv-org 与 favicon 回退；
- 支持播放列表提供的 `User-Agent` 和 `Referer`；
- `play` flavor 支持 Google Cast，`foss` flavor 不依赖 Google 服务。

## 安装

[APK](https://github.com/travnie/kanarek/releases)

- `play`：包含 Google Cast；
- `foss`：无 GMS，面向 FOSS / F-Droid 环境。

最低支持 Android 8.0（API 26）。

## 快速开始

1. 安装你需要的 APK flavor。
2. 打开 Kanarek，选择 **News** 或 **Radio & TV**。
3. 添加 RSS/Atom 来源，或导入 OPML。
4. 手动添加电台、搜索广播目录，或导入 M3U/M3U8 播放列表。
5. 长按 Android 主屏，打开 **Widgets**，添加任一 Kanarek 小组件。

Backend URL 可以留空：普通 feed 刷新仍在设备本地完成，而 feed 发现、电台搜索和 logo 查询可以使用 Kanarek 内置的默认服务。只有在希望把普通 feed 刷新转发到自己的 Worker，或需要简洁阅读器提取、同步状态等运营方控制功能时，才需要设置自己的 Worker URL。

## 文档

- [架构](docs/ARCHITECTURE.md)
- [构建、测试与 CI](docs/DEVELOPMENT.md)
- [Cloudflare Worker 与 API](docs/WORKER.md)
- [项目历史](docs/HISTORY.md)
- [F-Droid 提交说明](docs/FDROID.md)

Kanarek 在 2026 年 8 月之前开发于 [trvny/feeds](https://github.com/trvny/feeds)，之后连同完整历史一起拆分到本仓库。

## 开发

仓库有意不提交 Gradle wrapper 脚本。全新 clone 后，请先安装 `gradle/wrapper/gradle-wrapper.properties` 指定的精确 Gradle 版本，再生成 wrapper：

```bash
GRADLE_VERSION=$(sed -n 's#^distributionUrl=.*/gradle-\([0-9][A-Za-z0-9.-]*\)-\(bin\|all\)\.zip$#\1#p' gradle/wrapper/gradle-wrapper.properties)
command -v gradle >/dev/null || { echo "Install Gradle $GRADLE_VERSION first" >&2; exit 1; }
gradle --version | grep -F "Gradle $GRADLE_VERSION" >/dev/null || { echo "Use Gradle $GRADLE_VERSION to bootstrap the wrapper" >&2; exit 1; }
gradle wrapper --gradle-version "$GRADLE_VERSION" --no-daemon
./gradlew assembleDebug
./gradlew testPlayDebugUnitTest
```

## [许可证](LICENSE)

[![License](https://www.shieldcn.dev/github/license/travnie/kanarek.svg?variant=branded&size=xm&mode=light&theme=neutral&font=jetbrains-mono)](https://spdx.org/licenses/MIT)

---
## 💬 抽屉里的引语
<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝The first word spoken on the internet was “lo”. It was supposed to be “login” but the computer crashed after the first two letters.❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 最近播报
<!--README_FEED:START-->
- [Episode 2: Financing War in the Grey Zone](https://www.rusi.org/podcasts/suspicious-transaction-report/episode-2-financing-war-grey-zone)
- [Rozkochów zachwycił jury. Jest wysokie miejsce i nagroda - Przelom.pl - portal ziemi chrzanowskiej](https://news.google.com/atom/articles/CBMipAFBVV95cUxPY0tiLU1SOHViRk1TcWtxZXltVWoyVlJYTmJ0NW1wa0ppN1p6cnlWOEE1NWtzYjRyYXRhN29KbmJKLW5pcHp6emJMTkJIVmJzd1d0d2duVEdHU0JuMXR6aEJ1bWFMeTJhSWd2QUE3Z3ZHSko2d2pRZ0tPRVF3aDdQczc1Z2Fiek9kVjhWbldTODJ3NlJUUGFFVkNTSFU5S0JiY1FSTQ?oc=5)
- [Recenzja Trails in the Sky 2nd Chapter. Must play dla fanów dobrych jRPG](https://antyweb.pl/recenzja-trails-in-the-sky-2nd-chapter-must-play-dla-fanow-dobrych-jrpg)
- [Ostrzeżenie pierwszego stopnia dla Chrzanowa i okolic. Uwaga na gestą mgłę - Przelom.pl - portal ziemi chrzanowskiej](https://news.google.com/atom/articles/CBMivAFBVV95cUxOZmdOeF9uQS13NWZsLUoxN1k2QWVSQWtJUkFKU3FtMEF0YjVWbnJqTjVqbnladUhfcWkwNFFjSG5nWHVDbGhkUmdHNzk1emx1QWstR0VJYUdUMGotTlVlOXZob2JmZ1ZteHN3Z3JxRWNqb2FtWmRYME81UXNhWXFBc1FWVERMSXN2MHRTY3N2cWdKY3k5UUItc19mTG84Zi1WaGEyNU1UNDRTUlFPWUpKa3RtbTRoWFZvT2VaOQ?oc=5)
- [PLLuM: polski model językowy państwa. Co to jest, gdzie działa i czym różni się od Bielika](https://promptowy.com/pllum/)
- [Jensen Huang: kim jest szef Nvidii, ile jest wart i jak zbudował najcenniejszą firmę świata](https://promptowy.com/jensen-huang/)
<!--README_FEED:END-->