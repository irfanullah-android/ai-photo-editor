# Fix Hilt Kotlin Metadata Version Mismatch

The error `[Hilt] Unable to read Kotlin metadata due to unsupported metadata version` occurs because the version of Hilt being used (2.51.1) is not compatible with the Kotlin metadata produced by Kotlin 2.1.0. Upgrading Hilt to a newer version (2.60.1) should resolve this incompatibility.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///F:/irfanullah/android studio running projects/FotoNova/gradle/libs.versions.toml)
- Update `hilt` version from `2.51.1` to `2.60.1`.

#### [MODIFY] [build.gradle.kts (root)](file:///F:/irfanullah/android studio running projects/FotoNova/build.gradle.kts)
- Clean up plugin declarations to use `alias` where possible for consistency, although the primary fix is the Hilt version update.
- Ensure AGP version is consistent (optional but recommended).

#### [MODIFY] [collage-maker/build.gradle.kts](file:///F:/irfanullah/android studio running projects/FotoNova/Feature/collage-maker/build.gradle.kts)
- Update `jvmTarget` and `JavaVersion` to `17` to match the `:app` module, preventing potential bytecode version mismatches.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kaptDebugKotlin` to verify the Hilt annotation processor now successfully reads the Kotlin metadata.
- Run a full build: `./gradlew assembleDebug`.

### Manual Verification
- Verify that the app builds and runs successfully in Android Studio.
