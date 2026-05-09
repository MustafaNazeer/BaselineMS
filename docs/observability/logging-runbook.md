# Logging runbook

**Status:** placeholder. The Observability Engineer writes this file in Phase 0.

## Conventions

1. **Log tag format:** `BaselineMS::<module>` (e.g., `BaselineMS::Gait`, `BaselineMS::AudioCapture`).
2. **Levels:**
   - `D` (debug): fine grained development information; not useful in release.
   - `I` (info): milestone events such as session started, session completed, test result persisted.
   - `W` (warn): recoverable issues such as a sensor briefly unavailable, a permission denied that the user can grant later.
   - `E` (error): failures that prevent the user from completing a session, persistence errors, etc.
3. **Redaction rules.**
   - **Never** log a `UserProfile` field.
   - **Never** log raw sensor samples.
   - **Never** log file contents.
   - Test feature values may be logged at `I` (they are summary statistics, not raw data).
4. **Storage.** In the development build, logs go to `Logcat`. In the release build, an additional in app file logger writes to `Context.filesDir/logs/` with a circular buffer (most recent 1 MB).

## Diagnostic dump (Phase 11)

The user can export the in app file logger's most recent buffer via the Share Intent. The dump is redacted in the same way as live logs (no PII, only summary statistics and error events).

## Notes for the Observability Engineer

The privacy posture (no `INTERNET` permission) means there is no remote logging. Do not propose adding it. Play Console crash reporting is the only off device telemetry, and it ships automatically with any Play Store release without code changes.
