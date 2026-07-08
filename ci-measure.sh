#!/usr/bin/env bash
# Fork-only CI measurement helper for upstream issue #16.
# android-emulator-runner executes each `script:` line in a separate shell,
# so all timing logic must live here in a single process.
set -e

phase="$1"

case "$phase" in
  record)
    T0=$(date +%s)
    ./gradlew assembleDebug assembleDebugAndroidTest
    T1=$(date +%s)
    rm -rf app/screenshots/debug/
    ./gradlew executeScreenshotTests -Precord
    T2=$(date +%s)
    {
      echo "### Record step timing"
      echo "| phase | seconds |"
      echo "|---|---|"
      echo "| gradle build (both APKs) | $((T1-T0)) |"
      echo "| shot record (install + tests + processing) | $((T2-T1)) |"
    } >> "$GITHUB_STEP_SUMMARY"
    ;;
  interaction)
    T0=$(date +%s)
    ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=core.InteractionTest
    T1=$(date +%s)
    echo "| interaction tests (gradle total) | $((T1-T0)) |" >> "$GITHUB_STEP_SUMMARY"
    ;;
  *)
    echo "usage: ci-measure.sh record|interaction" >&2
    exit 1
    ;;
esac
