# Threat model

**Status:** placeholder. The Security Engineer writes this file in Phase 0.

## Sections to be written in Phase 0

1. **Adversaries and capabilities.**
   - Curious roommate or family member with intermittent physical access to the unlocked phone.
   - Lost or stolen phone, before or after device wipe.
   - Malicious app on the same device, no root.
   - Forensic adversary on a rooted device.
   - Network adversary (theoretically out of scope because the application has no network presence, but worth confirming).
2. **Assets.**
   - User profile (date of birth, sex, dominant hand, MS type, height).
   - Session history (timestamps, test results, raw IMU traces).
   - Generated PDF reports.
3. **Controls.**
   - No `INTERNET` permission.
   - Room database in app private storage, encrypted at rest by Android File Based Encryption (Android 10 plus).
   - No account; no credential to compromise.
   - Microphone audio not retained (only computed features).
   - User initiated export only; nothing leaves the device automatically.
4. **Residual risks.** Document what remains.

## Notes for the Security Engineer

Pair this file with `docs/security/hardening-checklist.md` (the enforceable rules) and the Phase 0 dependency audit. Update at every phase that introduces a new dependency, a new permission, or a new file written outside the app sandbox.
