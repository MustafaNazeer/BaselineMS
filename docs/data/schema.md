# Data schema

This document is the reference for the on device Room schema. The Data Engineer owns updates to it; the Database Administrator appends migration notes when the schema version is bumped.

The contract is append only at the `Session` level: completed sessions are immutable. Corrections produce a new session, never mutate an existing one.

## Phase 1 schema, dated 2026-05-07

**Status:** landed in Phase 1 commits 7499417 through 3535ee6. Database version 1. No migrations yet (none needed).

**Source files (absolute paths):**

- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/Enums.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/Converters.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/UserProfileEntity.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/UserProfileDao.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/SessionEntity.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/SessionDao.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/TestResultEntity.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/TestResultDao.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/AppDatabase.kt`

### Entity table

| Table | Kotlin entity | Purpose |
|-------|---------------|---------|
| `user_profile` | `UserProfileEntity` | Single row capturing the owner's static demographics (sex, dominant hand, height, optional MS subtype). Used to compute reference ranges and to label the exported PDF report. |
| `session` | `SessionEntity` | One row per weekly battery sitting. `startedAtEpochMs` is the entry timestamp; `completedAtEpochMs` is set when the orchestrator marks the session done. |
| `test_result` | `TestResultEntity` | One row per individual test within a session. Five rows per fully completed weekly session in v1. |

### Column tables

#### `user_profile`

| Column | Kotlin field | SQLite type | Nullable | Notes |
|--------|--------------|-------------|----------|-------|
| `id` (PK) | `id: String` | TEXT | no | UUID, generated client side. |
| `dateOfBirthEpochMs` | `dateOfBirthEpochMs: Long` | INTEGER | no | Epoch milliseconds. |
| `biologicalSex` | `biologicalSex: Sex` | TEXT | no | Stored as enum name via `Converters`. |
| `dominantHand` | `dominantHand: Hand` | TEXT | no | Stored as enum name. |
| `msTypeDisclosed` | `msTypeDisclosed: MSType` | TEXT | no | Defaults to `UNDISCLOSED`. |
| `heightCm` | `heightCm: Double` | REAL | no | Used for reference range lookup in Reporting. |
| `createdAtEpochMs` | `createdAtEpochMs: Long` | INTEGER | no | Defaults to `System.currentTimeMillis()`. |

#### `session`

| Column | Kotlin field | SQLite type | Nullable | Notes |
|--------|--------------|-------------|----------|-------|
| `id` (PK) | `id: String` | TEXT | no | UUID. |
| `startedAtEpochMs` | `startedAtEpochMs: Long` | INTEGER | no | Defaults to `System.currentTimeMillis()`. |
| `completedAtEpochMs` | `completedAtEpochMs: Long?` | INTEGER | yes | Null until orchestrator finalises the session. |
| `deviceInfo` | `deviceInfo: String` | TEXT | no | Frozen at session start (model plus Android version). |

#### `test_result`

| Column (DB name) | Kotlin field | SQLite type | Nullable | Notes |
|------------------|--------------|-------------|----------|-------|
| `id` (PK) | `id: String` | TEXT | no | UUID. |
| `session_id` | `sessionId: String` | TEXT | no | FK to `session.id`. Indexed. Mapped via `@ColumnInfo(name = "session_id")`. |
| `testType` | `testType: TestType` | TEXT | no | Enum name via `Converters`. |
| `startedAtEpochMs` | `startedAtEpochMs: Long` | INTEGER | no | Per test start. |
| `completedAtEpochMs` | `completedAtEpochMs: Long` | INTEGER | no | Per test end. |
| `qualityScore` | `qualityScore: Double` | REAL | no | 0..1 value from the test module's quality heuristic. |
| `featuresJson` | `featuresJson: String` | TEXT | no | Serialized `Map<String, Double>` (kotlinx.serialization). The per test feature key contract is owned by the Data Engineer and lives in this document under "Feature JSON contract" once Tap, Gait, Vision, SDMT, and Voice land. |
| `rawSensorRelativePath` | `rawSensorRelativePath: String?` | TEXT | yes | Relative path within `context.filesDir` for raw IMU traces (gait) when retained. Null for tests that do not retain raw signal. |

### Indices

| Table | Index | Columns | Justification |
|-------|-------|---------|---------------|
| `test_result` | (auto, unnamed) | `session_id` | Declared via `indices = [Index(value = ["session_id"])]`. Matches the only join key the orchestrator and Reporting use to fetch results for a session, so this index serves every `getForSession` call. Without it, longitudinal reporting would full scan `test_result` once per session displayed. |

Primary keys (`id` on every table) are indexed implicitly by SQLite. No additional indices are needed for Phase 1; longitudinal queries in Reporting (Phase 9) may require a covering index on `(session_id, testType)` or on `session.startedAtEpochMs` once the actual query plans are profiled. The DBA revisits this in Phase 9.

### Foreign keys

| Child table | Child column | Parent table | Parent column | Action |
|-------------|--------------|--------------|---------------|--------|
| `test_result` | `session_id` | `session` | `id` | `ON DELETE CASCADE` |

The cascade is verified end to end by `SessionDaoTest.deletingSessionCascadesToResults` (`/home/mustafa/src/MS-Battery/app/src/test/java/com/mustafan4x/msbattery/data/SessionDaoTest.kt` lines 50 to 68). Room enables SQLite foreign key enforcement by default, so `pragma foreign_keys = ON` is implicit.

There is intentionally no foreign key from `session` to `user_profile`. The product is single user; `user_profile` exists as a singleton row fetched by `UserProfileDao.getFirst()`.

### Conversion rules

`Converters` (`/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/Converters.kt`) registers four pairs of `@TypeConverter` methods, one pair per enum:

| Kotlin enum | Storage | Method pair |
|-------------|---------|-------------|
| `TestType` (TAP, GAIT, VISION, SDMT, VOICE) | TEXT (enum name) | `fromTestType` / `toTestType` |
| `Sex` (FEMALE, MALE, OTHER, UNDISCLOSED) | TEXT | `fromSex` / `toSex` |
| `Hand` (LEFT, RIGHT, AMBIDEXTROUS) | TEXT | `fromHand` / `toHand` |
| `MSType` (RRMS, PPMS, SPMS, CIS, UNDISCLOSED) | TEXT | `fromMSType` / `toMSType` |

Storing enums by `name()` rather than ordinal is intentional: it makes the on disk representation stable across reorderings of enum constants (a common foot gun if ordinals are used). Any future enum value addition is a non breaking change at the storage layer; removal or rename requires a migration.

UUIDs are generated client side via `UUID.randomUUID().toString()` and stored as TEXT. No `UUID` to byte array converter is needed for v1.

### DAO query patterns

| DAO | Method | Return | Reactive | Notes |
|-----|--------|--------|----------|-------|
| `UserProfileDao` | `insert(profile)` | `Unit` | suspend | `OnConflictStrategy.REPLACE` to keep singleton semantics. |
| `UserProfileDao` | `getFirst()` | `UserProfileEntity?` | suspend | `ORDER BY createdAtEpochMs ASC LIMIT 1`. Bounded to one row. |
| `SessionDao` | `insert/update/delete` | `Unit` | suspend | Standard CRUD. |
| `SessionDao` | `observeAll()` | `Flow<List<SessionEntity>>` | flow | Drives the home and history screens. Sort key is `startedAtEpochMs DESC`; uses no index, but the table has at most ~52 rows per year, so a full scan is acceptable. |
| `SessionDao` | `getById(id)` | `SessionEntity?` | suspend | Primary key lookup, O(log n). |
| `TestResultDao` | `insert(result)` | `Unit` | suspend | |
| `TestResultDao` | `getForSession(sessionId)` | `List<TestResultEntity>` | suspend | Indexed lookup on `session_id`, ordered by `startedAtEpochMs ASC`. |

There are no N+1 patterns in the Phase 1 query set: the orchestrator inserts at most one `test_result` row per test and reads sessions only by primary key. Reporting joins (Phase 9) will read `test_result` rows in batch via `getForSession` keyed off the indexed FK column, so they remain O(log n) per session lookup plus O(k) per result list.

### Database configuration

`AppDatabase` (`/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/data/AppDatabase.kt`):

- `version = 1`.
- `exportSchema = false` for Phase 1.
- `@TypeConverters(Converters::class)` registered at the database level.
- Three DAOs exposed: `userProfileDao()`, `sessionDao()`, `testResultDao()`.

#### Pending decision: flip `exportSchema` to `true` before Phase 5

The DBA recommends switching `exportSchema = true` and committing the generated `app/schemas/` directory **before any schema bump**, and at the latest before Phase 5 (Gait Validation Suite). Reasons:

1. Room emits a JSON snapshot per database version when `exportSchema = true`. That snapshot is the input to `MigrationTestHelper`, which is the canonical way to test that a `Migration` object preserves data and structure.
2. Without exported schemas, future migrations can be unit tested only by re running the new schema from scratch in memory; data preservation across the migration cannot be exercised.
3. Phase 5 produces validation data that beta users will accumulate. A botched migration after that point destroys real, hard to reproduce signal. The cost of recording schemas now is tiny; the cost of not having them when the first migration lands is meaningful.

The flip is a one line change in `AppDatabase` plus a Gradle `room { schemaDirectory(...) }` configuration in the KSP block. PM dispatches the Data Engineer when this is approved.

### Schema audit verdict, 2026-05-07

PASS, with one carry forward decision (flip `exportSchema` to `true` no later than Phase 5).

- Schema matches `SPEC.md` Section 8 entity, column, type, and default declarations.
- Foreign key on `test_result.session_id` to `session.id` declared with `ON DELETE CASCADE`; verified by `SessionDaoTest.deletingSessionCascadesToResults`.
- Index on `test_result.session_id` declared and justified by the only longitudinal join pattern.
- `Converters` covers every enum used in entities (TestType, Sex, Hand, MSType).
- Query patterns are sound: `Flow` for the screen that lists sessions, suspending for primary key and FK lookups, no scans of unbounded result sets.
- No N+1 risk in Phase 1 query patterns.
- `version = 1`, no migration code; correct for Phase 1.

## Future sections

- **Feature JSON contract.** Per test, the canonical feature key names, types, and units. Reporting and PDF export consume this contract. Example for gait: `{"cadence_spm": 112.3, "stride_length_m": 1.42, "step_time_cv": 0.045}`. The Data Engineer fills this in as each test module lands (Phases 2 through 8).
- **Migrations.** Every schema version bump is documented here with the `Migration` object, the rationale, and a Robolectric test that exercises the migration. Empty in Phase 1.
