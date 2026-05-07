# Hardening checklist

**Status:** placeholder. The Security Engineer writes this file in Phase 0.

## Rules every PR is checked against

(To be enumerated in Phase 0. Initial set:)

1. `AndroidManifest.xml` does not declare `<uses-permission android:name="android.permission.INTERNET" />`.
2. No third party SDKs that perform network I/O are added without explicit Security Engineer sign off documented in this file.
3. No PII (UserProfile fields, raw sensor traces, audio samples) appears in any log statement.
4. ProGuard or R8 is enabled in the release build.
5. The signing keystore is stored outside the repository, referenced from a gitignored `keystore.properties`.
6. Every new dependency is audited before it lands on `main`. The audit names: dependency, version, license, transitive dependencies, network behavior, privacy implications.
7. Any file written outside `Context.filesDir` (the app private files dir) requires Security Engineer sign off and is documented in the threat model.

## Notes for the Security Engineer

This list is meant to be enforceable mechanically. Where possible, add a CI check that fails the build if a rule is violated (e.g., a manifest scanner that fails on `INTERNET`).
