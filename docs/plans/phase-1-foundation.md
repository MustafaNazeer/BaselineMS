# BaselineMS Phase 1: Foundation Implementation Plan (Android)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the foundation of the BaselineMS Android app: Android Studio project, Room data layer, the `TestModule` interface with a mock implementation, the `BatteryOrchestrator` `ViewModel`, and the Compose UI shell (onboarding, profile creation, home screen, settings). End state is a runnable app where the weekly battery flow runs end to end against a mock test, results persist in Room, and history shows completed sessions.

**Architecture:** Native Android, Kotlin and Jetpack Compose, layered. The `TestModule` interface is the seam between the orchestrator and any concrete test, so adding real tests (tap, gait, vision, SDMT, voice) in later phases requires no changes to the orchestrator. Room provides on device persistence with no cloud sync. The app does not declare the `INTERNET` permission.

**Tech Stack:** Kotlin 1.9, Jetpack Compose (Material 3), Navigation Compose, Room with KSP, Kotlin Coroutines and Flow, JUnit 4, Robolectric, kotlinx-coroutines-test, Android Studio Iguana or later, min SDK 31, target SDK 34.

**Related spec:** `~/src/BaselineMS/SPEC.md`

**Platform note:** Every command in this plan runs on Linux. No macOS required.

---

## File Map

Files this plan creates or modifies:

```
~/src/BaselineMS/                                  (Android Studio project root)
├── build.gradle.kts                              (project)
├── settings.gradle.kts
├── gradle/wrapper/...
├── gradlew
├── app/
│   ├── build.gradle.kts                          (app module)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mustafan4x/baselinems/
│       │   │   ├── BaselineMSApp.kt               (Application + manual DI)
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── Enums.kt                  (TestType, Sex, Hand, MSType)
│       │   │   │   ├── Converters.kt             (Room TypeConverters for enums)
│       │   │   │   ├── UserProfileEntity.kt
│       │   │   │   ├── SessionEntity.kt
│       │   │   │   ├── TestResultEntity.kt
│       │   │   │   ├── UserProfileDao.kt
│       │   │   │   ├── SessionDao.kt
│       │   │   │   ├── TestResultDao.kt
│       │   │   │   └── AppDatabase.kt
│       │   │   ├── battery/
│       │   │   │   ├── TestModule.kt             (interface + TestResultPayload)
│       │   │   │   ├── MockTestModule.kt
│       │   │   │   └── BatteryOrchestrator.kt    (ViewModel)
│       │   │   ├── ui/
│       │   │   │   ├── RootScreen.kt             (routes onboarding vs home)
│       │   │   │   ├── theme/
│       │   │   │   │   └── Theme.kt              (default Material 3 theme)
│       │   │   │   ├── onboarding/
│       │   │   │   │   ├── DisclaimerScreen.kt
│       │   │   │   │   └── ProfileSetupScreen.kt
│       │   │   │   ├── home/
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   └── SessionRunnerScreen.kt
│       │   │   │   └── settings/
│       │   │   │       └── SettingsScreen.kt
│       │   │   └── util/
│       │   │       └── DeviceInfo.kt
│       │   └── res/                              (default + strings)
│       └── test/java/com/mustafan4x/baselinems/
│           ├── data/
│           │   ├── ConvertersTest.kt
│           │   ├── UserProfileDaoTest.kt
│           │   ├── SessionDaoTest.kt
│           │   └── TestResultDaoTest.kt
│           ├── battery/
│           │   ├── MockTestModuleTest.kt
│           │   ├── BatteryOrchestratorTest.kt
│           │   └── BatteryFlowIntegrationTest.kt
│           └── util/
│               └── MainDispatcherRule.kt
└── README.md
```

After this plan ships, every concrete test in later phases adds a new file under `battery/` and a corresponding screen under `ui/`; nothing in `data/`, `BatteryOrchestrator.kt`, or the UI shell needs to change.

---

## Task 1: Create the Android Studio project

**Files:**
- Create: `~/src/BaselineMS/` (Android Studio project tree)

This task is partially manual because Android Studio's project wizard is GUI driven. Capture the final state in git so subsequent tasks have a clean starting point.

- [ ] **Step 1: Create the Android Studio project**

In Android Studio Iguana or later: File > New > New Project > Empty Activity (the Compose template).

Set:
- Name: `BaselineMS`
- Package name: `com.mustafan4x.baselinems`
- Save location: `~/src/BaselineMS`
- Language: Kotlin
- Minimum SDK: API 31 ("Android 12.0 (S)")
- Build configuration language: Kotlin DSL (`build.gradle.kts`)

Click Finish and let Gradle sync.

- [ ] **Step 2: Update `app/build.gradle.kts` with the dependencies the rest of the plan needs**

File: `~/src/BaselineMS/app/build.gradle.kts`

Replace the file with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.mustafan4x.baselinems"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mustafan4x.baselinems"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    testOptions {
        unitTests.isIncludeAndroidResources = true   // for Robolectric
    }

    packaging {
        resources {
            excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("io.mockk:mockk:1.13.11")
}
```

Confirm `gradle/libs.versions.toml` already declares the `android.application` and `jetbrains.kotlin.android` plugin aliases (the Empty Activity template ships with these in Android Studio Iguana). If your version of Android Studio scaffolded a `compose.compiler` plugin alias instead of `composeOptions`, you can either keep it (and remove the `composeOptions` block from `app/build.gradle.kts`) or remove the alias and keep the `composeOptions` block this plan uses. Either path works; the plan chooses the `composeOptions` route to stay compatible with Kotlin 1.9.24.

Add the `kotlinx.serialization` Gradle plugin if you plan to use `kotlinx-serialization-json` (we will). In `~/src/BaselineMS/build.gradle.kts` (the project level file), add to the `plugins` block:

```kotlin
id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
```

And in `~/src/BaselineMS/app/build.gradle.kts`, add to the module level `plugins` block:

```kotlin
id("org.jetbrains.kotlin.plugin.serialization")
```

- [ ] **Step 3: Sync Gradle and confirm the project builds**

In the terminal:

```bash
cd ~/src/BaselineMS
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

If `./gradlew` is not yet present (Android Studio sometimes generates it on first sync), open the project in Android Studio first, let it sync, and then retry.

- [ ] **Step 4: Initialise git in the project root and commit the initial scaffold**

```bash
cd ~/src/BaselineMS
git init
echo ".gradle/" > .gitignore
echo "build/" >> .gitignore
echo "/local.properties" >> .gitignore
echo "/.idea/" >> .gitignore
echo "*.apk" >> .gitignore
echo "*.ap_" >> .gitignore
echo "*.aab" >> .gitignore
echo "/captures" >> .gitignore
echo "*.iml" >> .gitignore
git add .
git commit -m "chore: initial Android scaffold for BaselineMS"
```

- [ ] **Step 5: Replace the auto generated `MainActivity` and theme files with a minimal placeholder so we own the code from here**

File: `~/src/BaselineMS/app/src/main/java/com/mustafan4x/baselinems/MainActivity.kt`

```kotlin
package com.mustafan4x.baselinems

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { Text("BaselineMS placeholder") }
            }
        }
    }
}
```

Delete any other auto generated files in `com.mustafan4x.baselinems` (such as `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`) that the template scaffolded; we will recreate the theme properly in Task 10 if needed.

Confirm `AndroidManifest.xml` does not include `<uses-permission android:name="android.permission.INTERNET" />`. If it does, remove it.

- [ ] **Step 6: Build and confirm**

```bash
cd ~/src/BaselineMS
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "chore: trim Android template down to placeholder MainActivity"
```

---

## Task 2: Add `Enums.kt`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/Enums.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/data/EnumsTest.kt`

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/mustafan4x/baselinems/data/EnumsTest.kt`

```kotlin
package com.mustafan4x.baselinems.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EnumsTest {

    @Test
    fun testTypeHasFiveCases() {
        assertEquals(5, TestType.values().size)
    }

    @Test
    fun testTypeNames() {
        assertEquals("TAP", TestType.TAP.name)
        assertEquals("GAIT", TestType.GAIT.name)
        assertEquals("VISION", TestType.VISION.name)
        assertEquals("SDMT", TestType.SDMT.name)
        assertEquals("VOICE", TestType.VOICE.name)
    }

    @Test
    fun sexHasFourCases() { assertEquals(4, Sex.values().size) }

    @Test
    fun handHasThreeCases() { assertEquals(3, Hand.values().size) }

    @Test
    fun msTypeHasFiveCases() { assertEquals(5, MSType.values().size) }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.data.EnumsTest"
```

Expected: FAILURE with "unresolved reference: TestType" and similar.

- [ ] **Step 3: Implement `Enums.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/Enums.kt`

```kotlin
package com.mustafan4x.baselinems.data

enum class TestType { TAP, GAIT, VISION, SDMT, VOICE }
enum class Sex { FEMALE, MALE, OTHER, UNDISCLOSED }
enum class Hand { LEFT, RIGHT, AMBIDEXTROUS }
enum class MSType { RRMS, PPMS, SPMS, CIS, UNDISCLOSED }
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.data.EnumsTest"
```

Expected: BUILD SUCCESSFUL, all five tests pass.

- [ ] **Step 5: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(data): add TestType, Sex, Hand, MSType enums"
```

---

## Task 3: Add Room `Converters` and `UserProfileEntity` plus `UserProfileDao`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/Converters.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/UserProfileEntity.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/UserProfileDao.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt` (skeleton only, expanded in Task 4)
- Test: `app/src/test/java/com/mustafan4x/baselinems/data/ConvertersTest.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/data/UserProfileDaoTest.kt`

The Room `Converters` are needed because we are storing enum types and `UUID` strings.

- [ ] **Step 1: Write failing tests**

File: `app/src/test/java/com/mustafan4x/baselinems/data/ConvertersTest.kt`

```kotlin
package com.mustafan4x.baselinems.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testTypeRoundTrip() {
        assertEquals(TestType.GAIT, converters.toTestType(converters.fromTestType(TestType.GAIT)))
    }

    @Test
    fun sexRoundTrip() {
        assertEquals(Sex.FEMALE, converters.toSex(converters.fromSex(Sex.FEMALE)))
    }

    @Test
    fun handRoundTrip() {
        assertEquals(Hand.LEFT, converters.toHand(converters.fromHand(Hand.LEFT)))
    }

    @Test
    fun msTypeRoundTrip() {
        assertEquals(MSType.RRMS, converters.toMSType(converters.fromMSType(MSType.RRMS)))
    }

    @Test
    fun nullEnumRoundTrip() {
        assertNull(converters.toTestType(null))
    }
}
```

File: `app/src/test/java/com/mustafan4x/baselinems/data/UserProfileDaoTest.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class UserProfileDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserProfileDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.userProfileDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndFetchSingleProfile() = runTest {
        val profile = UserProfileEntity(
            dateOfBirthEpochMs = 0L,
            biologicalSex = Sex.FEMALE,
            dominantHand = Hand.RIGHT,
            heightCm = 170.0
        )
        dao.insert(profile)

        val fetched = dao.getFirst()
        assertNotNull(fetched)
        assertEquals(170.0, fetched!!.heightCm, 0.0001)
        assertEquals(Sex.FEMALE, fetched.biologicalSex)
        assertEquals(MSType.UNDISCLOSED, fetched.msTypeDisclosed)
    }

    @Test
    fun emptyTableReturnsNull() = runTest {
        assertNull(dao.getFirst())
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.data.ConvertersTest" --tests "com.mustafan4x.baselinems.data.UserProfileDaoTest"
```

Expected: BUILD FAILED with unresolved references.

- [ ] **Step 3: Implement `Converters.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/Converters.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromTestType(value: TestType?): String? = value?.name
    @TypeConverter fun toTestType(value: String?): TestType? = value?.let { TestType.valueOf(it) }

    @TypeConverter fun fromSex(value: Sex?): String? = value?.name
    @TypeConverter fun toSex(value: String?): Sex? = value?.let { Sex.valueOf(it) }

    @TypeConverter fun fromHand(value: Hand?): String? = value?.name
    @TypeConverter fun toHand(value: String?): Hand? = value?.let { Hand.valueOf(it) }

    @TypeConverter fun fromMSType(value: MSType?): String? = value?.name
    @TypeConverter fun toMSType(value: String?): MSType? = value?.let { MSType.valueOf(it) }
}
```

- [ ] **Step 4: Implement `UserProfileEntity.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/UserProfileEntity.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateOfBirthEpochMs: Long,
    val biologicalSex: Sex,
    val dominantHand: Hand,
    val msTypeDisclosed: MSType = MSType.UNDISCLOSED,
    val heightCm: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: Implement `UserProfileDao.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/UserProfileDao.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile ORDER BY createdAtEpochMs ASC LIMIT 1")
    suspend fun getFirst(): UserProfileEntity?
}
```

- [ ] **Step 6: Implement `AppDatabase.kt` (skeleton with one entity for now)**

File: `app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
```

- [ ] **Step 7: Run the tests to verify they pass**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.data.ConvertersTest" --tests "com.mustafan4x.baselinems.data.UserProfileDaoTest"
```

Expected: BUILD SUCCESSFUL, all seven tests pass.

- [ ] **Step 8: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(data): add Room Converters, UserProfileEntity, DAO, and AppDatabase skeleton"
```

---

## Task 4: Add `SessionEntity`, `TestResultEntity`, their DAOs, and expand `AppDatabase`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/SessionEntity.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/TestResultEntity.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/SessionDao.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/data/TestResultDao.kt`
- Modify: `app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/data/SessionDaoTest.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/data/TestResultDaoTest.kt`

- [ ] **Step 1: Write failing tests**

File: `app/src/test/java/com/mustafan4x/baselinems/data/SessionDaoTest.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SessionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var resultDao: TestResultDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        resultDao = db.testResultDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndFetchSession() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel Test, Android 14")
        sessionDao.insert(session)

        val all = sessionDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Pixel Test, Android 14", all.first().deviceInfo)
        assertNull(all.first().completedAtEpochMs)
    }

    @Test
    fun deletingSessionCascadesToResults() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel")
        sessionDao.insert(session)
        val result = TestResultEntity(
            sessionId = session.id,
            testType = TestType.TAP,
            startedAtEpochMs = 0L,
            completedAtEpochMs = 100L,
            qualityScore = 1.0,
            featuresJson = "{}"
        )
        resultDao.insert(result)
        assertEquals(1, resultDao.getForSession(session.id).size)

        sessionDao.delete(session)

        assertNull(sessionDao.getById(session.id))
        assertEquals(0, resultDao.getForSession(session.id).size)
    }

    @Test
    fun getByIdReturnsCorrectSession() = runTest {
        val a = SessionEntity(deviceInfo = "A")
        val b = SessionEntity(deviceInfo = "B")
        sessionDao.insert(a); sessionDao.insert(b)

        val fetched = sessionDao.getById(b.id)
        assertNotNull(fetched)
        assertEquals("B", fetched!!.deviceInfo)
    }
}
```

File: `app/src/test/java/com/mustafan4x/baselinems/data/TestResultDaoTest.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TestResultDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var resultDao: TestResultDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        resultDao = db.testResultDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun resultsForSessionReturnedInChronologicalOrder() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel")
        sessionDao.insert(session)

        val first = TestResultEntity(
            sessionId = session.id, testType = TestType.TAP,
            startedAtEpochMs = 100, completedAtEpochMs = 110,
            qualityScore = 0.9, featuresJson = "{}"
        )
        val second = TestResultEntity(
            sessionId = session.id, testType = TestType.GAIT,
            startedAtEpochMs = 200, completedAtEpochMs = 230,
            qualityScore = 0.8, featuresJson = "{}"
        )
        resultDao.insert(second)  // intentionally inserted out of order
        resultDao.insert(first)

        val fetched = resultDao.getForSession(session.id)
        assertEquals(2, fetched.size)
        assertEquals(TestType.TAP, fetched[0].testType)
        assertEquals(TestType.GAIT, fetched[1].testType)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.data.SessionDaoTest" --tests "com.mustafan4x.baselinems.data.TestResultDaoTest"
```

Expected: BUILD FAILED with unresolved references.

- [ ] **Step 3: Implement `SessionEntity.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/SessionEntity.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val deviceInfo: String
)
```

- [ ] **Step 4: Implement `TestResultEntity.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/TestResultEntity.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "test_result",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class TestResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "session_id") val sessionId: String,
    val testType: TestType,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val qualityScore: Double,
    val featuresJson: String,
    val rawSensorRelativePath: String? = null
)
```

- [ ] **Step 5: Implement `SessionDao.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/SessionDao.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM session ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?
}
```

- [ ] **Step 6: Implement `TestResultDao.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/data/TestResultDao.kt`

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestResultDao {

    @Insert
    suspend fun insert(result: TestResultEntity)

    @Query("SELECT * FROM test_result WHERE session_id = :sessionId ORDER BY startedAtEpochMs ASC")
    suspend fun getForSession(sessionId: String): List<TestResultEntity>
}
```

- [ ] **Step 7: Update `AppDatabase.kt` to register the new entities and DAOs**

File: `app/src/main/java/com/mustafan4x/baselinems/data/AppDatabase.kt`

Replace the file with:

```kotlin
package com.mustafan4x.baselinems.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        SessionEntity::class,
        TestResultEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun testResultDao(): TestResultDao
}
```

- [ ] **Step 8: Run the tests to verify they pass**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, every test added so far passes.

- [ ] **Step 9: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(data): add Session and TestResult entities with cascade delete"
```

---

## Task 5: Define `TestModule` interface and `TestResultPayload`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/TestModule.kt`

There is no test for this task on its own; the interface is exercised by `MockTestModule` and `BatteryOrchestrator` tests in the next two tasks.

- [ ] **Step 1: Implement `TestModule.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/battery/TestModule.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import androidx.compose.runtime.Composable
import com.mustafan4x.baselinems.data.TestType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface TestResultPayload {
    val qualityScore: Double         // 0..1
    val features: Map<String, Double>
}

interface TestModule {
    val testType: TestType
    val displayName: String
    val instructions: String
    val estimatedDurationSeconds: Int

    @Composable
    fun Content(onComplete: (TestResultPayload) -> Unit)
}

fun TestResultPayload.featuresAsJson(): String {
    return Json.encodeToString(features)
}
```

- [ ] **Step 2: Build to confirm it compiles**

```bash
cd ~/src/BaselineMS
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(battery): define TestModule interface and TestResultPayload"
```

---

## Task 6: Add `MockTestModule`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/MockTestModule.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/battery/MockTestModuleTest.kt`

The mock module is what lets us run the full battery flow end to end before any concrete sensor based test exists.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/mustafan4x/baselinems/battery/MockTestModuleTest.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import com.mustafan4x.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestModuleTest {

    @Test
    fun mockResultEncodesToJson() {
        val payload = MockTestModule.MockResult(qualityScore = 0.75, features = mapOf("fake_metric" to 1.5))
        assertEquals(0.75, payload.qualityScore, 0.0001)
        val json = payload.featuresAsJson()
        assertTrue(json.contains("fake_metric"))
        assertTrue(json.contains("1.5"))
    }

    @Test
    fun moduleMetadata() {
        val module = MockTestModule()
        assertEquals(TestType.TAP, module.testType)
        assertEquals(1, module.estimatedDurationSeconds)
        assertTrue(module.displayName.isNotEmpty())
        assertTrue(module.instructions.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.MockTestModuleTest"
```

Expected: FAILURE with unresolved reference `MockTestModule`.

- [ ] **Step 3: Implement `MockTestModule.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/battery/MockTestModule.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.data.TestType

class MockTestModule : TestModule {

    data class MockResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>
    ) : TestResultPayload

    override val testType: TestType = TestType.TAP
    override val displayName: String = "Mock Test"
    override val instructions: String = "This is a mock test used during scaffolding. Tap Continue."
    override val estimatedDurationSeconds: Int = 1

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(displayName)
            Text(instructions, textAlign = TextAlign.Center)
            Button(onClick = {
                onComplete(MockResult(qualityScore = 1.0, features = mapOf("mock_value" to 42.0)))
            }) {
                Text("Continue")
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.MockTestModuleTest"
```

Expected: BUILD SUCCESSFUL, both tests pass.

- [ ] **Step 5: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(battery): add MockTestModule for scaffolding"
```

---

## Task 7: Implement `BatteryOrchestrator`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/BatteryOrchestrator.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/util/DeviceInfo.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/util/MainDispatcherRule.kt`
- Test: `app/src/test/java/com/mustafan4x/baselinems/battery/BatteryOrchestratorTest.kt`

The orchestrator is a `ViewModel` that walks through a list of `TestModule` instances, persists each completed `TestResultEntity`, and exposes a `StateFlow` to the UI.

- [ ] **Step 1: Add the test dispatcher rule**

File: `app/src/test/java/com/mustafan4x/baselinems/util/MainDispatcherRule.kt`

```kotlin
package com.mustafan4x.baselinems.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(testDispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```

- [ ] **Step 2: Write the failing orchestrator test**

File: `app/src/test/java/com/mustafan4x/baselinems/battery/BatteryOrchestratorTest.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import com.mustafan4x.baselinems.data.SessionDao
import com.mustafan4x.baselinems.data.SessionEntity
import com.mustafan4x.baselinems.data.TestResultDao
import com.mustafan4x.baselinems.data.TestResultEntity
import com.mustafan4x.baselinems.data.TestType
import com.mustafan4x.baselinems.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeSessionDao : SessionDao {
    val sessions = mutableListOf<SessionEntity>()
    override suspend fun insert(session: SessionEntity) { sessions.add(session) }
    override suspend fun update(session: SessionEntity) {
        val i = sessions.indexOfFirst { it.id == session.id }
        if (i >= 0) sessions[i] = session
    }
    override suspend fun delete(session: SessionEntity) { sessions.removeAll { it.id == session.id } }
    override fun observeAll(): Flow<List<SessionEntity>> = flowOf(sessions.toList())
    override suspend fun getById(id: String): SessionEntity? = sessions.find { it.id == id }
}

private class FakeTestResultDao : TestResultDao {
    val results = mutableListOf<TestResultEntity>()
    override suspend fun insert(result: TestResultEntity) { results.add(result) }
    override suspend fun getForSession(sessionId: String): List<TestResultEntity> =
        results.filter { it.sessionId == sessionId }.sortedBy { it.startedAtEpochMs }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryOrchestratorTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsIdle() {
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = FakeSessionDao(),
            testResultDao = FakeTestResultDao(),
            deviceInfo = "test"
        )
        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
    }

    @Test
    fun startTransitionsToRunningAndCreatesSession() = runTest {
        val sessionDao = FakeSessionDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = FakeTestResultDao(),
            deviceInfo = "Pixel"
        )
        orchestrator.start()
        advanceUntilIdle()

        val s = orchestrator.state.value
        assertTrue("Expected Running, got $s", s is BatteryOrchestrator.State.Running)
        assertEquals(0, (s as BatteryOrchestrator.State.Running).index)

        assertEquals(1, sessionDao.sessions.size)
        assertEquals("Pixel", sessionDao.sessions.first().deviceInfo)
        assertNull(sessionDao.sessions.first().completedAtEpochMs)
    }

    @Test
    fun completingAllModulesPersistsResultsAndCompletesSession() = runTest {
        val sessionDao = FakeSessionDao()
        val resultDao = FakeTestResultDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule(), MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = resultDao,
            deviceInfo = "Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        orchestrator.recordResult(testType = TestType.TAP, qualityScore = 0.9, features = mapOf("a" to 1.0))
        advanceUntilIdle()
        orchestrator.recordResult(testType = TestType.GAIT, qualityScore = 0.8, features = mapOf("b" to 2.0))
        advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Completed, orchestrator.state.value)
        assertEquals(1, sessionDao.sessions.size)
        assertNotNull(sessionDao.sessions.first().completedAtEpochMs)
        assertEquals(2, resultDao.results.size)
        val types = resultDao.results.map { it.testType }.sortedBy { it.name }
        assertEquals(listOf(TestType.GAIT, TestType.TAP), types)
    }

    @Test
    fun cancelDiscardsActiveSession() = runTest {
        val sessionDao = FakeSessionDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = FakeTestResultDao(),
            deviceInfo = "Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        orchestrator.cancel(); advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        assertEquals(0, sessionDao.sessions.size)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.BatteryOrchestratorTest"
```

Expected: FAILURE with unresolved references for `BatteryOrchestrator`.

- [ ] **Step 4: Implement `DeviceInfo.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/util/DeviceInfo.kt`

```kotlin
package com.mustafan4x.baselinems.util

import android.os.Build

object DeviceInfo {
    val summary: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
}
```

- [ ] **Step 5: Implement `BatteryOrchestrator.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/battery/BatteryOrchestrator.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafan4x.baselinems.data.SessionDao
import com.mustafan4x.baselinems.data.SessionEntity
import com.mustafan4x.baselinems.data.TestResultDao
import com.mustafan4x.baselinems.data.TestResultEntity
import com.mustafan4x.baselinems.data.TestType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BatteryOrchestrator(
    val modules: List<TestModule>,
    private val sessionDao: SessionDao,
    private val testResultDao: TestResultDao,
    private val deviceInfo: String
) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data class Running(val index: Int) : State()
        data object Completed : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var activeSessionId: String? = null

    fun start() {
        if (modules.isEmpty()) return
        viewModelScope.launch {
            val session = SessionEntity(deviceInfo = deviceInfo)
            sessionDao.insert(session)
            activeSessionId = session.id
            _state.value = State.Running(index = 0)
        }
    }

    fun recordResult(testType: TestType, qualityScore: Double, features: Map<String, Double>) {
        val current = _state.value
        if (current !is State.Running) return
        val sessionId = activeSessionId ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val featuresJson = Json.encodeToString(features)
            testResultDao.insert(
                TestResultEntity(
                    sessionId = sessionId,
                    testType = testType,
                    startedAtEpochMs = now,
                    completedAtEpochMs = now,
                    qualityScore = qualityScore,
                    featuresJson = featuresJson
                )
            )

            val nextIndex = current.index + 1
            if (nextIndex >= modules.size) {
                val session = sessionDao.getById(sessionId)
                if (session != null) {
                    sessionDao.update(session.copy(completedAtEpochMs = now))
                }
                _state.value = State.Completed
            } else {
                _state.value = State.Running(index = nextIndex)
            }
        }
    }

    fun cancel() {
        val sessionId = activeSessionId
        viewModelScope.launch {
            if (sessionId != null) {
                val session = sessionDao.getById(sessionId)
                if (session != null) sessionDao.delete(session)
            }
            activeSessionId = null
            _state.value = State.Idle
        }
    }

    fun reset() {
        activeSessionId = null
        _state.value = State.Idle
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.BatteryOrchestratorTest"
```

Expected: BUILD SUCCESSFUL, all four tests pass.

- [ ] **Step 7: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(battery): implement BatteryOrchestrator state machine"
```

---

## Task 8: Implement onboarding screens

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/onboarding/DisclaimerScreen.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/onboarding/ProfileSetupScreen.kt`

These are Composable screens. Behavior is verified by manual emulator run plus the integration test in Task 11. We confirm the project builds after writing them.

- [ ] **Step 1: Implement `DisclaimerScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/onboarding/DisclaimerScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DisclaimerScreen(onAcknowledge: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(24.dp)),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BaselineMS",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "This app is not a medical device. It does not diagnose or treat any condition. " +
                "Do not change your treatment based on these results. " +
                "Share with your neurologist for clinical decisions.",
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I understand")
        }
    }
}
```

- [ ] **Step 2: Implement `ProfileSetupScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/onboarding/ProfileSetupScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.data.Hand
import com.mustafan4x.baselinems.data.MSType
import com.mustafan4x.baselinems.data.Sex
import com.mustafan4x.baselinems.data.UserProfileEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.mustafan4x.baselinems.data.UserProfileDao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userProfileDao: UserProfileDao,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sex by remember { mutableStateOf(Sex.UNDISCLOSED) }
    var hand by remember { mutableStateOf(Hand.RIGHT) }
    var msType by remember { mutableStateOf(MSType.UNDISCLOSED) }
    var heightCmText by remember { mutableStateOf("170") }
    var dobYearText by remember { mutableStateOf("1995") }

    Scaffold(topBar = { TopAppBar(title = { Text("Set up profile") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dobYearText,
                onValueChange = { dobYearText = it },
                label = { Text("Year of birth") }
            )
            OutlinedTextField(
                value = heightCmText,
                onValueChange = { heightCmText = it },
                label = { Text("Height (cm)") }
            )
            EnumDropdown("Biological sex", Sex.values().toList(), sex) { sex = it }
            EnumDropdown("Dominant hand", Hand.values().toList(), hand) { hand = it }
            EnumDropdown("MS type (optional)", MSType.values().toList(), msType) { msType = it }
            Button(onClick = {
                scope.launch {
                    val year = dobYearText.toIntOrNull() ?: 1995
                    val cal = java.util.Calendar.getInstance().apply { set(year, 0, 1) }
                    userProfileDao.insert(
                        UserProfileEntity(
                            dateOfBirthEpochMs = cal.timeInMillis,
                            biologicalSex = sex,
                            dominantHand = hand,
                            msTypeDisclosed = msType,
                            heightCm = heightCmText.toDoubleOrNull() ?: 170.0
                        )
                    )
                    onComplete()
                }
            }) {
                Text("Save and continue")
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("$label: ${selected.name}")
        Button(onClick = { expanded = true }) { Text("Change") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.name) }, onClick = {
                    onSelected(option); expanded = false
                })
            }
        }
    }
}
```

(The dropdown UX is intentionally minimal; later phases polish it.)

- [ ] **Step 3: Build to verify it compiles**

```bash
cd ~/src/BaselineMS
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(ui): add disclaimer and profile setup screens"
```

---

## Task 9: Implement home, session runner, and settings screens

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/home/SessionRunnerScreen.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Implement `SessionRunnerScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/home/SessionRunnerScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.battery.BatteryOrchestrator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRunnerScreen(
    orchestrator: BatteryOrchestrator,
    onFinished: () -> Unit
) {
    val state by orchestrator.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Battery") },
                actions = {
                    if (state !is BatteryOrchestrator.State.Completed) {
                        Button(onClick = { orchestrator.cancel(); onFinished() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                BatteryOrchestrator.State.Idle -> Text("Session not started")
                is BatteryOrchestrator.State.Running -> {
                    val module = orchestrator.modules[s.index]
                    module.Content { result ->
                        orchestrator.recordResult(
                            testType = module.testType,
                            qualityScore = result.qualityScore,
                            features = result.features
                        )
                    }
                }
                BatteryOrchestrator.State.Completed -> {
                    Text("Session complete")
                    Button(onClick = onFinished) { Text("Done") }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Implement `HomeScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/home/HomeScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.data.SessionDao
import com.mustafan4x.baselinems.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionDao: SessionDao,
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sessions by sessionDao.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BaselineMS") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onStartSession, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("  Start weekly battery", style = MaterialTheme.typography.titleMedium)
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (sessions.isEmpty()) {
                Text("No sessions yet. Run the weekly battery to get started.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions) { session ->
                        SessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    Column {
        Text(df.format(Date(session.startedAtEpochMs)), style = MaterialTheme.typography.bodyLarge)
        Text(
            if (session.completedAtEpochMs != null) "Completed" else "In progress",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

- [ ] **Step 3: Implement `SettingsScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/settings/SettingsScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.data.UserProfileDao
import com.mustafan4x.baselinems.data.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(userProfileDao: UserProfileDao) {
    var profile by remember { mutableStateOf<UserProfileEntity?>(null) }
    LaunchedEffect(Unit) { profile = userProfileDao.getFirst() }
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            val p = profile
            if (p == null) {
                Text("No profile yet.")
            } else {
                Text("Date of birth: ${df.format(Date(p.dateOfBirthEpochMs))}")
                Text("Sex: ${p.biologicalSex.name}")
                Text("Dominant hand: ${p.dominantHand.name}")
                Text("Height: ${p.heightCm.toInt()} cm")
                Text("MS type: ${p.msTypeDisclosed.name}")
            }
            Text(" ")
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "BaselineMS is not a medical device. It does not diagnose or treat any condition.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

- [ ] **Step 4: Build to verify it compiles**

```bash
cd ~/src/BaselineMS
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(ui): add home, session runner, and settings screens"
```

---

## Task 10: Wire `RootScreen`, `BaselineMSApp` (Application), and `MainActivity`

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/ui/RootScreen.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/BaselineMSApp.kt`
- Modify: `app/src/main/java/com/mustafan4x/baselinems/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

`RootScreen` decides between disclaimer, profile setup, and home based on `SharedPreferences` and the Room store. The Application class owns the `AppDatabase` instance for the entire app lifecycle.

- [ ] **Step 1: Implement `BaselineMSApp.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/BaselineMSApp.kt`

```kotlin
package com.mustafan4x.baselinems

import android.app.Application
import androidx.room.Room
import com.mustafan4x.baselinems.data.AppDatabase

class BaselineMSApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "baselinems.db"
        ).build()
    }
}
```

- [ ] **Step 2: Implement `RootScreen.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/ui/RootScreen.kt`

```kotlin
package com.mustafan4x.baselinems.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mustafan4x.baselinems.BaselineMSApp
import com.mustafan4x.baselinems.battery.BatteryOrchestrator
import com.mustafan4x.baselinems.battery.MockTestModule
import com.mustafan4x.baselinems.ui.home.HomeScreen
import com.mustafan4x.baselinems.ui.home.SessionRunnerScreen
import com.mustafan4x.baselinems.ui.onboarding.DisclaimerScreen
import com.mustafan4x.baselinems.ui.onboarding.ProfileSetupScreen
import com.mustafan4x.baselinems.ui.settings.SettingsScreen
import com.mustafan4x.baselinems.util.DeviceInfo

private const val PREFS = "baselinems_prefs"
private const val KEY_DISCLAIMER = "disclaimer_acknowledged"

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BaselineMSApp
    val nav = rememberNavController()
    var disclaimerAcknowledged by remember {
        mutableStateOf(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DISCLAIMER, false))
    }
    var hasProfile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasProfile = app.database.userProfileDao().getFirst() != null
    }

    val startDestination = when {
        !disclaimerAcknowledged -> "disclaimer"
        !hasProfile -> "profile"
        else -> "home"
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("disclaimer") {
            DisclaimerScreen(onAcknowledge = {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_DISCLAIMER, true).apply()
                disclaimerAcknowledged = true
                nav.navigate(if (hasProfile) "home" else "profile") {
                    popUpTo("disclaimer") { inclusive = true }
                }
            })
        }
        composable("profile") {
            ProfileSetupScreen(
                userProfileDao = app.database.userProfileDao(),
                onComplete = {
                    hasProfile = true
                    nav.navigate("home") { popUpTo("profile") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen(
                sessionDao = app.database.sessionDao(),
                onStartSession = { nav.navigate("session") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("session") {
            val orchestrator = remember {
                BatteryOrchestrator(
                    modules = listOf(MockTestModule()),
                    sessionDao = app.database.sessionDao(),
                    testResultDao = app.database.testResultDao(),
                    deviceInfo = DeviceInfo.summary
                ).also { it.start() }
            }
            SessionRunnerScreen(orchestrator = orchestrator, onFinished = {
                nav.popBackStack("home", inclusive = false)
            })
        }
        composable("settings") {
            SettingsScreen(userProfileDao = app.database.userProfileDao())
        }
    }
}
```

(The orchestrator is constructed fresh per session route. This is fine for Phase 1 because there is only one mock module; in Phase 2 we will lift orchestrator construction into a `ViewModel` factory keyed to the session.)

- [ ] **Step 3: Update `MainActivity.kt`**

File: `app/src/main/java/com/mustafan4x/baselinems/MainActivity.kt`

Replace with:

```kotlin
package com.mustafan4x.baselinems

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.mustafan4x.baselinems.ui.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { RootScreen() }
            }
        }
    }
}
```

- [ ] **Step 4: Update `AndroidManifest.xml` to register the Application class**

File: `app/src/main/AndroidManifest.xml`

Update the `<application>` tag to include `android:name=".BaselineMSApp"`. The full element should look like:

```xml
<application
    android:name=".BaselineMSApp"
    android:allowBackup="false"
    android:icon="@mipmap/ic_launcher"
    android:label="BaselineMS"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.BaselineMS">
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

Confirm there is **no** `<uses-permission android:name="android.permission.INTERNET" />` element.

- [ ] **Step 5: Build and run on the emulator**

Create or use an Android Virtual Device (AVD) running API 33 or 34. In Android Studio, use the Device Manager to create a Pixel 7 with API 33 if you do not have one.

```bash
cd ~/src/BaselineMS
./gradlew :app:installDebug
adb shell am start -n com.mustafan4x.baselinems/.MainActivity
```

Walk the flow on the emulator:

1. Disclaimer screen appears. Tap "I understand".
2. Profile setup form appears. Fill out, tap "Save and continue".
3. Home screen appears with empty history.
4. Tap "Start weekly battery". Session runner appears with the mock test view.
5. Tap "Continue" in the mock test. Session marked complete. Tap "Done".
6. Home screen now shows one session in the history list.
7. Open Settings via the gear icon and verify the profile values are displayed.

- [ ] **Step 6: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "feat(app): wire RootScreen, BaselineMSApp Application class, and MainActivity; complete Phase 1 happy path"
```

---

## Task 11: Add an integration test for the full battery flow

**Files:**
- Test: `app/src/test/java/com/mustafan4x/baselinems/battery/BatteryFlowIntegrationTest.kt`

This test exercises orchestrator + Room persistence with multiple modules and verifies the complete data shape that the UI depends on. It runs on the JVM via Robolectric so it stays in the fast unit test layer.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/mustafan4x/baselinems/battery/BatteryFlowIntegrationTest.kt`

```kotlin
package com.mustafan4x.baselinems.battery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafan4x.baselinems.data.AppDatabase
import com.mustafan4x.baselinems.data.TestType
import com.mustafan4x.baselinems.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BatteryFlowIntegrationTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun endToEndBatteryRunPersistsCompleteSession() = runTest {
        val modules = listOf(MockTestModule(), MockTestModule(), MockTestModule())
        val orchestrator = BatteryOrchestrator(
            modules = modules,
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao(),
            deviceInfo = "Integration Pixel"
        )

        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        orchestrator.start(); advanceUntilIdle()
        orchestrator.recordResult(TestType.TAP, 0.9, mapOf("a" to 1.0)); advanceUntilIdle()
        orchestrator.recordResult(TestType.GAIT, 0.8, mapOf("b" to 2.0)); advanceUntilIdle()
        orchestrator.recordResult(TestType.VISION, 0.7, mapOf("c" to 3.0)); advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Completed, orchestrator.state.value)

        val sessionList = db.sessionDao().observeAll().first()
        assertEquals(1, sessionList.size)
        val session = sessionList.first()
        assertNotNull(session.completedAtEpochMs)

        val results = db.testResultDao().getForSession(session.id)
        assertEquals(3, results.size)
        val visionFeatures = results.first { it.testType == TestType.VISION }.featuresJson
        assertTrue(visionFeatures.contains("c") && visionFeatures.contains("3"))
    }
}
```

- [ ] **Step 2: Run the test**

```bash
cd ~/src/BaselineMS
./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.BatteryFlowIntegrationTest"
```

Expected: BUILD SUCCESSFUL.

(This test exercises only existing code, so it should pass on first run. If it fails, fix the orchestrator or the DAOs before continuing.)

- [ ] **Step 3: Commit**

```bash
cd ~/src/BaselineMS
git add .
git commit -m "test(battery): add end to end integration test"
```

---

## Task 12: Add a README

**Files:**
- Create: `~/src/BaselineMS/README.md`

The README is the artifact a recruiter will read. We seed it now and grow it as later phases land.

- [ ] **Step 1: Create the README**

File: `~/src/BaselineMS/README.md`

```markdown
# BaselineMS (Android)

A native Android application that lets people living with Multiple Sclerosis self administer a short, clinically grounded battery of five tests once a week, track their results longitudinally on device, and share a clear summary report with their neurologist.

## Status

Phase 1 (foundation) complete. The application runs end to end against a mock test module. Real test modules are added in subsequent phases.

## Architecture

See `~/src/docs/superpowers/specs/2026-05-06-baselinems-design.md` for the full design specification.

## Running

Open `~/src/BaselineMS` in Android Studio Iguana or later, select an Android 12+ emulator (or plug in a physical device with USB debugging enabled), and press Run.

From the command line:

```
./gradlew :app:installDebug
adb shell am start -n com.mustafan4x.baselinems/.MainActivity
```

## Testing

```
./gradlew :app:testDebugUnitTest
```

This runs all JVM unit tests including the Room repository tests (via Robolectric) and the orchestrator tests.

## Privacy

All data is stored on device. No cloud sync, no account, no analytics, no `INTERNET` permission.
```

- [ ] **Step 2: Commit**

```bash
cd ~/src/BaselineMS
git add README.md
git commit -m "docs: add README for Phase 1"
```

---

## Phase 1 Done When

- All 12 tasks above are committed.
- `./gradlew :app:testDebugUnitTest` returns `BUILD SUCCESSFUL` with zero failures across `EnumsTest`, `ConvertersTest`, `UserProfileDaoTest`, `SessionDaoTest`, `TestResultDaoTest`, `MockTestModuleTest`, `BatteryOrchestratorTest`, and `BatteryFlowIntegrationTest`.
- The emulator walkthrough in Task 10 Step 5 succeeds end to end on a real Android 12+ AVD.
- A clean working tree on `main` of the `BaselineMS` git repo.

The next plan (Phase 2: Tap Test) will swap `MockTestModule` for the real bilateral tap test, leaving everything else untouched.
