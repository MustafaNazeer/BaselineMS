# Data schema

This document is the reference for the on device Room schema. The Data Engineer owns updates to it; the Database Administrator appends migration notes when the schema version is bumped.

The contract is append only at the `Session` level: completed sessions are immutable. Corrections produce a new session, never mutate an existing one.

## Phase 1 schema, dated 2026-05-07

**Status:** landed in Phase 1 commits 7499417 through 3535ee6. Database version 1. No migrations yet (none needed).

**Source files (absolute paths):**

- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/Enums.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/Converters.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/UserProfileEntity.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/UserProfileDao.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/SessionEntity.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/SessionDao.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/TestResultEntity.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/TestResultDao.kt`
- `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`

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

The cascade is verified end to end by `SessionDaoTest.deletingSessionCascadesToResults` (`/home/mustafa/src/BaselineMS/app/src/test/java/com/mustafan4x/baselinems/data/SessionDaoTest.kt` lines 50 to 68). Room enables SQLite foreign key enforcement by default, so `pragma foreign_keys = ON` is implicit.

There is intentionally no foreign key from `session` to `user_profile`. The product is single user; `user_profile` exists as a singleton row fetched by `UserProfileDao.getFirst()`.

### Conversion rules

`Converters` (`/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/Converters.kt`) registers four pairs of `@TypeConverter` methods, one pair per enum:

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

`AppDatabase` (`/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`):

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

## Phase 2 schema decisions, 2026-05-07

This section is the Data Engineer's response to the Phase 2 prep dispatch. It resolves the three open items the PM flagged in `STATUS.md` Resume notes and in the Patient Advocate Phase 1 review (`docs/qa/patient-advocate-reviews.md`, 2026-05-07 entry, Issues 4 and 17, plus the Uncertainties section). The Database Administrator reviews this section after the Data Engineer hands back; the Android Engineer applies the approved changes in Phase 2B Task 5.

The three items are independent. Each one is decided on its own merits and named explicitly so the DBA review can ratify, modify, or veto each one in isolation.

### Decision 1, `UserProfileEntity.heightCm` becomes nullable in v1: VERDICT YES, nullable

**Verdict:** make `heightCm` nullable (`Double?`) in v1, defaulting to `null` for the "Skip for now" onboarding path Patient Advocate Issue 4 proposes. Bump the database to version 2 with a Room `Migration(1, 2)` object that alters the column to allow nulls.

**Rationale:**

1. **Retention is the load bearing argument.** Galati 2024 (`docs/source/clinical-references.md`) is the empirical anchor for treating onboarding friction as a measurable retention floor: registration to activation rose from 53.9 percent to 74.6 percent in the Floodlight Open cohort after onboarding was simplified, a 20.7 percentage point gain from removing pre test gating. The Patient Advocate's Issue 4 cites this exact mechanism. A profile screen that demands a height value before the first test runs is the single category of onboarding friction Galati 2024 specifically quantified as recoverable.
2. **Dignity.** Per Patient Advocate framing concern 5 (mobility aid users) and standing objection 5 (onboarding gating), the user must not be forced to disclose body metrics to use the application. `Sex` and `MSType` already default to `UNDISCLOSED`; allowing `heightCm` to be null aligns the schema with the same posture.
3. **Reporting null handling is a Phase 9 concern, not a Phase 2 blocker.** Height is consumed by exactly one downstream feature: the gait stride length normative comparison against Givon 2009 height stratified reference values, per `docs/source/clinical-references.md`. That comparison is a Phase 9 reporting concern. The Reporting layer can degrade gracefully when height is null by reporting raw stride length without the height stratified comparison line, and labeling that case in the PDF ("comparison to height stratified norms requires a height value; add one in Settings to enable this comparison"). This is the same pattern the application already uses for `MSType.UNDISCLOSED`. No Phase 2 code consumes height, so no Phase 2 module needs to handle null specially.
4. **The schema has not been exported yet.** Per `STATUS.md` Resume notes item 2 and the existing pending decision section above ("Pending decision: flip `exportSchema` to `true` before Phase 5"), `exportSchema = false` today. This is the cheapest moment in the project's history to change a column nullability: there is no captured v1 JSON snapshot to migrate from. See Recommendation 3 below for how this interacts with the Phase 2 sequencing.

**Trade off explicitly named for the DBA:**

There are two implementation paths and they have different risk profiles. The DBA picks between them.

- **Path A, version bump with Migration object.** Treat the change as a real schema bump from version 1 to version 2 even though no production user has installed v1 yet. Add a `Migration(1, 2)` that runs `ALTER TABLE user_profile RENAME TO user_profile_old; CREATE TABLE user_profile (... heightCm REAL); INSERT INTO user_profile SELECT * FROM user_profile_old; DROP TABLE user_profile_old`. This is the SQLite idiom for changing a column constraint (SQLite cannot drop NOT NULL in place). The cost is more code and a Robolectric `MigrationTestHelper` test that requires an exported v1 schema (see Recommendation 3 below). The benefit is establishing the migration discipline early and exercising the `MigrationTestHelper` workflow before the first migration that carries real user data.
- **Path B, in place change with `fallbackToDestructiveMigration` only on dev builds.** Bump the version to 2 but accept that any existing dev install loses its data. This is acceptable because no production user exists. The cost is that the migration discipline is not exercised; the benefit is one line of code instead of fifteen and no migration test to maintain.

**Data Engineer recommendation: Path A.** Reasons:

1. The migration discipline is going to land somewhere. Phase 2 is a smaller, less risky place for the first migration to land than any later phase that also carries real Givon 2009 reference comparison code.
2. Path A is what the DBA will need to write in Phase 4 or Phase 5 anyway when the first real migration lands. Writing it now under cheap conditions (no real user data) reduces the bus factor on migration knowledge.
3. Path B's "destructive migration" flag is a foot gun: if it is ever forgotten and shipped to production, a real user loses their session history on the next schema bump. Path A teaches the team to never reach for `fallbackToDestructiveMigration` in any build flavor.

**Path A migration code, ready for the Android Engineer to apply in Phase 2B Task 5:**

```kotlin
// /home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/Migrations.kt
package com.mustafan4x.baselinems.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_profile_new (
                id TEXT NOT NULL PRIMARY KEY,
                dateOfBirthEpochMs INTEGER NOT NULL,
                biologicalSex TEXT NOT NULL,
                dominantHand TEXT NOT NULL,
                msTypeDisclosed TEXT NOT NULL,
                heightCm REAL,
                createdAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO user_profile_new (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
            SELECT id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs FROM user_profile
            """.trimIndent()
        )
        db.execSQL("DROP TABLE user_profile")
        db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
    }
}
```

The `UserProfileEntity` change the Android Engineer applies alongside the migration:

```kotlin
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateOfBirthEpochMs: Long,
    val biologicalSex: Sex,
    val dominantHand: Hand,
    val msTypeDisclosed: MSType = MSType.UNDISCLOSED,
    val heightCm: Double? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
```

The `AppDatabase.kt` change:

```kotlin
@Database(
    entities = [
        UserProfileEntity::class,
        SessionEntity::class,
        TestResultEntity::class,
    ],
    version = 2,
    exportSchema = true
)
```

(`version = 2` and `exportSchema = true` are coupled; see Recommendation 3.)

The `Room.databaseBuilder(...)` call site (in the Hilt module or wherever the database is constructed) gains `.addMigrations(MIGRATION_1_2)`. The Data Engineer flags this as a search needed by the Android Engineer; the Phase 1 wiring location is whatever module assembled the singleton in commits `7499417` through `3535ee6` and the Android Engineer is the role that owns that file.

**Robolectric migration test that the Android Engineer or DBA writes alongside Path A:**

```kotlin
// app/src/test/java/com/mustafan4x/baselinems/data/Migration1To2Test.kt
@RunWith(RobolectricTestRunner::class)
class Migration1To2Test {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesProfileRow_andAllowsNullHeight() {
        helper.createDatabase("test.db", 1).use { db ->
            db.execSQL(
                """
                INSERT INTO user_profile (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
                VALUES ('u1', 0, 'FEMALE', 'RIGHT', 'UNDISCLOSED', 168.0, 0)
                """.trimIndent()
            )
        }
        helper.runMigrationsAndValidate("test.db", 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT heightCm FROM user_profile WHERE id = 'u1'").use { c ->
                c.moveToFirst()
                check(c.getDouble(0) == 168.0)
            }
            db.execSQL(
                """
                INSERT INTO user_profile (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
                VALUES ('u2', 0, 'FEMALE', 'RIGHT', 'UNDISCLOSED', NULL, 0)
                """.trimIndent()
            )
        }
    }
}
```

The migration test depends on an exported v1 schema being present on disk. That dependency is exactly why Recommendation 3 below recommends flipping `exportSchema = true` in Phase 2 rather than waiting for Phase 4.

### Decision 2, `UserProfileEntity` mutability for v1: VERDICT MUTABLE

**Verdict:** keep `UserProfileEntity` mutable. The "Edit profile" path Patient Advocate Issue 17 proposes updates the same row via `UserProfileDao.insert(profile)` with `OnConflictStrategy.REPLACE` (the existing v1 insert behavior). No schema change. No new query.

**Rationale:**

1. **SPEC.md Section 8 says append only "at the Session level."** The exact wording is "Append only at the `Session` level: a completed session is immutable; corrections produce a new session." It is intentionally specific about which entity the rule applies to. Extending the same rule to `UserProfileEntity` is a stricter posture than the spec requires; it would need a SPEC.md amendment. The Data Engineer is not authorized to amend SPEC.md unilaterally and would not recommend the amendment regardless (see point 4).
2. **The audit trail argument is weaker than it looks for this entity.** A profile correction means the user is telling us "the height I entered earlier was wrong." Preserving the wrong value as a historical record does not strengthen the clinical signal; it pollutes any retroactive height stratified comparison with a value the user has explicitly disowned. The Givon 2009 norms are computed against the user's actual height. If the user enters 168 cm by mistake on day one and corrects to 178 cm on day thirty, every Reporting screen that runs a height stratified comparison should use 178 cm for both old and new sessions, not 168 cm for the first thirty days. Mutable in place gives this for free; immutable with newest read latest still works but adds complexity for no clinical gain.
3. **Cost of mutable: zero.** The user profile is a singleton row. The current `UserProfileDao.getFirst()` query already returns at most one row. The current `UserProfileDao.insert(profile)` with `OnConflictStrategy.REPLACE` already overwrites it on second insert. The "Edit profile" UX in Patient Advocate Issue 17 maps onto these existing operations with no DAO additions and no schema change.
4. **Cost of immutable: real and recurring.** Immutable would require: a new DAO query (`SELECT * FROM user_profile ORDER BY createdAtEpochMs DESC LIMIT 1`), a new convention every consumer of the profile must follow ("always read the latest, never read by id"), and a row count that grows monotonically with every correction. The latter is a small cost in absolute terms (a user might edit profile five times in a year), but it is a recurring schema invariant that future code has to maintain forever. SPEC.md does not ask for this. The Patient Advocate raised it as an uncertainty (Uncertainties section item 3), not as a recommendation.
5. **Audit trail can be added later if a real need surfaces.** If a future phase produces a clinical reason to preserve historical profile values, the Data Engineer can introduce a `user_profile_history` table that captures each prior version on edit, leaving the live `user_profile` row mutable. That migration is straightforward when and if motivated. Building the audit trail speculatively, with no concrete consumer, is YAGNI.

**No schema change. No new DAO method. The Android Engineer wires the "Edit profile" button in Settings to navigate to the existing `ProfileSetupScreen` pre populated with the row returned by `UserProfileDao.getFirst()`, and the existing Save handler calls `userProfileDao.insert(profile)` which already overwrites the row.**

The one DAO behavior the Android Engineer must preserve: `OnConflictStrategy.REPLACE` on `UserProfileDao.insert` is what makes this work. The Data Engineer confirms this is already the v1 behavior per `docs/data/schema.md` Phase 1 entry (DAO query patterns table, "OnConflictStrategy.REPLACE to keep singleton semantics"). The Phase 2 Android Engineer must not change it without dispatching back to the Data Engineer.

### Recommendation 3, flip `exportSchema = true` in Phase 2: VERDICT FLIP NOW

**Verdict:** flip `exportSchema = true` in Phase 2, in the same patch that lands the Decision 1 migration. Specifically, flip it before `version = 2` is committed, so a v1 JSON snapshot is captured first, then the migration runs, then a v2 snapshot is captured. The DBA's earlier recommendation named Phase 4 close as the latest acceptable moment; Phase 2 is the earliest acceptable moment, and Decision 1 above turns "earliest" into "now or never for v1."

**Rationale:**

1. **Decision 1 forces the issue.** The migration test sketched above needs an exported v1 schema JSON to validate the migration against. Without it, `MigrationTestHelper.createDatabase("test.db", 1)` cannot synthesize a v1 schema and the test will not run. Flipping `exportSchema = true` after the migration ships means the v1 snapshot is gone forever; only v2 is captured. That is exactly the failure mode the existing pending decision section in this document warned about.
2. **The Phase 4 close argument was framed around "before any schema bump."** Decision 1 is a schema bump. So the Phase 4 deadline becomes the Phase 2 deadline by virtue of Decision 1 landing in Phase 2. If the PM ratifies Decision 1, Recommendation 3 is consequential, not optional.
3. **The flip is two small file changes and a build configuration.** No production behavior change. No test failures. The KSP code generation step gains a `room.schemaLocation` argument and emits one JSON file per database version into `app/schemas/`. Those files are committed alongside the source to give the migration test a stable input.

**Concrete changes the Android Engineer or DevOps Engineer applies in Phase 2B Task 5:**

In `/home/mustafa/src/BaselineMS/app/build.gradle.kts`, after line 47 (the closing brace of the `android { ... }` block) and before line 49 (the `dependencies { ... }` block opening), add:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

The Data Engineer flags an alternative path explicitly: Room 2.6.0 and later support a dedicated `androidx.room` Gradle plugin that exposes a `room { schemaDirectory("$projectDir/schemas") }` block. This plugin is more idiomatic than passing the schema location as a KSP argument, and it is what the Room documentation now recommends. Adopting the plugin requires adding `id("androidx.room") version "2.6.1"` to the plugins block and the corresponding entry to `gradle/libs.versions.toml` if version catalogs are in use. The DBA picks between the KSP arg form and the Room plugin form; both produce the same on disk artifact (a `schemas/` directory with one JSON per database version).

In `/home/mustafa/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`, change:

```kotlin
exportSchema = false
```

to

```kotlin
exportSchema = true
```

The first build after this change emits `app/schemas/com.mustafan4x.baselinems.data.AppDatabase/1.json` (the v1 snapshot). That file must be committed before the `version = 2` change lands, in a separate commit or at least a separate file change reviewed independently. The migration test (Decision 1, Path A) then runs against that snapshot. The second build, after the `version = 2` change, emits the v2 snapshot; both snapshots stay committed.

The Data Engineer flags the commit ordering as load bearing for the DBA and PM: if the v1 snapshot is captured after the v2 schema lands, the snapshot is wrong (it captures v2 metadata under filename `1.json`). The safe ordering is:

1. Patch 1: flip `exportSchema = true` only. Build. Commit `1.json`.
2. Patch 2: bump to `version = 2`, add `MIGRATION_1_2`, change `heightCm` to nullable. Build. Commit `2.json` plus the source changes.
3. Patch 3: write `Migration1To2Test`. Run. Commit.

The DBA can collapse 1 and 2 if desired by running the build twice locally and committing both snapshots together, but the conceptual ordering must be preserved (v1 snapshot captured against the unchanged v1 source code).

### Decisions summary table

| # | Item | Verdict | Owner of next action |
|---|------|---------|----------------------|
| 1 | `heightCm` nullable in v1 | YES, with Path A migration | Android Engineer applies in Phase 2B Task 5; DBA reviews migration code and Robolectric test |
| 2 | `UserProfileEntity` mutability | MUTABLE (no schema change) | Android Engineer wires Edit profile path in Phase 2B; no Data Engineer or DBA action |
| 3 | `exportSchema = true` flip | FLIP IN PHASE 2 (forced by Decision 1) | Android Engineer or DevOps applies in Phase 2B Task 5; DBA decides KSP arg vs Room plugin form |

### Uncertainties the Data Engineer flags for the DBA and PM

1. **Room plugin vs KSP arg form for `exportSchema`.** Both are correct. The Data Engineer recommends the Room plugin form on style grounds (matches Google's current docs) but does not insist on it. The DBA decides. The migration test works identically against either.
2. **The `Room.databaseBuilder(...)` call site for `addMigrations(MIGRATION_1_2)`.** The Data Engineer did not grep the Phase 1 source to locate the exact builder file because that is the Android Engineer's territory per `agents/05-data-engineer.md` ("Stay strictly in role. Do not write Compose UI."). The Android Engineer locates the builder and adds the migration in Phase 2B Task 5.
3. **`SettingsScreen` "Edit profile" navigation route.** Decision 2 says reuse `ProfileSetupScreen` with prepopulated values. The Data Engineer is not specifying the navigation argument shape (route with no args and the screen pulls the row, vs. route with profile id arg); that is the Android Engineer's call.
4. **No SPEC.md amendment is requested.** Decisions 1 and 2 are compatible with SPEC.md as written. The Patient Advocate flagged Issue 4 as a SPEC.md change in their entry; the Data Engineer's read is that adding nullability to a Room column is a schema implementation detail, not a spec change. SPEC.md Section 8's data class declaration is illustrative of the entity shape; it is not a regulatory contract that requires an amendment for a column type change. The PM may disagree and amend SPEC.md anyway; the Data Engineer flags this for visibility.

## Phase 2 schema audit, DBA verdict, 2026-05-07

This section is the Database Administrator's review of the Data Engineer's "Phase 2 schema decisions, 2026-05-07" section above. It ratifies, modifies, or defers each decision named there. The Android Engineer applies the modifications in Phase 2B Task 5.

The DBA verified the Phase 1 source files before issuing this verdict: `AppDatabase.kt` (version 1, `exportSchema = false`), `UserProfileEntity.kt` (`heightCm: Double` non nullable), `UserProfileDao.kt` (`@Insert(onConflict = OnConflictStrategy.REPLACE)` and `getFirst()` with `ORDER BY createdAtEpochMs ASC LIMIT 1`), `SessionEntity.kt` (no FK to `user_profile`), `TestResultEntity.kt` (single FK from `test_result.session_id` to `session.id`, no FK to `user_profile`), and `app/build.gradle.kts` (KSP plugin applied, no Room Gradle plugin applied, Room 2.6.1).

### Item 1, Decision 1, `Migration(1, 2)` migration code: VERDICT PASS with two minor follow ups

The migration SQL in Decision 1 Path A is correct, safe, atomic, and preserves data. The DBA ratifies it with the two follow ups named below; those follow ups do not block code work.

**Verified properties:**

1. **Column rename, copy, drop, rename pattern is the canonical SQLite idiom for relaxing a NOT NULL constraint.** SQLite has no `ALTER COLUMN`; this rebuild is the documented workaround.
2. **All seven columns (`id`, `dateOfBirthEpochMs`, `biologicalSex`, `dominantHand`, `msTypeDisclosed`, `heightCm`, `createdAtEpochMs`) are listed in both the new table definition and the `INSERT ... SELECT` statement, in the same order.** Every row's data, primary key, and timestamps are preserved.
3. **`heightCm REAL` (no `NOT NULL` qualifier) correctly relaxes the NOT NULL constraint.** Existing non null values flow through the `INSERT ... SELECT` unchanged; future inserts may pass `NULL`.
4. **Foreign key constraints are not affected.** The DBA verified by reading `SessionEntity.kt` and `TestResultEntity.kt`: no entity declares a foreign key referencing `user_profile`. The only foreign key in the schema is `test_result.session_id` to `session.id`, which is untouched by this migration. There is nothing to preserve across the `user_profile` rebuild.
5. **Idempotence under Room's contract.** Room invokes `Migration.migrate()` only when stepping the database forward from a strictly earlier version; it never re runs a migration once the database is at the target version. The migration as written is therefore idempotent in the sense Room requires. (It is not idempotent in isolation: re running `migrate()` against a v2 database would fail at `INSERT ... SELECT` because the source schema no longer matches. This is normal and expected.)
6. **Empty table case is correct.** A fresh install initializes the database directly at version 2 via Room's `createAllTables` path; the migration does not run at all. For an existing v1 install with zero `user_profile` rows (technically possible if the user opened the app but never completed onboarding before upgrading), `INSERT INTO user_profile_new ... SELECT ... FROM user_profile` selects zero rows and the migration completes cleanly. `DROP TABLE` and `ALTER TABLE ... RENAME TO` operate on an empty table without error.

**Follow up A (non blocking): foreign keys pragma.** SQLite's `PRAGMA foreign_keys = ON` is a per connection setting, and Room enables it by default. The migration as written is safe regardless because no foreign key references `user_profile`. The DBA flags this only as a forward looking note: if a future migration rebuilds a table that has incoming foreign keys, the standard idiom is to wrap the rebuild in `PRAGMA foreign_keys = OFF; ... PRAGMA foreign_keys = ON;` (or use `PRAGMA defer_foreign_keys = ON` inside a transaction) to prevent the `DROP TABLE` from cascading. Not applicable to MIGRATION_1_2; recorded here for the next migration that will need it.

**Follow up B (non blocking): Room schema validator strictness.** When `runMigrationsAndValidate(..., true, ...)` runs in the Robolectric test, Room compares the post migration schema against the v2 entity metadata. The CREATE TABLE in MIGRATION_1_2 must match exactly what Room generates for the v2 `UserProfileEntity`. The Data Engineer's CREATE TABLE matches Room's column types and nullability, but Room's generated SQL formats the primary key as a trailing `PRIMARY KEY(`id`)` clause rather than the inline `id TEXT NOT NULL PRIMARY KEY` form used in the migration. SQLite treats both forms as equivalent at the storage level, and Room's validator compares column metadata (name, affinity, nullability, default value, primary key position) rather than exact CREATE TABLE text, so this should pass. If the migration test fails the validator, switch the CREATE TABLE in MIGRATION_1_2 to the trailing primary key form: `CREATE TABLE user_profile_new (id TEXT NOT NULL, dateOfBirthEpochMs INTEGER NOT NULL, biologicalSex TEXT NOT NULL, dominantHand TEXT NOT NULL, msTypeDisclosed TEXT NOT NULL, heightCm REAL, createdAtEpochMs INTEGER NOT NULL, PRIMARY KEY(`id`))`. The Android Engineer applies this change only if the validator rejects the inline form.

### Item 2, Robolectric `MigrationTestHelper` test sketch: VERDICT REQUIRES CHANGE

The test sketch covers the load bearing case (a seeded v1 row preserves its data, a null `heightCm` is permitted post migration). It is missing two assertions and one constructor detail. The DBA names the changes the Android Engineer applies in Phase 2B Task 5; the test as sketched is not yet sufficient.

**Required changes:**

1. **Assert all seven columns of the seeded row, not just `heightCm`.** The migration's primary correctness property is full row preservation. The current test reads only `heightCm` and asserts its value. Extend the post migration query to `SELECT id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs FROM user_profile WHERE id = 'u1'` and assert each value matches what was seeded. This catches a class of bugs where the `INSERT ... SELECT` accidentally drops or reorders a column.
2. **Add an explicit empty table case.** A second test method, `migrate1To2_emptyTable_succeeds`, that creates a v1 database, does not seed any row, runs the migration, and asserts the post migration `user_profile` table exists with zero rows. This guards against the "user installed v1 but never completed onboarding" upgrade path.
3. **Verify `MigrationTestHelper` constructor for room-testing 2.6.1.** The two argument constructor `MigrationTestHelper(Instrumentation, Class<out RoomDatabase>)` is deprecated in recent Room versions in favor of the three argument form that takes a `FrameworkSQLiteOpenHelperFactory` (or the four argument form that adds a list of auto migration spec classes). The Android Engineer verifies the correct constructor signature against the actual `androidx.room:room-testing:2.6.1` API at write time. If the two argument form does not compile, switch to `MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java, FrameworkSQLiteOpenHelperFactory())`.
4. **Use `assertEquals` rather than `check`.** `check` throws `IllegalStateException` and produces an unhelpful failure message. `org.junit.Assert.assertEquals(168.0, c.getDouble(0), 0.0)` is the conventional form and gives a readable failure on regression. Style only; not load bearing.
5. **Assert the post migration null insert is readable as null.** The current test inserts a row with `heightCm = NULL` after running the migration, but does not query it back. Add a follow up `db.query("SELECT heightCm FROM user_profile WHERE id = 'u2'")` and assert `c.isNull(0)` is true. This proves the new nullability is end to end correct, not just accepted at insert time.

The Android Engineer applies all five changes when writing `Migration1To2Test.kt` in Phase 2B Task 5.

### Item 3, Decision 2, `UserProfileEntity` mutability via `OnConflictStrategy.REPLACE` + `getFirst()`: VERDICT PASS, with one Android Engineer constraint named explicitly

The DBA ratifies MUTABLE. The Data Engineer's reasoning is sound: SPEC.md Section 8 scopes append only to `Session`, the singleton row pattern is preserved by `OnConflictStrategy.REPLACE` keyed on the primary key (`id`), and immutable would force a new query and a new convention without a concrete clinical consumer. The audit trail can be retrofitted with a `user_profile_history` side table later if and when motivated.

**Constraint the Android Engineer must respect when wiring "Edit profile":**

`OnConflictStrategy.REPLACE` preserves the singleton invariant (one row in `user_profile`) only if the edit path reuses the existing row's `id`. The `UserProfileEntity` data class declares `id: String = UUID.randomUUID().toString()`, which means a naively constructed `UserProfileEntity(...)` in the Edit handler will mint a fresh UUID and `insert(profile)` will create a second row, not replace the first. Two rows in `user_profile` violates the singleton invariant assumed by `getFirst()` (which only returns the row with the earliest `createdAtEpochMs`).

The Edit profile flow must:

1. Load the existing row via `userProfileDao.getFirst()` first.
2. Construct the updated `UserProfileEntity` by `.copy(...)` on that loaded row, preserving `id` and `createdAtEpochMs` and overwriting only the user editable fields.
3. Call `userProfileDao.insert(updatedProfile)`. With the same `id`, `OnConflictStrategy.REPLACE` overwrites the existing row.

The DBA flags this as an Android Engineer constraint, not a schema change. If the Phase 2B implementation deviates from this pattern, the Code Reviewer catches it; the DBA additionally checks the resulting row count in the Phase 2 close audit (one row in `user_profile` after a profile edit, not two).

### Item 4, KSP arg form vs Room Gradle plugin form for `exportSchema`: VERDICT KSP arg form

The DBA picks the KSP arg form. Both produce the same `app/schemas/com.mustafan4x.baselinems.data.AppDatabase/<version>.json` artifact; the choice is stylistic and operational.

**Reasoning:**

1. **The current build does not apply the Room Gradle plugin.** Verified in `app/build.gradle.kts` lines 1 to 6: the plugins block applies `com.android.application`, `org.jetbrains.kotlin.android`, `com.google.devtools.ksp`, and `org.jetbrains.kotlin.plugin.serialization`. There is no `androidx.room` plugin and no `room { ... }` block. KSP is the existing code generation pipeline; passing one argument to it is a two line addition.
2. **The Room plugin form is more invasive in this repository.** Adopting it requires adding `id("androidx.room") version "2.7.x"` (or pinned via the version catalog) to the plugins block, declaring the plugin in `gradle/libs.versions.toml` if version catalogs are used (the alias pattern in `app/build.gradle.kts` line 2 suggests they are), and configuring the `room { schemaDirectory("$projectDir/schemas") }` block. This is multi file blast radius for a stylistic improvement.
3. **The Room plugin form is the documented future direction, and the migration to it is mechanical.** When the project picks up a future Room version where the plugin form is mandatory, or when DevOps does a Gradle modernization sweep, the swap is a search and replace plus three lines of plugin wiring. There is no lock in cost to choosing the KSP arg form now.

**Concrete change the Android Engineer or DevOps Engineer applies in Phase 2B Task 5:**

In `/home/mustafa/src/BaselineMS/app/build.gradle.kts`, after the `android { ... }` block (currently closes at line 47) and before the `dependencies { ... }` block (currently opens at line 49), add:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

The DBA also requires the generated `app/schemas/` directory to be committed to source control. Add no entry for it to `.gitignore`. The directory is a build input for migration tests, not a build output, and must be reviewable in pull requests.

### Item 5, commit ordering: VERDICT PASS, with one tightening

The DBA ratifies the Data Engineer's three patch ordering. The v1 snapshot must be captured against unmodified v1 source code before any v2 change lands. The conceptual ordering is correct; the DBA tightens it slightly to make the rollback story explicit.

**Ratified ordering:**

1. **Patch 1, "capture v1 schema."** Flip `exportSchema = true` in `AppDatabase.kt`. Add the `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` block in `app/build.gradle.kts`. Run `./gradlew :app:compileDebugKotlin` (or any build target that triggers KSP) to emit `app/schemas/com.mustafan4x.baselinems.data.AppDatabase/1.json`. Commit the source changes plus the generated `1.json` in one commit. Verify that `version = 1` is unchanged in this patch.
2. **Patch 2, "land the v2 migration."** In a separate commit: bump `version = 1` to `version = 2` in `AppDatabase.kt`, change `heightCm: Double` to `heightCm: Double? = null` in `UserProfileEntity.kt`, create `Migrations.kt` with the `MIGRATION_1_2` object as written in Decision 1, and add `.addMigrations(MIGRATION_1_2)` to the `Room.databaseBuilder(...)` call site. Run the build to emit `2.json`. Commit the source changes plus `2.json` in one commit.
3. **Patch 3, "exercise the migration."** In a separate commit: add `Migration1To2Test.kt` with the changes from Item 2 of this audit applied (full row assertion, empty table case, correct constructor, JUnit assertions, post migration null read back). Run `./gradlew :app:testDebugUnitTest` to confirm green. Commit the test plus any small fixes the test surfaces.

**Why three patches and not one:** the v1 snapshot in patch 1 is the input to the migration test in patch 3. If patches 1 and 2 are collapsed, the `1.json` snapshot is captured against v2 entity metadata under filename `1.json`, which is the failure mode the Data Engineer warned about. The DBA confirms it is also a serious bisect hazard: a developer who later runs `git checkout <commit before patch 2>` and rebuilds expects to see the v1 schema regenerated cleanly; if `1.json` was committed against v2 source, the regeneration mismatches and the build looks broken.

**Tightening the DBA adds:** before patch 2 runs, the developer should `rm -rf app/schemas` locally and rebuild from clean to regenerate `1.json` from the v1 source code, then `git diff` the regenerated `1.json` against the committed `1.json`. They must be byte for byte identical. If they are not, patch 1 captured the wrong snapshot and must be redone before patch 2 lands. This is a one minute check that prevents the bisect hazard above.

**Optional collapse:** patches 1 and 2 may be collapsed into a single commit if and only if the developer locally runs the build twice (once at v1 source to capture `1.json`, once at v2 source to capture `2.json`) and commits both JSON files together. The DBA prefers the three patch form for review clarity, but the collapsed form is acceptable if the developer can attest to the build twice protocol in the commit message.

### Decisions summary table, DBA verdicts

| # | Item | Data Engineer verdict | DBA verdict | Owner of next action |
|---|------|----------------------|-------------|----------------------|
| 1 | `Migration(1, 2)` code | YES, Path A | PASS, with two non blocking follow ups (FK pragma forward note, validator fallback CREATE TABLE form) | Android Engineer applies migration in Phase 2B Task 5 |
| 2 | Migration test sketch | provided | REQUIRES CHANGE: full row assertion, empty table case, constructor verification, `assertEquals`, null read back | Android Engineer applies the five required changes when writing `Migration1To2Test.kt` |
| 3 | `UserProfileEntity` mutability | MUTABLE | PASS, with explicit Android Engineer constraint to reuse `id` on edit | Android Engineer wires Edit profile path with `.copy(id = existing.id, createdAtEpochMs = existing.createdAtEpochMs)` |
| 4 | KSP arg vs Room plugin | DBA choice | KSP arg form (lower blast radius) | Android Engineer or DevOps applies two line `ksp { arg(...) }` block |
| 5 | Commit ordering | three patches | PASS, with byte equality check between regenerated and committed `1.json` before patch 2 | Android Engineer follows three patch protocol or attests to build twice protocol in collapsed commit |

### DBA uncertainties, flagged for the PM

1. **Whether the Hilt module that owns `Room.databaseBuilder(...)` was named in Phase 1.** The Data Engineer's section above flags this as Android Engineer territory. The DBA did not grep for it. The Android Engineer must locate the builder before patch 2 and confirm that adding `.addMigrations(MIGRATION_1_2)` does not require any other wiring change (for example, if the database is currently constructed without `databaseBuilder` at all, this changes the patch shape).
2. **Whether `room-testing:2.6.1` in this build exposes the two argument `MigrationTestHelper` constructor or only the three argument form.** The DBA flagged this in Item 2 above as a verification step the Android Engineer performs at write time. If the two argument form is unavailable, the test sketch needs the additional `FrameworkSQLiteOpenHelperFactory()` parameter.
3. **Whether the `app/schemas/` directory should be excluded from CI test artifact uploads.** Forward looking only; not a Phase 2 blocker. The schemas directory is small and reviewable; the DBA recommends keeping it tracked and uploaded with no exclusion.

### Phase 2 readiness

The Phase 2 plan can proceed to code work. All three Data Engineer decisions are ratified (Decision 1 PASS, Decision 2 PASS, Recommendation 3 PASS). Items 4 and 5 are decided. Item 2 (the migration test sketch) requires modification, but the modification is named precisely enough for the Android Engineer to apply directly without dispatching back to the DBA. No SPEC.md amendment is required.

## Future sections

- **Feature JSON contract.** Per test, the canonical feature key names, types, and units. Reporting and PDF export consume this contract. Example for gait: `{"cadence_spm": 112.3, "stride_length_m": 1.42, "step_time_cv": 0.045}`. The Data Engineer fills this in as each test module lands (Phases 2 through 8).
- **Migrations.** Every schema version bump is documented here with the `Migration` object, the rationale, and a Robolectric test that exercises the migration. The first migration (`MIGRATION_1_2`) is sketched in the Phase 2 schema decisions section above; the DBA's audit verdict on it is in the Phase 2 schema audit section.
