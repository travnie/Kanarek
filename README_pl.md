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
<i>❝The first electronic computer ENIAC weighed more than 27 tons and took up 1800 square feet.❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Ostatnio w eterze
<!--README_FEED:START-->
- [The Debt Toll Booth: A Modular Solution to Addressing Sovereign Debt Crises](https://carnegieendowment.org/research/2026/10/the-debt-toll-booth-a-modular-solution-to-addressing-sovereign-debt-crises)
- [Wieś podzielona na pół. Tędy już nie przejedziemy - krzeszowiceone.pl](https://news.google.com/atom/articles/CBMinwFBVV95cUxNaHJ5TWZmSl92bFNxemU0RW50MnEwcHFuTUR3aTduc0Z1LTdPWTdwcFhpZGJ2X2FJNnBwZU9ndHBuUHEwYUhjRm53eVU3RklINnlGRFBQdjVBdjBZMTMtRU9MYWpaVkN4Q3lTTzVKMDV0NElzRXdzdHdLQUFYQUw2WHFRZjE1bWRMMUZha3NwWE5RXzhrQklLVVpzNE5mWjDSAaQBQVVfeXFMTmZ3cG1zd2h2Nk9fdWlFbDBtRzhjMkhRU3dQVUptYm1hbkRPVkV5N2lrMHJGLXpDUFR4VkkzZUtSM3dkNllNdjRLdHpzYzViaGs2REJ5Y1M1cTZYNnFNbmE2OGtpTVM2TTZ0MHR5M3k5alYtUXF2Q3B2QVlnblh2WTVCX1B2RklHbEU2LVNHYWo1c0FWRXpQemREckhqTlFqVXVLSGk?oc=5)
- [Ostatnia stacja Drogi Żelaznej Warszawsko-Wiedeńskiej. Oto dworzec Sosnowiec Maczki w czasach swojej świetności - Dziennik Zachodni](https://news.google.com/atom/articles/CBMi6AFBVV95cUxOaXR4NDQ4U1U1M05paHd1TkhJUzJ1VV83VDZsbTF1cUh6dnV5aXBKREFZZXljb1hhOTFCSkpJcktlX0Ffa2ptOFk1ZHlHN3V5cDhEVmtJNEdUYnQ1SUdOS3J3U0d4eWMwSkdlcThIZVVtZlI4eXV3LVpMdmZFMzVvMGJpQ1ZHdzNhbktadl9BcEFrcVVDMzFIR0dYc3Rxd3hkV09CR0hCa0N6ZHJJdU9FN0lZdzVQLWFZZ0pyOWE2VU4zWTlVelhUNnZuTi1aZ3VIcUVXMHAtWFhUUDFsU2E4OGh2YVpPU0h3?oc=5)
- [Przegląd AI: 3 października 2026](https://promptowy.com/przeglad-ai-2026-10-03/)
- [Zamknięcie dnia: Agenci wymknęli się spod kontroli - i mamy dowody](https://promptowy.com/zamkniecie-dnia-agenci-wymkneli-sie-spod-kontroli-i-mamy-dowody/)
- [Co oznacza zielone kółko na WhatsApp?](https://antyweb.pl/co-oznacza-zielone-kolko-na-whatsapp)
<!--README_FEED:END-->