**Polski** · [English](README.md) · [简体中文](README_zh.md)

<div align="center">

<img src="assets/kanarek.svg" alt="Kanarek" width="96">

# Kanarek

**Czytnik i widżet wiadomości oraz odtwarzacz radia/IPTV dla Androida.**

[![android CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/android-ci.yml?label=android%20CI&logo=android&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/android-ci.yml)
[![worker CI](https://img.shields.io/github/actions/workflow/status/travnie/kanarek/worker-ci.yml?label=worker%20CI&logo=cloudflare&logoColor=111&color=FFC107&style=flat-square)](https://github.com/travnie/kanarek/actions/workflows/worker-ci.yml)
[![last commit](https://img.shields.io/github/last-commit/travnie/kanarek?color=FFC107&logo=git&logoColor=111&style=flat-square)](https://github.com/travnie/kanarek/commits/main)
[![license](https://img.shields.io/github/license/travnie/kanarek?color=FFC107&style=flat-square)](LICENSE)<br>
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white) ![Cloudflare Worker](https://img.shields.io/badge/Worker-F38020?style=flat-square&logo=cloudflareworkers&logoColor=white)<br>
<a href="https://deepwiki.com/travnie/kanarek"><img src="https://deepwiki.com/badge.svg" alt="DeepWiki"></a>

</div>

Kanarek łączy dwa narzędzia w jednej natywnej aplikacji:

- czytnik RSS/Atom z automatycznym pokazem wiadomości i widżetem ekranu głównego,
- odtwarzacz radia internetowego i IPTV działający w tle, również z własnym widżetem.

Opcjonalny Cloudflare Worker przyspiesza pobieranie i dodaje funkcje sieciowe. Nie jest wymagany: zwykłe kanały RSS/Atom mogą być przetwarzane bezpośrednio na urządzeniu.

## Najważniejsze funkcje

### Wiadomości

- skalowalny widżet z pokazem slajdów, ręczną nawigacją i osobnymi ustawieniami każdego egzemplarza,
- własne źródła RSS 2.0 i Atom, import oraz eksport OPML,
- wyszukiwanie wiadomości, filtrowanie źródeł i tryb najważniejszych nagłówków,
- oznaczanie jako przeczytane, zapisywanie artykułów oraz opcjonalny tekst offline,
- podgląd czystej treści artykułu przy skonfigurowanym Workerze,
- opcjonalne powiadomienia o nowych wiadomościach z godzinami ciszy,
- zachowanie ostatnich poprawnych wiadomości, gdy pojedyncze źródło chwilowo nie działa.

### Radio i IPTV

- odtwarzanie w tle przez Media3/ExoPlayer z kontrolkami systemowymi,
- import, eksport i edycja playlist M3U/M3U8,
- radio, telewizja, grupy kanałów, ulubione stacje i metadane aktualnego utworu,
- wyszukiwanie stacji w katalogu Radio Browser,
- uzupełnianie brakujących logotypów kanałów przez iptv-org i favikony,
- obsługa `User-Agent` oraz `Referer` zapisanych w playliście,
- Google Cast w wariancie `play`; wariant `foss` nie wymaga usług Google.

## Instalacja

[APK](https://github.com/travnie/kanarek/releases).

- `play`: zawiera obsługę Google Cast,
- `foss`: wariant bez GMS, przeznaczony dla środowisk FOSS i F-Droid.

Minimalna wersja systemu to Android 8.0 (API 26).

## Szybki start

1. Zainstaluj wybrany wariant APK.
2. Otwórz Kanarka i wybierz **Wiadomości** albo **Radio i TV**.
3. Dodaj własne źródła RSS/Atom lub zaimportuj OPML.
4. Dodaj stacje ręcznie, wyszukaj radio albo zaimportuj playlistę M3U/M3U8.
5. Przytrzymaj ekran główny Androida, otwórz **Widżety** i dodaj wybrany widżet Kanarka.

Pole Backend URL może pozostać puste: zwykłe feedy są wtedy odświeżane na urządzeniu, a odkrywanie źródeł, wyszukiwanie stacji i dobieranie logo mogą korzystać z wbudowanego domyślnego serwisu Kanarka. Własny adres Workera ustaw dopiero wtedy, gdy chcesz kierować przez niego normalne odświeżanie feedów albo używać funkcji zależnych od świadomej konfiguracji operatora, takich jak czysty czytnik i synchronizacja stanu.

## Dokumentacja

- [Architektura](docs/ARCHITECTURE.md)
- [Budowanie, testy i CI](docs/DEVELOPMENT.md)
- [Cloudflare Worker i API](docs/WORKER.md)
- [Historia projektu](docs/HISTORY.md)
- [Notatki do zgłoszenia w F-Droidzie](docs/FDROID.md)

Kanarek powstawał w repozytorium [trvny/feeds](https://github.com/trvny/feeds) do sierpnia 2026 i
został stamtąd wydzielony razem z całą historią.

## Rozwój

Skrypty Gradle wrappera celowo nie są commitowane. Na świeżym klonie zainstaluj dokładną wersję Gradle wskazaną w `gradle/wrapper/gradle-wrapper.properties`, a następnie utwórz wrapper przed użyciem `./gradlew`:

```bash
GRADLE_VERSION=$(sed -n 's#^distributionUrl=.*/gradle-\([0-9][A-Za-z0-9.-]*\)-\(bin\|all\)\.zip$#\1#p' gradle/wrapper/gradle-wrapper.properties)
command -v gradle >/dev/null || { echo "Najpierw zainstaluj Gradle $GRADLE_VERSION" >&2; exit 1; }
gradle --version | grep -F "Gradle $GRADLE_VERSION" >/dev/null || { echo "Do utworzenia wrappera użyj Gradle $GRADLE_VERSION" >&2; exit 1; }
gradle wrapper --gradle-version "$GRADLE_VERSION" --no-daemon
./gradlew assembleDebug
./gradlew testPlayDebugUnitTest
```

## [Licencja](LICENSE)

[![License](https://www.shieldcn.dev/github/license/travnie/kanarek.svg?variant=branded&size=xm&mode=light&theme=neutral&font=jetbrains-mono)](https://spdx.org/licenses/MIT)

---
## 💬 Cytat z szuflady
<!-- markdownlint-disable MD033 -->
<!--STARTS_HERE_QUOTE_README-->
<i>❝A programmer developed an operating system called TempleOS in Hospitalized for mental health problems, he believes that TempleOS is literally the Third Temple as biblically prophesied. Per God’s “instructions,” the OS uses a 640×480, 16 color display, and uses the language HolyC.❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Ostatnio w eterze
<!--README_FEED:START-->
- [Operetka wróci do Młoszowej - Przelom.pl](https://news.google.com/atom/articles/CBMif0FVX3lxTE1zeVl6VHlCYlBqSlNRYU5TVGoxV19VSTU0a3FCckpiYjdUT0tMOEptNTRkLV92MzRXdUlINXIxajBRVWVvOVQzSkw3RVdCM3IxUFd5aVZETHNtOG1LN053dHRHUmEtaUZDeW5qTXJjUXl6WC1XdGFYYVZpRXJIUkU?oc=5)
- [Seniorzy z UTW ruszyli po wiedzę, zdrowie i przygodę - Przelom.pl](https://news.google.com/atom/articles/CBMirgFBVV95cUxPcGRwc1ludjBJSWFaZ0dWZ0JUUkxwczBoSWQ2cGVRUkgxb3NNWWo0ZnJqNW5Id1dyWkFCa1pZQVJRU3VmdXVkajlnZ0xsWHJFaDJrYUtMUmNFeVFta0otcFFSUS1UMlhrTXNhcHF4SExiTnJQZXY5YUtyY2xuSnFYV0ctcEhZcDR5bUFyUDZTdkpHbktDY2lIMFZSdVUyLWpSTkdfWGx4Rl93c1g2SVE?oc=5)
- [Amazon in talks to buy AI startup Decart for around $7 billion, WSJ reports](https://www.reuters.com/business/retail-consumer/amazon-talks-buy-ai-startup-decart-around-7-billion-wsj-reports-2026-10-11/)
- [W mijającym tygodniu odeszli od nas \[5 - 10 październik\] - Przelom.pl](https://news.google.com/atom/articles/CBMiogFBVV95cUxQRTFFRDRGbkVMMXI0Ym8yd2g1a3IwaXczZm1VaUpmbWRwaVdwd3Z0TWRlSWZOcW1IVUdPcGluV2VicHo2blVOREt5djFERnowR3dsSkFwN0FraV9HYUt1UEE5T3VXM1RqZG14bHNOVFhLMWUxenBFa2E5ZWpyaHROWERHUXVOZUVldnBfNWhnZnY1X0tnMjIydGQzeG9tQmF3NHc?oc=5)
- [Wiązanki i kwiaty na cmentarz Wszystkich Świętych 2026. Najpiękniejsze stroiki na cmentarz z żywych kwiatów 12.10.2026 - Dziennik Łódzki](https://news.google.com/atom/articles/CBMi8wFBVV95cUxObjFvTnNmTEh2c2ZWZGttUWJRX25MLU5sRHJJSWVlVWhQdjFvMjVjaFE3amdLd015THpMY2hjb3V3azJOY0xFQ1dRU3lQcjRRaTNZTzJBUEJYWGxOYS1TX3Y2M1N5V2dwQ2xLV0FSblkwaTVCZGFkU2xNdmNSRTY3WmRTal9COW95T1BfaE0zT2pBaENXTk9OU3JKaEwxRnpiZkcwOFBnX3h0ZnNDbTBmUTJEYm4wMVNQQ1pBckQzTE5GdHJwckl0b3hJbzBDaVJhX2ItZTZoMlJRTW5TUEJFOGtRT21udE5DSzBWeEE1LTJsdzg?oc=5)
- [At least 72 Sudanese artisanal miners killed in strike, rights monitor says](https://www.reuters.com/world/africa/least-72-sudanese-artisanal-miners-killed-strike-saturday-rights-monitor-says-2026-10-11/)
<!--README_FEED:END-->