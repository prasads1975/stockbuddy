# Chainway SDK location

Place the Chainway C72 SDK files here:
- `chainway-sdk.aar` (or whatever the actual filename is — update app/build.gradle.kts to match)
- Any supporting .jar files the SDK ships with

This folder is already wired into the Gradle build via `flatDir` (see root settings.gradle.kts)
and `implementation(name: "chainway-sdk", ext: "aar")` in app/build.gradle.kts (currently
commented out until the real SDK files are dropped in here).

See hardware/ChainwayScannerManager.kt for where SDK calls get wired in.
