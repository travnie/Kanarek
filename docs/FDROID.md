# Submitting Kanarek to F-Droid

Prepared submission notes. Nothing has been submitted to F-Droid yet.

## Ready state

- Application ID: `com.kanarek`
- License: MIT
- Current release: `0.0.6` (`versionCode 4`)
- Android source module: `app/`
- F-Droid build flavor: `foss`
- Minimum Android: 8.0 / API 26
- Upstream listing metadata: `app/fastlane/metadata/android/`
- English and Polish store text are included upstream.

The `play` flavor includes Google Cast / Play Services. The `foss` flavor does not and is the only flavor intended for F-Droid.

## fdroiddata metadata

Copy this to `metadata/com.kanarek.yml` in a fork of `fdroid/fdroiddata` when filing the submission.

```yaml
Categories:
  - Internet
  - Multimedia
License: MIT
AuthorName: trvny
SourceCode: https://github.com/travnie/kanarek
IssueTracker: https://github.com/travnie/kanarek/issues
Changelog: https://github.com/travnie/kanarek/releases

AutoName: Kanarek

RepoType: git
Repo: https://github.com/travnie/kanarek.git

Builds:
  - versionName: 0.0.6
    versionCode: 4
    commit: f0cfc72b879ead14a9c2accb9c6805fd4e9fdb63
    subdir: app
    gradle:
      - foss
    prebuild:
      - sdkmanager "platforms;android-37.0" "build-tools;37.0.0"

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags ^v[0-9.]+$
CurrentVersion: 0.0.6
CurrentVersionCode: 4
```

The first F-Droid build intentionally uses the exact preparation commit rather than the older `v0.0.6` tag. That commit contains the F-Droid-visible Fastlane layout while keeping the same app version and version code. Future releases can be picked up from normal `v*` tags by `AutoUpdateMode`.

## Store listing

F-Droid can read the upstream Fastlane metadata from `app/fastlane/metadata/android/` when building with `subdir: app`.

English short description:

> News widget for your home screen, plus a radio and IPTV player.

The full English description covers RSS/Atom feeds and widgets, OPML import/export, radio/IPTV playback, M3U playlists, background controls, privacy, and the optional self-hostable backend. A Polish listing is included as well.

Screenshots and a feature graphic are optional polish rather than submission blockers. F-Droid can extract the launcher icon from the APK.

## Build notes

The repository intentionally does not commit `gradlew` or `gradle-wrapper.jar`. F-Droid build servers provide `gradlew-fdroid`, which reads the pinned Gradle version from `gradle/wrapper/gradle-wrapper.properties`.

The pinned submission above compiles against Android 37.0. Keep its SDK setup unchanged.

Current checkout: app requires Android 37.1; shared requires 37.0. Install both `platforms;android-37.0` and `platforms;android-37.1`, plus `build-tools;37.0.0`. Update the F-Droid prebuild SDK list for the next release.

The optional `worker/` backend is not part of the APK and is not required for ordinary on-device RSS/Atom parsing.

## Before filing

1. Run one final `fdroid lint` / `fdroid build com.kanarek:4` sanity check if an F-Droid build environment is available.
2. Open the `fdroiddata` merge request as a new app submission.

Do not reuse or modify the separate WiFi Automatic submission.
