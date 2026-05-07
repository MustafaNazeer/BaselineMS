# Future ideas

Features and directions explicitly deferred. Each entry follows the format: Idea, Why deferred, When to revisit, Notes for the implementer.

## UI customization beyond Material 3 baseline

- **Idea:** A polished, distinctive visual identity for the application beyond Material 3 defaults plus accessibility tokens. Possibly custom colour palettes, illustration system, micro animations, dark mode tuned for low light clinic settings.
- **Why deferred:** The user explicitly said this is not a high priority right now. v1 ships with Material 3 plus the accessibility token set in `docs/design/tokens.md`. Spending design cycles before the core technical work (gait pipeline, validation) ships would invert the priority.
- **When to revisit:** After Phase 11 (accessibility, beta, polish) ships. Probably together with a small visual refresh phase.
- **Notes for the implementer:** Stay accessibility first. Any custom palette must pass WCAG AA contrast in both light and dark themes. Coordinate with UI/UX Designer and Accessibility Specialist.

## iOS port

- **Idea:** Port the application to native iOS (Swift, SwiftUI, SwiftData, CoreMotion, AVFoundation). The spec was originally written for iOS before pivoting to Android because Android Studio runs on Linux and iOS native builds require macOS.
- **Why deferred:** The user does not currently have macOS access. Cross platform frameworks (React Native, Flutter) would weaken the deep sensor and DSP story. Android first is the right call.
- **When to revisit:** After Phase 5 (gait validation) is shipped on Android, when the validation numbers are concrete and the clinical narrative is proven on one platform. At that point, a port is feasible because the architecture is platform agnostic below the UI layer.
- **Notes for the implementer:** Reuse the existing spec sections 5 through 10 with minor language swaps. The Madgwick filter, gait pipeline, and feature set transfer one to one. The data model maps cleanly from Room to SwiftData. Distribution requires App Store review and a paid Apple Developer account.

## EHR integration

- **Idea:** Export results directly into Epic, Cerner, or other EHR systems via FHIR.
- **Why deferred:** Out of scope for v1. v1 produces a PDF the patient brings to their neurologist; EHR integration is a separate regulatory and integration effort.
- **When to revisit:** Only if the application is validated and adopted by clinics. Realistic horizon: 12 plus months post v1.
- **Notes for the implementer:** This becomes a regulated activity. Consult an FDA / SaMD specialist before scoping.

## FDA pathway

- **Idea:** Pursue 510(k) or De Novo classification for the application as Software as a Medical Device.
- **Why deferred:** v1 is positioned as wellness or research, with explicit non diagnostic non treatment disclaimers. The FDA pathway is a multi year, multi hundred thousand dollar regulatory project that requires clinical trials and a quality management system.
- **When to revisit:** Only if a partner organization (clinic, research institution, company) is committed to funding it.
- **Notes for the implementer:** Existing wellness positioning must remain unchanged until and unless the FDA pathway is formally entered. Do not soften the disclaimer language without regulatory sign off.

## Beta cohort recruitment platform

- **Idea:** A small companion website or recruitment funnel for MS patients interested in joining the beta cohort.
- **Why deferred:** v1 uses Play Store internal testing track plus direct outreach through MS support communities. A formal recruitment platform is overkill until the application has demonstrated value.
- **When to revisit:** Phase 11 plus.
- **Notes for the implementer:** Any data collected through such a platform is regulated under HIPAA or local equivalents if it links to health information. Default to "no PHI on the website."

## Multi language support

- **Idea:** Localize the application into Spanish, French, German, Arabic, Mandarin, etc. Particularly relevant given the global MS population skew toward Android.
- **Why deferred:** v1 ships in English to keep the validation surface small.
- **When to revisit:** After Phase 11. Coordinate with Clinical Validator: cognitive tests (SDMT) and reading passages (Voice Test) need culturally validated translations, not direct ones.
- **Notes for the implementer:** Use Android resource qualifiers from day one of the localization effort. The fixed reading passage in the Voice Test must be replaced with a passage validated for each target language.

## Additional clinical tests

- **Idea:** Add tests beyond the v1 five (e.g., Pinch test, balance via standing IMU, finger tapping with the device flat on a table, drawing a spiral for tremor analysis).
- **Why deferred:** v1 prioritizes depth over breadth. Five tests, fully validated, beats ten tests at half quality.
- **When to revisit:** Only after the v1 five are validated and shipped. New tests require their own clinical validation pass.
- **Notes for the implementer:** Each new test goes through the same Clinical Validator review and validation suite as the v1 tests. No exceptions.

## SQLCipher database encryption

- **Idea:** Replace the default Room file encryption (Android File Based Encryption, tied to user passcode) with SQLCipher for an additional layer of explicit application controlled encryption.
- **Why deferred:** Android FBE on Android 10 plus already encrypts the database file at rest. SQLCipher adds operational complexity and a dependency.
- **When to revisit:** Only if a real threat model surfaces a scenario where FBE is insufficient (e.g., a forensic adversary on a rooted device).
- **Notes for the implementer:** Coordinate with Security Engineer. If adopted, key storage moves to the Android Keystore. Feature flag the change so existing users are migrated cleanly.
