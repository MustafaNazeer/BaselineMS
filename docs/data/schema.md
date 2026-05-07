# Data schema

**Status:** placeholder. The Data Engineer writes this file in Phase 1.

## Sections to be written in Phase 1

1. **Entities.** `UserProfileEntity`, `SessionEntity`, `TestResultEntity`. Field by field, with type, units, and nullability.
2. **Relationships and cascade rules.** `Session` to `TestResult` is one to many with `ON DELETE CASCADE`.
3. **Indices.** At minimum `test_result.session_id`. Additional indices justified by query plans.
4. **Type converters.** Enum to string for `TestType`, `Sex`, `Hand`, `MSType`. UUID to string for primary keys.
5. **Feature JSON contract.** Per test, the canonical feature key names, types, and units. Reporting and PDF export consume this contract. Example: `{"cadence_spm": 112.3, "stride_length_m": 1.42, "step_time_cv": 0.045}`.
6. **Migrations.** Every schema version bump documented with the `Migration` object and a Robolectric test that exercises the migration.

## Notes for the Data Engineer

The contract is append only at the `Session` level: completed sessions are immutable. Corrections produce a new session, never mutate an existing one. The DBA reviews schema changes for migration safety and indexing.
