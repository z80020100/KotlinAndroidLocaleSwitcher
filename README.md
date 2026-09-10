# LocaleSwitcher

Change the device language to English (United States) or Japanese.
Each selection replaces the system language list with `en-US` or `ja-JP`.
The app requires Android 11 or later.
Compatibility with each Android release and manufacturer requires device testing.

This file is the shared project documentation for developers and coding agents.
`CLAUDE.md` and `AGENTS.md` are relative symbolic links to `README.md`.
Edit this file to update all three entry points.

## Build and install

Use Java 25 and the Android SDK required by `app/build.gradle.kts`.

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.example.localeswitcher android.permission.CHANGE_CONFIGURATION
adb shell appops set com.example.localeswitcher WRITE_SETTINGS allow
```

Open LocaleSwitcher after both grants.
Select English or Japanese.
The app reads the system configuration after the request to check the result.
Android can recreate the screen during the change.
Check the current device languages after recreation.

The manifest declares both permissions. Declaration alone does not grant access.
`CHANGE_CONFIGURATION` requires the ADB permission grant.
`WRITE_SETTINGS` requires special access. The ADB command above enables that access.
Both grants are required before the app enables the language buttons.
Uninstalling the app removes the grants. Repeat both commands after reinstalling.
To revoke access:

```sh
adb shell pm revoke com.example.localeswitcher android.permission.CHANGE_CONFIGURATION
adb shell appops set com.example.localeswitcher WRITE_SETTINGS default
```

## Implementation

The app calls the Android internal `LocalePicker.updateLocales` method through
[AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass).
This library handles hidden API access. It does not grant system permissions.
The implementation follows the
[Android 11 LocalePicker](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/core/java/com/android/internal/app/LocalePicker.java).
That method updates the persistent configuration and notifies the settings backup service.
Hidden APIs can change between Android releases.

## Device validation

1. Install without granting permission. Check that both buttons are disabled.
2. Grant only `CHANGE_CONFIGURATION`. Reopen the app. Check that both buttons remain disabled. Then grant `WRITE_SETTINGS` access and reopen the app. Check that both buttons are enabled.
3. Select Japanese. Check the system settings language and the current language list.
4. Select English. Check the system settings language and the current language list.
5. Restart the device. Check that the selected language persists.
6. Revoke each permission separately. Reopen the app after each change. Check that either missing permission disables both buttons.

These checks change the device language. Run them only on a device approved for testing.

## Project structure

The app uses Kotlin and Jetpack Compose with Material 3.
The compile SDK and target SDK are 37. The minimum SDK is 30.
The Gradle daemon requires Java 25. Java source compatibility is 11.

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/example/localeswitcher/MainActivity.kt` | Language buttons and screen state. Refreshes permission checks when the activity resumes. |
| `app/src/main/java/com/example/localeswitcher/SystemLocaleSwitcher.kt` | Permission checks and hidden API calls. Reads the system configuration to verify the change. |
| `app/src/main/java/com/example/localeswitcher/ui/theme/` | Compose theme. |
| `app/src/main/AndroidManifest.xml` | Activity registration and permission declarations. |
| `app/src/main/res/values/strings.xml` | Screen text and result messages. |
| `app/src/test/` | Local unit tests. |
| `app/src/androidTest/` | Tests that require an Android device or emulator. |
| `app/build.gradle.kts` | Android build configuration and dependencies. |
| `gradle/libs.versions.toml` | Dependency versions. |
| `gradle/gradle-daemon-jvm.properties` | Gradle daemon Java requirement. |

## Diagnostics

Start Logcat before selecting a language:

```sh
adb logcat -v threadtime 'LocaleSwitcher:V' '*:S'
```

The `LocaleSwitcher` tag reports permission checks and API call stages.
Failures include exception types and stack frames. Raw exception messages are omitted to avoid exposing device data.
The app also reports activity recreation and whether the resulting configuration matches the requested language.

| Log field or event | Meaning |
| --- | --- |
| `requiredPermissionsGranted=false` | At least one required permission is missing. The buttons remain disabled. |
| `changeConfigurationGranted=false` | The ADB configuration permission grant is missing. |
| `Write settings access checked. allowed=false` | The app cannot modify system settings. |
| `stage=update_locales` | The failure occurred while preparing or invoking `LocalePicker.updateLocales`. |
| `stage=get_service` | The failure occurred while obtaining the system service for verification. |
| `stage=get_configuration` | The failure occurred while reading or checking the system configuration. |
| `matchesRequestedLocales=true` | The system configuration matched the requested language list when checked. |

A returned API call alone does not prove that the language changed.
A successful readback does not prove that the language persists after a restart.

## Local validation

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
git diff --check
```

Run long builds and test suites in the background. Read summarized output instead of full logs.
The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.
The lint report is `app/build/reports/lint-results-debug.html`.
Unit test results are under `app/build/test-results/testDebugUnitTest/`.

The current unit test checks template arithmetic. It does not test system language changes.
The current instrumented test checks the application package name.
Build success and lint success do not replace the device validation steps above.

## Rules for changes

- Change only the requested scope. Preserve unrelated work and existing document ordering.
- Use the existing Android and Compose APIs where possible. Check existing open-source solutions before adding a new tool.
- Keep system permission checks separate from hidden API access. HiddenApiBypass does not grant permissions.
- Keep language changes off the main thread. Check the system configuration before reporting success.
- Keep diagnostic output free of credentials and device identifiers. Use obviously fake values in examples and fixtures.
- Read and modify repository files without requesting approval. Ask before external actions or actions that are difficult to reverse.
- Do not install the APK or change a device language without authorization for that device.
- Split work into independently reviewable units. Stop after each unit.
- Run `/pre-commit` before creating a commit or pull request.
- Write commit messages as one line unless the repository history establishes a body convention.
- Respond in Traditional Chinese. Write code comments and GitHub technical content in English unless the existing content uses another language.
- Do not join clauses with commas. Use separate sentences or conjunctions.
- Apply `asd-ste100` to English instructions and diagnostic messages. Use its STE-flavored mode for documentation and commit messages.
