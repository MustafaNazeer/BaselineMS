package com.mustafanazeer.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.ReportsScreenState
import com.mustafanazeer.baselinems.ui.reports.TestDetailScreenState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TrendsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var resultDao: TestResultDao
    private lateinit var repository: TrendsRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        resultDao = db.testResultDao()
        repository = TrendsRepository(sessionDao = sessionDao, testResultDao = resultDao)
    }

    @After
    fun teardown() { db.close() }

    private suspend fun seedSession(
        startedAt: Long,
        completed: Boolean = true
    ): SessionEntity {
        val session = SessionEntity(
            startedAtEpochMs = startedAt,
            completedAtEpochMs = if (completed) startedAt + 60_000L else null,
            deviceInfo = "Pixel"
        )
        sessionDao.insert(session)
        return session
    }

    private suspend fun seedResult(
        sessionId: String,
        testType: TestType,
        startedAt: Long,
        qualityScore: Double,
        featuresJson: String
    ) {
        resultDao.insert(
            TestResultEntity(
                sessionId = sessionId,
                testType = testType,
                startedAtEpochMs = startedAt,
                completedAtEpochMs = startedAt + 30_000L,
                qualityScore = qualityScore,
                featuresJson = featuresJson
            )
        )
    }

    @Test
    fun emptyDatabaseRendersReportsEmpty() = runTest {
        val state = repository.observeReportsState().first()
        assertTrue(state is ReportsScreenState.Empty)
    }

    @Test
    fun oneSessionRendersReadyWithSessionCountOne() = runTest {
        val session = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = session.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"cadence_steps_per_minute": 96.0}"""
        )
        val state = repository.observeReportsState().first()
        assertTrue(state is ReportsScreenState.Ready)
        val ready = state as ReportsScreenState.Ready
        val gaitCard = ready.testSummaries.first { it.testType == TestType.GAIT }
        assertEquals(1, gaitCard.sessionCount)
        assertEquals(QualityBand.HIGH, gaitCard.latestQualityBand)
        assertEquals(listOf(96.0), gaitCard.primaryFeatureSparkline)
    }

    @Test
    fun threeSessionsPopulateFullSummaryCard() = runTest {
        listOf(1_000L to 95.0, 2_000L to 97.0, 3_000L to 98.0).forEach { (t, cadence) ->
            val s = seedSession(startedAt = t)
            seedResult(
                sessionId = s.id,
                testType = TestType.GAIT,
                startedAt = t,
                qualityScore = 0.9,
                featuresJson = """{"cadence_steps_per_minute": $cadence}"""
            )
        }
        val state = repository.observeReportsState().first() as ReportsScreenState.Ready
        val gaitCard = state.testSummaries.first { it.testType == TestType.GAIT }
        assertEquals(3, gaitCard.sessionCount)
        assertEquals(listOf(95.0, 97.0, 98.0), gaitCard.primaryFeatureSparkline)
    }

    @Test
    fun missingFeatureRendersEmDashInSummaryRow() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"step_time_cv": 0.05}"""
        )
        val state = repository.observeTestDetailState(TestType.GAIT).first()
        assertTrue(state is TestDetailScreenState.Ready)
        val ready = state as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("—", row.perFeatureValues["cadence_steps_per_minute"])
        val cadenceSeries = ready.featureSeries.first { it.key == "cadence_steps_per_minute" }
        val point = cadenceSeries.points.single()
        assertTrue(point.omittedFromChart)
        assertTrue(point.value.isNaN())
    }

    @Test
    fun lowQualitySessionExcludedFromAxisWhenHighPresent() = runTest {
        val s1 = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s1.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"cadence_steps_per_minute": 95.0}"""
        )
        val s2 = seedSession(startedAt = 2_000L)
        seedResult(
            sessionId = s2.id,
            testType = TestType.GAIT,
            startedAt = 2_000L,
            qualityScore = 0.2,
            featuresJson = """{"cadence_steps_per_minute": 40.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val series = ready.featureSeries.first { it.key == "cadence_steps_per_minute" }
        val bandsInOrder = series.points.map { it.qualityBand }
        assertEquals(listOf(QualityBand.HIGH, QualityBand.LOW), bandsInOrder)
        assertFalse(series.points.first().omittedFromChart)
        assertFalse(series.points.last().omittedFromChart)
    }

    @Test
    fun allLowQualitySessionsKeepAllPointsRendered() = runTest {
        listOf(1_000L to 40.0, 2_000L to 45.0).forEach { (t, c) ->
            val s = seedSession(startedAt = t)
            seedResult(
                sessionId = s.id,
                testType = TestType.GAIT,
                startedAt = t,
                qualityScore = 0.2,
                featuresJson = """{"cadence_steps_per_minute": $c}"""
            )
        }
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val series = ready.featureSeries.first { it.key == "cadence_steps_per_minute" }
        assertEquals(2, series.points.size)
        assertTrue(series.points.all { it.qualityBand == QualityBand.LOW })
        assertTrue(series.points.none { it.omittedFromChart })
    }

    @Test
    fun speakingRateAboveCeilingOmitsPointFromChart() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """{"speaking_rate_wpm": 500.0, "voiced_seconds": 25.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val series = ready.featureSeries.first { it.key == "speaking_rate_wpm" }
        val point = series.points.single()
        assertTrue(point.omittedFromChart)
        assertEquals("Speech window too short", point.omissionReason)
    }

    @Test
    fun voicedSecondsBelowFloorOmitsSpeakingRatePoint() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """{"speaking_rate_wpm": 150.0, "voiced_seconds": 10.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val series = ready.featureSeries.first { it.key == "speaking_rate_wpm" }
        val point = series.points.single()
        assertTrue(point.omittedFromChart)
        assertEquals("Speech window too short", point.omissionReason)
    }

    @Test
    fun voicedSecondsAtOrAboveFloorKeepsSpeakingRatePoint() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """{"speaking_rate_wpm": 150.0, "voiced_seconds": 25.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val series = ready.featureSeries.first { it.key == "speaking_rate_wpm" }
        val point = series.points.single()
        assertFalse(point.omittedFromChart)
        assertEquals(150.0, point.value, 1e-9)
    }

    @Test
    fun legacyFeaturesJsonWithoutReservedKeysDefaultsToAllOk() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.3,
            featuresJson = """{"jitter_local": 0.02}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(QualityBand.LOW, row.qualityBand)
        assertEquals(R.string.phase9_context_cell_default_low, row.contextCellResId)
    }

    @Test
    fun qualityBandHighThresholdBoundary() {
        assertEquals(QualityBand.HIGH, deriveQualityBand(0.7))
        assertEquals(QualityBand.HIGH, deriveQualityBand(0.7001))
    }

    @Test
    fun qualityBandMediumThresholdBoundary() {
        assertEquals(QualityBand.MEDIUM, deriveQualityBand(0.4))
        assertEquals(QualityBand.MEDIUM, deriveQualityBand(0.6999))
    }

    @Test
    fun qualityBandLowJustBelowThreshold() {
        assertEquals(QualityBand.LOW, deriveQualityBand(0.39999))
        assertEquals(QualityBand.LOW, deriveQualityBand(0.0))
    }

    @Test
    fun voiceLowQualityShortCaptureContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "jitter_local": 0.02,
                  "_qualityFlags": {"jitter_local": true},
                  "_sessionFlags": {"engagementOk": false, "clippingDetected": false, "snrAdequate": true}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_voice_short, row.contextCellResId)
    }

    @Test
    fun voiceLowQualityNoisyContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "jitter_local": 0.02,
                  "_qualityFlags": {"jitter_local": true},
                  "_sessionFlags": {"engagementOk": true, "clippingDetected": true, "snrAdequate": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_voice_noisy, row.contextCellResId)
    }

    @Test
    fun voiceLowQualityDefaultContextCellWhenNoSpecificFlag() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "jitter_local": 0.02,
                  "_qualityFlags": {"jitter_local": true},
                  "_sessionFlags": {"engagementOk": true, "clippingDetected": false, "snrAdequate": true}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_default_low, row.contextCellResId)
    }

    @Test
    fun tapLowQualityShortContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.TAP,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "dominant_tap_rate_hz": 4.0,
                  "dominant_in_target_taps": 5.0,
                  "non_dominant_in_target_taps": 20.0
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.TAP).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_tap_short, row.contextCellResId)
    }

    @Test
    fun gaitLowQualityShortContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "cadence_steps_per_minute": 80.0,
                  "detected_step_count": 12.0
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_gait_short, row.contextCellResId)
    }

    @Test
    fun visionLowQualityDefaultContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VISION,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """{"sloan_total": 80.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.VISION).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(R.string.phase9_context_cell_default_low, row.contextCellResId)
    }

    @Test
    fun highQualitySessionHasNoContextCell() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"cadence_steps_per_minute": 95.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertNull(row.contextCellResId)
    }

    @Test
    fun observeTestDetailEmptyWhenNoSessionsOfThatType() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = "{}"
        )
        val state = repository.observeTestDetailState(TestType.VOICE).first()
        assertTrue(state is TestDetailScreenState.Empty)
    }

    @Test
    fun sparklineCapsAtSixPoints() = runTest {
        for (i in 0 until 10) {
            val t = 1_000L + i * 1_000L
            val s = seedSession(startedAt = t)
            seedResult(
                sessionId = s.id,
                testType = TestType.GAIT,
                startedAt = t,
                qualityScore = 0.9,
                featuresJson = """{"cadence_steps_per_minute": ${90 + i}.0}"""
            )
        }
        val ready = repository.observeReportsState().first() as ReportsScreenState.Ready
        val gait = ready.testSummaries.first { it.testType == TestType.GAIT }
        assertEquals(10, gait.sessionCount)
        assertEquals(6, gait.primaryFeatureSparkline.size)
        assertEquals(99.0, gait.primaryFeatureSparkline.last(), 1e-9)
    }

    @Test
    fun readyStatePreservesBatteryOrderForCards() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = "{}"
        )
        val ready = repository.observeReportsState().first() as ReportsScreenState.Ready
        val order = ready.testSummaries.map { it.testType }
        assertEquals(
            listOf(TestType.TAP, TestType.GAIT, TestType.VISION, TestType.SDMT, TestType.VOICE),
            order
        )
        val gait = ready.testSummaries.first { it.testType == TestType.GAIT }
        assertNotNull(gait.latestQualityBand)
    }

    @Test
    fun voiceDirectiveClippingAnnotationSurfacesRegardlessOfBand() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """
                {
                  "jitter_local": 0.008,
                  "shimmer_local": 0.030,
                  "_qualityFlags": {"jitter_local": true, "shimmer_local": true},
                  "_sessionFlags": {"engagementOk": true, "clippingDetected": true, "snrAdequate": true}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(QualityBand.HIGH, row.qualityBand)
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_clipping))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_noisy))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_short))
    }

    @Test
    fun voiceDirectiveNoisyAnnotationSurfacesWhenSnrInadequate() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "jitter_local": 0.008,
                  "shimmer_local": 0.030,
                  "_qualityFlags": {"jitter_local": true, "shimmer_local": true},
                  "_sessionFlags": {"engagementOk": true, "clippingDetected": false, "snrAdequate": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_noisy))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_clipping))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_short))
    }

    @Test
    fun voiceDirectiveShortAnnotationSurfacesWhenEngagementOff() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "jitter_local": 0.008,
                  "shimmer_local": 0.030,
                  "_qualityFlags": {"jitter_local": true, "shimmer_local": true},
                  "_sessionFlags": {"engagementOk": false, "clippingDetected": false, "snrAdequate": true}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_short))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_clipping))
        assertFalse(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_noisy))
    }

    @Test
    fun voiceDirectiveAnnotationsCanCoexist() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.2,
            featuresJson = """
                {
                  "jitter_local": 0.008,
                  "shimmer_local": 0.030,
                  "_qualityFlags": {"jitter_local": true, "shimmer_local": true},
                  "_sessionFlags": {"engagementOk": false, "clippingDetected": true, "snrAdequate": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(3, row.sessionAnnotationResIds.size)
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_clipping))
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_noisy))
        assertTrue(row.sessionAnnotationResIds.contains(R.string.phase9_voice_directive_short))
    }

    @Test
    fun nonVoiceSessionHasNoDirectiveAnnotations() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"cadence_steps_per_minute": 96.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertTrue(row.sessionAnnotationResIds.isEmpty())
    }

    @Test
    fun voiceSteadinessBandSteadyForLowJitterAndShimmer() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"jitter_local": 0.005, "shimmer_local": 0.020}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("Steady", row.steadinessBandLabel)
    }

    @Test
    fun voiceSteadinessBandMostlySteadyAboveSteadyCutoff() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"jitter_local": 0.015, "shimmer_local": 0.060}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("Mostly steady", row.steadinessBandLabel)
    }

    @Test
    fun voiceSteadinessBandVariedAboveMostlyCutoff() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.7,
            featuresJson = """{"jitter_local": 0.025, "shimmer_local": 0.090}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("Varied", row.steadinessBandLabel)
    }

    @Test
    fun voiceSteadinessBandUnmeasurableWhenJitterMissing() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.3,
            featuresJson = """{"shimmer_local": 0.060}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("Unmeasurable", row.steadinessBandLabel)
    }

    @Test
    fun voiceSteadinessBandUnmeasurableWhenShimmerMissing() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.3,
            featuresJson = """{"jitter_local": 0.008}"""
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals("Unmeasurable", row.steadinessBandLabel)
    }

    @Test
    fun nonVoiceSessionHasNullSteadinessBandLabel() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """{"cadence_steps_per_minute": 96.0}"""
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertNull(row.steadinessBandLabel)
    }

    @Test
    fun voicePerFeatureFlagJitterUnreliableSurfaces() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "jitter_local": 0.02,
                  "_qualityFlags": {"jitter_local": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(
            R.string.phase9_voice_flag_jitter_unreliable,
            row.perFeatureAnnotationResIds["jitter_local"]
        )
    }

    @Test
    fun voicePerFeatureFlagShimmerUnreliableSurfaces() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "shimmer_local": 0.05,
                  "_qualityFlags": {"shimmer_local": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(
            R.string.phase9_voice_flag_shimmer_unreliable,
            row.perFeatureAnnotationResIds["shimmer_local"]
        )
    }

    @Test
    fun voicePerFeatureFlagHnrUnreliableSurfaces() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "hnr_db": 12.0,
                  "_qualityFlags": {"hnr_db": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(
            R.string.phase9_voice_flag_hnr_unreliable,
            row.perFeatureAnnotationResIds["hnr_db"]
        )
    }

    @Test
    fun voicePerFeatureFlagF0SdUnreliableSurfaces() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "f0_sd_hz": 30.0,
                  "_qualityFlags": {"f0_sd_hz": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(
            R.string.phase9_voice_flag_f0_sd_unreliable,
            row.perFeatureAnnotationResIds["f0_sd_hz"]
        )
    }

    @Test
    fun voicePerFeatureFlagPauseFractionUnreliableSurfaces() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.8,
            featuresJson = """
                {
                  "pause_fraction": 0.35,
                  "_qualityFlags": {"pause_fraction": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertEquals(
            R.string.phase9_voice_flag_pause_fraction_unreliable,
            row.perFeatureAnnotationResIds["pause_fraction"]
        )
    }

    @Test
    fun voicePerFeatureFlagAnnotationAbsentWhenFlagTrue() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.VOICE,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """
                {
                  "jitter_local": 0.008,
                  "shimmer_local": 0.030,
                  "_qualityFlags": {"jitter_local": true, "shimmer_local": true}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.VOICE).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertTrue(row.perFeatureAnnotationResIds.isEmpty())
    }

    @Test
    fun nonVoiceSessionHasNoPerFeatureFlagAnnotations() = runTest {
        val s = seedSession(startedAt = 1_000L)
        seedResult(
            sessionId = s.id,
            testType = TestType.GAIT,
            startedAt = 1_000L,
            qualityScore = 0.9,
            featuresJson = """
                {
                  "cadence_steps_per_minute": 96.0,
                  "_qualityFlags": {"cadence_steps_per_minute": false}
                }
            """.trimIndent()
        )
        val ready = repository.observeTestDetailState(TestType.GAIT).first()
            as TestDetailScreenState.Ready
        val row = ready.summaryRows.single()
        assertTrue(row.perFeatureAnnotationResIds.isEmpty())
    }
}
