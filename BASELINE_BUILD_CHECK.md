# Baseline Build Verification

This file exists only on the `baseline-build-check` branch to trigger the existing
`CI/CD Build` workflow (`.github/workflows/build.yml`) via a pull request.

Purpose: verify that the unmodified MPV EX codebase (commit f4ea63e) builds
successfully with `./gradlew assembleStandardRelease` on GitHub Actions (JDK 17)
before any feature work begins.

No application source code is changed by this branch. Do not merge.
