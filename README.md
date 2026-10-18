# حصن — HISN Browser

**Browse Faster. Stay in Control.**  
A native Android browser project built with Kotlin, Jetpack Compose, Material 3, Android WebView, Room, and DataStore.

**Creator:** Mohammed Hussein  
**Website:** https://iegy.net/  
**Credit:** Designed & Developed by Mohammed Hussein | iegy.net  
**Arabic credit:** تصميم وتطوير Mohammed Hussein | iegy.net  
**Copyright:** © 2026 Mohammed Hussein. All rights reserved.

> The source project and GitHub Actions workflow are prepared. This environment does not include the Android SDK or Gradle and has no network access, so an APK has **not** been built or verified here. The first successful GitHub Actions run is the build verification.

## Current implemented functionality

- Native Android app, not a website wrapped as an APK.
- Arabic-first interface with an English resource set, RTL/LTR layout switching, light/dark/system themes.
- Local start page with user-managed website shortcuts, recent normal browsing entries, and creator credit.
- Combined URL/search input with Google, Bing, and DuckDuckGo choices; rejects direct `javascript:`, `file:`, `content:`, `data:`, and `intent:` address-bar navigation.
- Multi-tab browsing with per-tab WebView instances, tab switcher, close-tab/all-tabs actions, a 12-tab cap, ordinary `target="_blank"` popup handling when initiated by a user gesture, and saved regular-tab URLs during Activity state restoration.
- Bookmark and browsing-history persistence with Room, bookmark title/URL editing, single-entry deletion, and clear-history confirmation.
- Android `DownloadManager` integration for direct downloadable resources surfaced by WebView; filenames are sanitized and completed files can be opened.
- Separate private Activity process and WebView data-directory suffix; requests to clear its cookies, website storage, and cache when that Activity finishes; private browsing history is excluded from normal history and private home doesn't display regular recent history.
- A conservative local domain-based request blocker with user toggle and site allowlist. The default list is intentionally small and is not a full EasyList/EasyPrivacy implementation.
- DataStore-backed preferences for theme, interface language, search engine, blocking, allowlist, desktop-site default, and start-page shortcuts.
- SSL errors are cancelled rather than bypassed; direct WebView file/content access is disabled; website camera/microphone permission requests are denied unless a future explicit runtime-permission flow is implemented.
- GitHub Actions workflows for automated test/lint/debug builds and optional signed tagged releases.

## Important limitations

HISN currently uses **Android System WebView**, whose rendering behavior and feature set depend on the WebView component installed on the device. It is not a complete replacement for standalone Chromium or Quetta.

- Chrome extensions are not supported.
- Ad blocking is basic host-level request interception, not universal cosmetic filtering. Some sites may need to be added to the allowlist. Filter rules are bundled and are not automatically updated from the network.
- Video support is limited to resources a website exposes as ordinary downloadable files. DRM, paywall, authentication and technical access controls are not bypassed; universal HLS/M3U8 extraction is not implemented.
- The downloads screen displays Android DownloadManager status and opens completed files, but advanced retry/share/cancel workflows and in-app progress percentages are not implemented.
- Bookmark storage supports a default folder value but there is not yet a full folder-management UI.
- Website camera/microphone permissions are denied by default; no arbitrary website permission is granted silently.
- The browser does not guarantee anonymity. Sites, network owners, and internet providers may still observe relevant traffic. HTTP pages are supported but are not encrypted.
- There is no measured performance benchmark yet. The project avoids analytics SDKs and starts on a local Compose start page, but performance should be measured on physical devices.

## Requirements

- Android Studio with a compatible stable Android Gradle Plugin toolchain.
- JDK 17 or newer supported by the selected Gradle version.
- Android SDK API 37 and build tools 36.0.0 (as pinned by CI).
- Android 9 / API 28 or newer.

## Build locally

From the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows:

```bat
gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The launcher scripts in this bundle bootstrap the pinned official Gradle 9.3.1 distribution when it is not present. To avoid downloading Gradle, use Android Studio's Gradle integration or install Gradle 9.3.1 and run the same tasks directly.

Expected APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The debug APK uses application ID `net.iegy.hisn` and is intended for installation/testing on Android 9 or newer.

## Get the APK from GitHub

1. Create a new repository on GitHub and upload the contents of this project folder (not just the ZIP file).
2. Make sure the default branch is named `main` or `master` so the configured push workflow runs automatically. You can also run it manually from **Actions → Android build and APK → Run workflow**.
3. Open **Actions** and choose the latest successful **Android build and APK** run.
4. In the **Artifacts** area, download **Hisn-Browser-APK**.
5. Extract the downloaded artifact ZIP. It contains `Hisn-Browser-debug.apk` and `SHA256SUMS.txt`.
6. Transfer the APK to your Android phone/tablet, open it, and allow installation from that download source if Android asks.

The workflow fails if tests/lint/build fail or the expected APK is missing. The artifact is available only after a successful run.

## GitHub Actions

- Main build workflow: `.github/workflows/android.yml`
- Optional tag workflow: `.github/workflows/release.yml`
- Main artifact name: `Hisn-Browser-APK`
- APK file inside the artifact: `Hisn-Browser-debug.apk`

The main workflow runs unit tests, Android lint, `assembleDebug`, checks that the APK exists, computes its SHA-256 checksum, and uploads the APK. It does not require a paid build service.

## Signed release configuration

For automatic publishing of a signed APK on a tag such as `v1.0.0`, configure these **Repository → Settings → Secrets and variables → Actions** secrets:

- `ANDROID_KEYSTORE_BASE64` — the release keystore encoded as one Base64 string.
- `KEYSTORE_PASSWORD` — keystore password.
- `KEY_ALIAS` — alias inside the keystore.
- `KEY_PASSWORD` — key password.

The release workflow decodes the keystore into the runner's temporary directory, restricts its file permissions, uses the Gradle signing configuration, verifies the release signature, publishes the APK to a GitHub Release only when all signing secrets are present, and removes temporary signing files. Do not commit a keystore or put passwords in source files. Without all four secrets, the tag workflow only uploads a debug APK artifact; it does not publish an unsigned APK as a release.

## Changing the app version

Edit `versionCode` and `versionName` in `app/build.gradle.kts`. Increase `versionCode` for each release update.

## Project layout

```text
.
├── .github/workflows/
│   ├── android.yml
│   └── release.yml
├── app/
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/java/net/iegy/hisn/
│   │   ├── BaseBrowserActivity.kt
│   │   ├── MainActivity.kt
│   │   ├── PrivateBrowserActivity.kt
│   │   ├── browser/       # session, URL resolution, blocking and history policy
│   │   ├── data/          # Room entities/DAOs, repository and DataStore settings
│   │   ├── download/      # DownloadManager and filename sanitation
│   │   ├── screens/       # browser, tabs, library, settings and about UI
│   │   └── ui/            # root navigation and theme
│   ├── src/main/res/      # icons, themes and Arabic/English strings
│   ├── src/main/assets/filters/
│   └── src/test/          # URL, blocking, privacy-history and filename unit tests
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew
└── gradlew.bat
```

### About the Gradle launcher

A binary `gradle-wrapper.jar` could not be obtained in the offline preparation environment. To keep CI usable, `gradlew` and `gradlew.bat` are bootstrap scripts that download and invoke the pinned official Gradle 9.3.1 distribution. This is not the standard generated Gradle Wrapper JAR arrangement; if you prefer a standard wrapper, open the project with a compatible Gradle installation and run `gradle wrapper --gradle-version 9.3.1` before uploading.

## Third-party components and licenses

The project uses AndroidX, Jetpack Compose, Material 3, Room, Preferences DataStore, Kotlin Coroutines, and Android System WebView. Their licenses are provided by their respective upstream projects. The bundled starter blocking-domain list is a small hand-maintained seed list described in `app/src/main/assets/filters/README.md`; no third-party filter-list database is copied into this project.

## Troubleshooting

- **Workflow does not run:** confirm the branch name is `main` or `master`, then check **Actions** permissions in repository settings.
- **SDK package not found:** inspect the runner log for `platforms;android-37` and `build-tools;36.0.0` installation results.
- **Build fails:** open the failed run, expand the first failing Gradle step and fix that reported error before rerunning. Do not assume an APK exists after a failed workflow.
- **APK won't install:** confirm Android 9/API 28 or newer, download the APK from the artifact, and allow installation from that source. A debug APK is for testing and is not a Play Store release build.

---

**Designed & Developed by Mohammed Hussein | https://iegy.net/**  
**تصميم وتطوير Mohammed Hussein | iegy.net**
