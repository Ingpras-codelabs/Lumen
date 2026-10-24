# Lumen

Lumen (an offline file viewer) is a completely free, fully offline native Android
app (Kotlin + Jetpack Compose + Material 3) that renders file formats Android does
not preview natively. It prioritizes SVG and Markdown and also handles a broad
curated set: plain text/log, JSON, XML, YAML, CSV, TSV, HTML, RTF, EPUB, Office
documents via content extraction (Word `.docx`, Excel `.xlsx`, PowerPoint
`.pptx`), source code with syntax highlighting, archive listings (zip, jar, tar,
gz/tgz), and extra image types (webp, bmp, gif, ico, wbmp, and heif/heic on
supported devices). Every format is rendered by a pluggable renderer and, when a
file matches nothing specific, it falls back to a readable hex + text preview so
no file is ever left unhandled.

Beyond raw format coverage, Lumen adds the conveniences you expect from a daily
viewer: predictable system-back navigation with a confirm-before-exit prompt, a
locally remembered list of recently opened files for one-tap reopening, a
searchable and categorized list of supported formats on the home screen, and an
in-document find (with copy-all) for text, source, and structured files.

Lumen is free and open source software (FOSS), released under the MIT License and
built by Ingpras Codelabs. It is completely free for everyone: no ads, no
telemetry, no accounts, and no cost, now or ever.

## Completely free and fully offline

The app is completely free for everyone and ships no ads, no telemetry, and no
account system. There is no cost of any kind, and the full source code is open
under the MIT License. Lumen is offline by construction, not by policy, and that
guarantee is enforced in three concrete ways:

1. No INTERNET permission. `app/src/main/AndroidManifest.xml` declares no
   `<uses-permission>` entries at all, and in particular never declares
   `android.permission.INTERNET`. Without that permission the Android runtime
   denies the process any socket access, so the app physically cannot reach the
   network. (The only permission in the merged manifest is the signature-level
   `com.offlineviewer.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` that the
   Android Gradle Plugin injects automatically on modern targets. It is not a
   network permission.)
2. WebView is locked down. HTML and EPUB chapters render in a `WebView` with
   JavaScript disabled, file and content access disabled, and both
   `blockNetworkLoads` and `blockNetworkImage` enabled. Content is loaded with
   `loadDataWithBaseURL(null, ...)` and a custom `WebViewClient` that blocks all
   navigation and returns empty responses for any non-data subresource, so no
   remote asset can be fetched even from inside rendered markup.
3. Bundled offline libraries only. Every rendering library (AndroidSVG, Markwon,
   SnakeYAML) is bundled into the APK and used purely for local parsing and
   rendering. The Markdown renderer installs no image/network plugin, so it never
   performs remote image loading.

## Broad device support

The app is designed to run on essentially any modern Android device:

| Setting        | Value                          |
|----------------|--------------------------------|
| Language       | Kotlin (JVM target 17)         |
| UI toolkit     | Jetpack Compose + Material 3   |
| compileSdk     | 34                             |
| minSdk         | 21 (Android 5.0 Lollipop)      |
| targetSdk      | 34 (Android 14)                |
| applicationId  | `com.offlineviewer.app`        |
| versionName    | 1.0                            |
| Permissions    | none (fully offline)           |

`minSdk 21` means the app installs on Android 5.0 and up, covering the vast
majority of active devices, while `targetSdk 34` keeps it compliant with current
platform behavior. The UI is responsive via `WindowSizeClass` (phones, tablets,
and foldables get appropriate content width), themed with Material 3 including
light/dark and Android 12+ dynamic color with a persisted in-app System/Light/Dark
toggle, and accessible (content descriptions on all icons/images, scalable `sp`
text, interactive targets of at least 48dp, selectable text, and logical TalkBack
reading order).

## Supported formats

Each format is handled by an offline renderer. Extensions are matched without the
leading dot; MIME matching is case-insensitive. All third-party libraries are
permissively licensed (Apache License 2.0).

| Format                | Extensions                                                                 | MIME types                                                                 | Offline library / technique                                      | License    |
|-----------------------|----------------------------------------------------------------------------|----------------------------------------------------------------------------|------------------------------------------------------------------|------------|
| SVG                   | `.svg`                                                                      | `image/svg+xml`                                                             | AndroidSVG 1.4 parses to a Picture; Compose pinch-zoom/pan       | Apache-2.0 |
| Markdown              | `.md`, `.markdown`                                                          | `text/markdown`, `text/x-markdown`                                         | Markwon 4.6.2 (core + tables + strikethrough + tasklist + html)  | Apache-2.0 |
| Plain text / log      | `.txt`, `.log`                                                             | `text/plain`                                                               | Built-in scrollable, selectable monospace renderer (also fallback)| n/a        |
| JSON                  | `.json`                                                                     | `application/json`, `text/json`                                            | `org.json` (Android built-in) pretty-print + syntax coloring     | Public Domain |
| XML                   | `.xml`                                                                      | `application/xml`, `text/xml`                                              | Custom structural indenter + syntax coloring                     | n/a        |
| YAML                  | `.yaml`, `.yml`                                                             | `text/yaml`, `application/x-yaml`, `application/yaml`                      | SnakeYAML 2.3 validation + syntax coloring                        | Apache-2.0 |
| CSV                   | `.csv`                                                                      | `text/csv`                                                                 | Custom RFC-4180-style parser + scrollable table                  | n/a        |
| TSV                   | `.tsv`                                                                      | `text/tab-separated-values`                                               | Custom delimited parser + scrollable table                       | n/a        |
| HTML                  | `.html`, `.htm`                                                            | `text/html`                                                               | Built-in WebView, JS disabled and network blocked                | n/a        |
| RTF                   | `.rtf`                                                                     | `application/rtf`, `text/rtf`                                              | Custom RTF control-word converter to styled text                 | n/a        |
| EPUB                  | `.epub`                                                                    | `application/epub+zip`                                                     | `java.util.zip` + OPF spine parse; chapters in offline WebView   | n/a        |
| Word document         | `.docx`                                                                    | `application/vnd.openxmlformats-officedocument.wordprocessingml.document`  | `java.util.zip` + custom OOXML XML scan of `word/document.xml`; paragraphs/headings/tables as styled scrollable text (content extraction) | n/a        |
| Spreadsheet           | `.xlsx`                                                                    | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`        | `java.util.zip` + custom OOXML XML scan of `xl/sharedStrings.xml` + worksheets; each sheet in the scrollable table UI (content extraction) | n/a        |
| Presentation          | `.pptx`                                                                    | `application/vnd.openxmlformats-officedocument.presentationml.presentation` | `java.util.zip` + custom OOXML XML scan of `ppt/slides/slideN.xml`; per-slide text in slide order (content extraction) | n/a        |
| Source code / config  | `.kt`, `.kts`, `.java`, `.js`, `.mjs`, `.jsx`, `.ts`, `.tsx`, `.py`, `.c`, `.h`, `.cpp`, `.cc`, `.cxx`, `.hpp`, `.cs`, `.go`, `.rb`, `.rs`, `.php`, `.swift`, `.sh`, `.bash`, `.zsh`, `.sql`, `.css`, `.scss`, `.less`, `.toml`, `.ini`, `.cfg`, `.conf`, `.env`, `.properties`, `.scala`, `.sc`, `.dart`, `.lua`, `.pl`, `.pm`, `.r`, `.groovy`, `.gradle`, `.ps1`, `.bat`, `.cmd` | `text/x-java`, `text/x-kotlin`, `text/x-python`, `application/javascript`, `application/x-sh`, `text/x-csrc`, `text/x-c++src` | Built-in regex syntax highlighter (per-language), monospace view | n/a        |
| Archives (listing)    | `.zip`, `.jar`, `.tar`, `.gz`, `.tgz`                                      | `application/zip`, `application/java-archive`, `application/x-tar`, `application/gzip`, `application/x-gzip` | `java.util.zip` (ZipInputStream/GZIPInputStream) + minimal ustar tar reader; lists entries and sizes, does not extract | n/a        |
| Images                | `.webp`, `.bmp`, `.gif`, `.ico`, `.png`, `.jpg`, `.jpeg`, `.wbmp`, `.heif`, `.heic` | `image/webp`, `image/bmp`, `image/gif`, `image/x-icon`, `image/vnd.microsoft.icon`, `image/png`, `image/jpeg`, `image/vnd.wap.wbmp`, `image/heif`, `image/heic` | Android `BitmapFactory` decoders (+ `ImageDecoder` for heif/heic on API 28+) + Compose zoom/pan | n/a        |

Notes:

- "n/a" denotes logic implemented directly in this project with no third-party
  runtime dependency; that code is covered by this repository's own license.
- `org.json` is part of the Android runtime, so no runtime dependency is added for
  it. It is pulled in only as a test dependency so the JSON formatter can be unit
  tested on the JVM, where the `android.jar` stub of `org.json` throws.
- GIF renders as a static first frame; ICO support is basic. SVG is intentionally
  routed to the SVG renderer rather than the raster image renderer.
- HEIF/HEIC decode uses the platform `ImageDecoder`, which is only available from
  API 28; on older devices a HEIF/HEIC file degrades to a clear error state. TIFF
  and AVIF are deliberately NOT advertised because the platform decoders do not
  handle them reliably across the supported API range (minSdk 21 to targetSdk 34).
- Archives are shown as a read-only table of contents (entry names, sizes, and
  counts); contents are listed, not extracted. Listings are capped for very large
  archives and malformed archives degrade gracefully to a partial or empty view.
- Office support (`.docx`, `.xlsx`, `.pptx`) is **content extraction, not
  pixel-perfect rendering**. Readable text, table cells, and slide text are
  recovered from the OOXML parts, but original fonts, images, charts, shapes,
  cell styling, exact positioning, and animations are **not** reproduced; `.xlsx`
  shows the cached cell values stored in the file, not recomputed formulas. A
  malformed or empty Office archive degrades to a clear error state.
- Legacy binary Office formats (`.doc`, `.xls`, `.ppt`, the pre-2007 OLE2
  compound files) are **not** decoded. There is no permissive, fully-offline,
  high-fidelity decoder for them under this project's license constraints, so
  they are not advertised and instead fall through to the universal hex + text
  fallback rather than overclaiming support.
- TIFF and AVIF images are deliberately omitted: the Android platform decoders
  do not handle TIFF reliably across the supported API range, and AVIF decode is
  only available on API 31+ and is device/codec dependent, so neither can be
  promised honestly on minSdk 21 to targetSdk 34.
- No third-party runtime dependency was added for Office, archive, recent-files,
  or in-document search support. All of that logic is implemented directly in
  this project on top of `java.util.zip` and the Android platform, so the runtime
  dependency set is unchanged from before these features.
- The exact dependency versions above are read from
  [`gradle/libs.versions.toml`](gradle/libs.versions.toml):
  AndroidSVG `1.4`, Markwon `4.6.2`, SnakeYAML `2.3`, `org.json` `20240303` (test
  only), Android Gradle Plugin `8.6.1`, Kotlin `2.0.21`, Compose BOM `2024.09.03`.

## Architecture

Lumen is a single-activity Compose app organized around a small, pluggable
rendering abstraction. Everything that can be is kept framework-free so it is
unit-testable on the JVM.

### FileRenderer abstraction

`render/FileRenderer.kt` defines the plug-in contract: a human-readable
`formatName`, a set of `supportedMimeTypes`, a set of `supportedExtensions`, a pure
`canRender(mime, ext)` predicate, and a single Compose entry point `Render(file,
modifier)`. Only `Render` is Compose-bound; the matching logic is pure.

### RendererRegistry resolution rules

`render/RendererRegistry.kt` holds every registered renderer plus a universal
`fallback` (the plain-text renderer). `resolve(mimeType, extension)` picks a
renderer deterministically in this order:

1. MIME match: the first renderer whose `supportedMimeTypes` contains the exact
   (lower-cased) MIME type.
2. Extension match: the first renderer whose `supportedExtensions` contains the
   (lower-cased, dot-stripped) extension.
3. `canRender` match: the first renderer whose `canRender(mime, ext)` returns true
   (covers any wildcard-style logic).
4. Plain-text fallback: when nothing matches, the `PlainTextRenderer` is returned
   so every file renders something.

`createDefault()` registers the renderers in a documented order. SVG is listed
before the image renderer because SVG is an `image/*` type and must win for
`image/svg+xml` and `.svg`; the remaining format sets are disjoint. The plain-text
renderer is both the explicit handler for `text/plain`, `.txt`, and `.log` and the
last-resort fallback.

### File loading with a size guard

`data/FileLoader.kt` reads a content `Uri` through `ContentResolver`: it resolves
the display name via `OpenableColumns`, reads the provider MIME type, and loads the
payload either as UTF-8 text or as raw bytes. The pure helpers in
`data/FileLoaderLogic.kt` handle extension parsing, MIME resolution (a generic
`application/octet-stream` from the provider is overridden by a known extension
mapping), the text-vs-binary decision, and the size guard. The default in-memory
limit is 32 MB; oversized or unreadable files surface an error state rather than
causing an out-of-memory crash (both `OutOfMemoryError` and general `Throwable` are
caught and converted to a failure result).

### Storage Access Framework and intent handling

`MainActivity` wires an `ActivityResultContracts.OpenDocument` launcher so users can
pick a local file with the system file picker (SAF), taking persistable read
permission where available. It also handles incoming intents so the app appears in
Android's "Open with" and share menus:

- `ACTION_VIEW`: the `Uri` is read from the intent data.
- `ACTION_SEND`: the `Uri` is read from `EXTRA_STREAM`.

The manifest declares a VIEW filter for the curated MIME set, a second VIEW filter
that matches the curated file extensions by `pathPattern` when the MIME type is a
generic `application/octet-stream`, and a SEND filter for the curated MIME set. The
manifest intent-filters mirror `render/SupportedFormats.kt`, the single source of
truth for the advertised format set.

### Compose / Material 3 UI

The UI layer is Compose with Material 3. `ViewerViewModel` exposes a sealed
`ViewerUiState` (Empty, Loading, Content, Error) driven by `FileLoader` results, and
resolves the matching renderer through the registry. `ViewerScreen` is a `Scaffold`
with a `TopAppBar` (file name title, detected-format subtitle, open action, a find
action for text content, and an overflow menu for Share, File details, Find in
file / Copy all text for text content, Open another, and Theme) and dispatches the
loaded file to its renderer. Reusable state composables in
`ui/components/StateScreens.kt` provide consistent `LoadingState`, `EmptyState`
(friendly prompt, a recent-files list, and a searchable/categorized
supported-format list), and `ErrorState` (icon, headline, message, and an
open-another action) screens that the viewer reuses. Responsive horizontal padding
is derived from `WindowSizeClass`, and theming supports light/dark plus Android 12+
dynamic color with a persisted System/Light/Dark toggle.

## Navigation, recent files, and search

Lumen behaves predictably with the system back button and adds a few local-only
conveniences layered on the same offline guarantees as the rest of the app.

- **Back navigation.** Back behaviour is derived from the current viewer state
  by the pure `ui/BackNavigation.kt` logic (unit-tested on the JVM). When a file
  is open, loading, or showing an error, pressing back returns to the home
  screen (closing the file) instead of leaving the app. When the in-document
  find bar is open, back closes it first.
- **Exit confirmation.** At the home screen there is nowhere further back to go,
  so back raises an "Exit Lumen?" confirmation dialog; only confirming it lets
  the activity finish, so the app is never closed by an accidental back press.
- **Recent files.** `data/RecentFilesPreferences.kt` persists a short,
  de-duplicated, most-recent-first list of opened files via `SharedPreferences`
  (mirroring the theme preference store). It is fully local: it stores only the
  SAF URI string and the display name, declares no permission, and makes no
  network calls. A file is remembered **only** when its persistable read
  permission was successfully taken, so every remembered entry can be reopened;
  the list is shown as tappable rows on the home screen (hidden when empty), and
  an entry whose permission has since been revoked is pruned instead of failing.
  The bounding/de-duplication rules live in the pure `data/RecentFilesLogic.kt`
  and are unit-tested.
- **Searchable, categorized format list.** The home screen groups the supported
  formats into categories (Documents, Spreadsheets, Presentations, Images, Code
  & config, Data, Archives) and offers a search field that filters them by label
  or extension. The categories and the filter both come from
  `render/SupportedFormats.kt`, the single source of truth, and the filter logic
  is unit-tested.
- **In-document find and copy-all.** For text-based content (plain text, source
  code, and structured data), the top bar exposes a find action that opens a
  search field, highlights every match, and lets you step through them; the
  match-finding itself is the pure, unit-tested `render/logic/TextSearch.kt`.
  Text content is selectable, and an explicit "Copy all text" action is
  available from the overflow menu.

## Building and running

### Prerequisites

- JDK 17 for the Gradle build (see the sandbox note below).
- An Android SDK with `platforms;android-34` and `build-tools;34.0.0`.
- The Gradle wrapper is checked in and targets Gradle 8.14.6, so no separate Gradle
  install is required.

### Point the build at your SDK

Either create a `local.properties` file at the project root (it is gitignored):

```properties
sdk.dir=/projects/sandbox/android-sdk
```

or export the SDK location into the environment:

```bash
export ANDROID_SDK_ROOT=/projects/sandbox/android-sdk
```

### Reproducible commands used in this environment

The Android SDK in this sandbox is installed at `/projects/sandbox/android-sdk`.
Two environment variables are required on every Gradle invocation here:

- `GRADLE_USER_HOME=/projects/sandbox/.gradle-home` - the `/root` home directory is
  read-only in this sandbox, so Gradle must use a writable home inside the
  workspace. Without this, Gradle fails to unpack its native library
  (`libnative-platform.so`). This directory also carries the sandbox-only build
  settings described below, keeping the committed project config portable.
- `ANDROID_SDK_ROOT=/projects/sandbox/android-sdk` - points Gradle/AGP at the
  installed SDK.

The build runs on JDK 17. Java 17 is required because a newer default JVM on the
sandbox `PATH` crashes Gradle's embedded Kotlin DSL script compiler; the JDK 17
location and the `ANDROID_USER_HOME` redirect (AGP also needs a writable
preferences directory because `/root/.android` is read-only) are configured in
`/projects/sandbox/.gradle-home/gradle.properties`, which is not committed.

Build the debug APK:

```bash
GRADLE_USER_HOME=/projects/sandbox/.gradle-home \
  ANDROID_SDK_ROOT=/projects/sandbox/android-sdk \
  ./gradlew assembleDebug
```

Run the JVM unit tests:

```bash
GRADLE_USER_HOME=/projects/sandbox/.gradle-home \
  ANDROID_SDK_ROOT=/projects/sandbox/android-sdk \
  ./gradlew testDebugUnitTest
```

The debug APK is produced at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Install on a real device

With a device connected over USB (USB debugging enabled) or an emulator running:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then launch Lumen from the launcher, or open any supported file from a
file manager and choose Lumen in the "Open with" sheet.

## Honest build status

The following was actually run in this sandbox from `/projects/sandbox/app-root`
with `GRADLE_USER_HOME=/projects/sandbox/.gradle-home` and
`ANDROID_SDK_ROOT=/projects/sandbox/android-sdk`, and these are the real results:

- `./gradlew assembleDebug` -> BUILD SUCCESSFUL. It produces
  `app/build/outputs/apk/debug/app-debug.apk` at roughly 17 MB. A harmless Gradle
  9.0 deprecation warning is printed; it does not affect the build.
- `./gradlew testDebugUnitTest` -> BUILD SUCCESSFUL, 134 unit tests, 0 failures,
  0 errors, 0 skipped. These cover the pure logic: renderer registry resolution,
  file loader MIME/extension/size guard logic, the CSV/TSV parser, the JSON and
  XML formatters, the YAML validation decision, the RTF converter, the
  code-language detection, the EPUB parser, the OOXML Office parsers (docx/xlsx/
  pptx), the archive lister, the back-navigation decision logic, the recent-files
  list logic, the in-document text-search matcher, and the supported-format
  search/grouping.

What was NOT run: instrumented tests, UI tests, and any emulator-based tests were
not executed, because this sandbox has no connected device, no emulator, and no KVM
to run one. The Compose UI, `AndroidView`/`WebView` rendering, SAF picker, and
intent handling are therefore verified by compilation and by the unit-tested pure
logic they build on, not by on-device execution. No claim is made here about any
test that was not actually run.

## Extending with new formats

Adding a new format is a localized change that does not touch the registry
resolution logic or the file loader:

1. Implement a `FileRenderer` in `app/src/main/java/com/offlineviewer/app/render/`.
   Provide `formatName`, `supportedMimeTypes`, `supportedExtensions`, and a Compose
   `Render(file, modifier)` body. Keep any parsing in a pure helper object so it can
   be unit tested; show an error or raw-text fallback on malformed input rather than
   throwing. The shared `RenderUi.kt` helpers (`RendererError`, `FallbackNotice`)
   keep error UX consistent.
2. Register it in `RendererRegistry.createDefault()`. Order only matters when two
   renderers claim the same MIME type or extension; place a more specific renderer
   before a more general one (as SVG precedes the image renderer).
3. Add the format to `render/SupportedFormats.kt` so the home-screen chips, the
   text-vs-binary decision, and the advertised set all stay in sync.
4. Mirror the new MIME types and extensions in
   `app/src/main/AndroidManifest.xml` (the VIEW MIME filter, the
   `octet-stream`-by-extension VIEW filter, and the SEND filter) so the app appears
   in "Open with" for the new type.
5. Add JVM unit tests for the new pure logic under `app/src/test/`.

## Project layout

```
app-root/
  app/
    src/main/java/com/offlineviewer/app/
      MainActivity.kt            # single activity, SAF + VIEW/SEND intent handling
      data/                      # FileLoader + pure FileLoaderLogic (size guard, MIME)
      render/                    # FileRenderer, RendererRegistry, all renderers, SupportedFormats
      ui/                        # ViewerScreen, ViewerViewModel, components, theme
    src/main/AndroidManifest.xml # no INTERNET permission; intent-filters mirror SupportedFormats
    src/test/                    # JVM unit tests for the pure logic
    build.gradle.kts
  gradle/libs.versions.toml      # single source of dependency versions
  settings.gradle.kts
  build.gradle.kts
  gradlew, gradlew.bat, gradle/wrapper/
```

## License

Lumen is free and open source software (FOSS), released under the
[MIT License](LICENSE) and built by Ingpras Codelabs. Copyright (c) 2026 Ingpras
Codelabs. You are free to use, study, modify, and redistribute it. The app is
completely free for everyone, with no ads, no telemetry, no accounts, and no cost.

Releasing Lumen under the MIT License covers the project's own original code; it
does not change the licensing of the third-party libraries it bundles, which remain
under their respective licenses as described below.

## License of bundled libraries

All third-party runtime libraries are under the Apache License 2.0: AndroidSVG,
Markwon, SnakeYAML, and the AndroidX / Jetpack Compose libraries. `org.json`
(Android built-in, used as a test dependency) is public domain. Logic marked "n/a"
in the format table is original to this project and is licensed under the MIT
License as stated above.
