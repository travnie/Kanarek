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
<i>❝My passion has been to build an enduring company where people were motivated to make great products; the products, not the profits, were the motivation. Sculley flipped these priorities to where the goal was to make money. It's a subtle difference, but it ends up meaning everything. — Steve Jobs❞</i>
<!--ENDS_HERE_QUOTE_README-->
<!-- markdownlint-enable MD033 -->

## 📰 Ostatnio w eterze
<!--README_FEED:START-->
- [Recasting American Power in Latin America](https://carnegieendowment.org/research/2026/09/recasting-american-power-in-latin-america)
- [Kopalnia, która zbudowała Libiąż - Przelom.pl - portal ziemi chrzanowskiej](https://news.google.com/atom/articles/CBMijAFBVV95cUxNbGFTQUJOTE1oVDQzT3B4TDNQQlYwblNLZnNIa2dOUTlDMV9YTUdkeThmSTlTWjRnaFZySERoSjZPeVF2eVVxb0drNl8yQmdPeWRDazVRUnRwYV9seVRoVUotN3FIbklfZVd5NjNxMTMzMEtlOTlYb3BPTmRybkZITTljZXZNVUtacWVDYQ?oc=5)
- [Co łączy salezjanów w Oświęcimiu z Juliuszem Słowackim? Niezwykła historia - Gazeta Krakowska](https://news.google.com/atom/articles/CBMiuAFBVV95cUxNd3VELWw1UkxOeG1VOHMtbF82R0N3OVp6amp2Mnd0Mi0wMGxQNGtEaUtNekhvdjltR2lWYUxKQW4yNGpoaFhxNk5oamIzMlNlM1VycXRZTkN3c1NsZktVdjVqTXExamc0LXpId3QwYU5hOENCbWg5MFA5ZUJ6cE5ocWNmeTVhN0xpNm9tMWNhNUpUQVpDVGoxWGgxbTI0WkpIcGRBVTY4MVRiakFXaS0tSWF4RmQ4MTdE?oc=5)
- [Janina znów trzęsie powiatem. Gdzie teraz trwa wydobycie? - Przelom.pl - portal ziemi chrzanowskiej](https://news.google.com/atom/articles/CBMirgFBVV95cUxPSnhkRndIdkFSNmdMeXVyRFJoS19tWDFfekFEN1k4TXBpQ1hyTTUwb09rRFZBVjdhbVBfLU9lUmU5UnprUWtYZExYeVJUVFZZUHFKdl9LMWhHaUdaWWxwbDQwakppZWNSTlNMQnB1WHZQWTA1RTBva1p6N092alk2NE51dmMtUmh3UkE3aHg4cEQxeXc0YXZGQjN2TXNOM0RuQjhOSnlmcWJHODlzT3c?oc=5)
- [Turkey says it could help meet Saudi military needs under defence pact](https://www.reuters.com/business/aerospace-defense/turkey-says-it-could-help-meet-saudi-military-needs-under-defence-pact-2026-09-19/)
- [Slovak PM Fico says some in West seek war between Russia and NATO](https://www.reuters.com/world/slovak-pm-fico-says-some-west-seek-war-between-russia-nato-2026-09-19/)
<!--README_FEED:END-->