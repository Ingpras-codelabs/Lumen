# Offline Viewer

A completely free, **fully offline** native Android app (Kotlin + Jetpack Compose +
Material 3) for rendering file formats Android does not preview natively (SVG,
Markdown, and more). The app declares **no permissions at all** (explicitly no
`android.permission.INTERNET`) so it never touches the network.

This repository currently contains the project baseline (FEAT-001): a buildable,
single-activity Compose scaffold. File-format renderers are added in later features.

## Requirements

- JDK 17 (used for the Gradle build; see note below)
- Android SDK with `platforms;android-34` and `build-tools;34.0.0`
- Gradle wrapper (checked in) targets Gradle 8.14.6

## Project configuration

| Setting      | Value                     |
|--------------|---------------------------|
| Language     | Kotlin (JVM target 17)    |
| UI           | Jetpack Compose + Material 3 |
| compileSdk   | 34                        |
| minSdk       | 21 (broad device support) |
| targetSdk    | 34                        |
| applicationId| `com.offlineviewer.app`   |
| Permissions  | none (fully offline)      |

Dependency versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Building

Point the build at your Android SDK, either with a `local.properties` file
containing `sdk.dir=/path/to/android-sdk` or by exporting `ANDROID_SDK_ROOT`.

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# JVM unit tests
./gradlew testDebugUnitTest
```

### Sandbox / CI build notes

This project was scaffolded and verified in a sandbox with a few constraints that
may apply to similar CI environments:

- The default JVM on `PATH` was Java 25, which breaks Gradle's embedded Kotlin DSL
  script compiler. The build is pinned to **JDK 17** via `org.gradle.java.home`.
- `/root` was read-only, so `GRADLE_USER_HOME` must point at a writable directory
  and AGP's preferences directory is redirected via the `ANDROID_USER_HOME` system
  property.

Those overrides live in the `GRADLE_USER_HOME/gradle.properties` (not committed),
keeping the committed project config portable. The exact verified commands were:

```bash
GRADLE_USER_HOME=/projects/sandbox/.gradle-home \
  ANDROID_SDK_ROOT=/projects/sandbox/android-sdk \
  ./gradlew assembleDebug

GRADLE_USER_HOME=/projects/sandbox/.gradle-home \
  ANDROID_SDK_ROOT=/projects/sandbox/android-sdk \
  ./gradlew testDebugUnitTest
```

## Build status

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**, produces `app-debug.apk`.
- `./gradlew testDebugUnitTest` — **BUILD SUCCESSFUL**.

Instrumented/UI tests are not run here (no emulator/device available in the sandbox).
