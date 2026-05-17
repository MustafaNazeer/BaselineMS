package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.dsp.voice.FeatureQualityFlag
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceQuality
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore
import com.mustafanazeer.baselinems.signals.AudioCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTestViewModelTest {

    private class FakeAudioCapture(
        private val fixture: ShortArray = ShortArray(44_100 * 30),
        private val throwSecurity: Boolean = false,
        private val throwIllegalState: Boolean = false,
        private val suspendForever: Boolean = false
    ) : AudioCapture {
        var recordCallCount: Int = 0
            private set
        var lastDurationSec: Int = -1
            private set

        override suspend fun record(durationSec: Int): ShortArray {
            recordCallCount += 1
            lastDurationSec = durationSec
            if (throwSecurity) throw SecurityException("denied")
            if (throwIllegalState) throw IllegalStateException("hal failed")
            if (suspendForever) {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }
            return fixture
        }
    }

    private fun TestScope.newViewModel(
        capture: AudioCapture = FakeAudioCapture()
    ): VoiceTestViewModel {
        return VoiceTestViewModel(
            audioCapture = capture,
            scope = this,
            clockMs = { currentTime }
        )
    }

    @Test
    fun `starts in Instructions state`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        assertTrue(vm.state.value is VoiceTestState.Instructions)
    }

    @Test
    fun `onStart transitions to RecordAudioRequested`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart()
        assertTrue(vm.state.value is VoiceTestState.RecordAudioRequested)
    }

    @Test
    fun `permission granted routes to AudioQualityCheck Pending band`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = true)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Pending, s.band)
            assertNull(s.noiseFloorRmsDbFs)
        }

    @Test
    fun `permission denied routes to RecordAudioDenied`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = false)
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `denied path does not re request in the same session`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = false)
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
            vm.onStart()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `noise floor below green threshold yields green band`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Green, s.band)
        }

    @Test
    fun `noise floor in yellow band yields yellow`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-40.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Yellow, s.band)
        }

    @Test
    fun `noise floor above yellow threshold yields red`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-20.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Red, s.band)
        }

    @Test
    fun `red band blocks onQualityCheckPassed`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart(); vm.onPermissionResult(granted = true)
        vm.onNoiseFloorMeasured(-10.0)
        vm.onQualityCheckPassed()
        assertTrue(vm.state.value is VoiceTestState.AudioQualityCheck)
    }

    @Test
    fun `green band onQualityCheckPassed transitions to Running and produces Done`() =
        runTest(UnconfinedTestDispatcher()) {
            val capture = FakeAudioCapture()
            val vm = newViewModel(capture)
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            val s = vm.state.value
            assertTrue("expected Done, was ${s::class.simpleName}", s is VoiceTestState.Done)
            assertEquals(1, capture.recordCallCount)
            assertEquals(30, capture.lastDurationSec)
        }

    @Test
    fun `cancel from Running transitions to Cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(suspendForever = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            assertTrue(vm.state.value is VoiceTestState.Running)
            vm.onCancel()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Cancelled)
        }

    @Test
    fun `SecurityException from AudioCapture maps to RecordAudioDenied`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwSecurity = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `IllegalStateException from AudioCapture maps to Cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwIllegalState = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Cancelled)
        }

    @Test
    fun `capture buffer is zeroed after a successful Done transition`() =
        runTest(UnconfinedTestDispatcher()) {
            val shared = ShortArray(44_100 * 30) { (it % 100).toShort() }
            val capture = FakeAudioCapture(fixture = shared)
            val vm = newViewModel(capture)
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Done)
            assertTrue(
                "buffer not zeroed after Done; first nonzero index = " +
                    shared.indexOfFirst { it != 0.toShort() },
                shared.all { it == 0.toShort() }
            )
        }

    @Test
    fun `capture buffer is zeroed after a SecurityException`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwSecurity = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `permission result with no pending request is a no op`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onPermissionResult(granted = true)
            assertTrue(vm.state.value is VoiceTestState.Instructions)
        }

    @Test
    fun `start is a no op outside Instructions`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart()
        val before = vm.state.value
        vm.onStart()
        assertEquals(before, vm.state.value)
    }

    @Test
    fun `persistedFeaturesJsonContainsQualityFlagsAndSessionFlags`() {
        val features = sampleFeatures()
        val quality = sampleQuality(allOk = true)

        val json = buildVoiceFeaturesJson(features = features, quality = quality)
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals(0.0123, root[VoiceQuality.FEATURE_KEY_JITTER]!!.jsonPrimitive.double, 1e-12)
        assertEquals(0.0456, root[VoiceQuality.FEATURE_KEY_SHIMMER]!!.jsonPrimitive.double, 1e-12)
        assertEquals(20.3, root[VoiceQuality.FEATURE_KEY_HNR]!!.jsonPrimitive.double, 1e-12)
        assertEquals(8.2, root[VoiceQuality.FEATURE_KEY_F0_SD]!!.jsonPrimitive.double, 1e-12)
        assertEquals(
            153.0,
            root[VoiceQuality.FEATURE_KEY_SPEAKING_RATE]!!.jsonPrimitive.double,
            1e-12
        )
        assertEquals(
            0.18,
            root[VoiceQuality.FEATURE_KEY_PAUSE_FRACTION]!!.jsonPrimitive.double,
            1e-12
        )

        val qFlags = root[KEY_QUALITY_FLAGS] as JsonObject
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_JITTER]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_SHIMMER]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_HNR]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_F0_SD]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_SPEAKING_RATE]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_PAUSE_FRACTION]!!.jsonPrimitive.boolean)

        val sFlags = root[KEY_SESSION_FLAGS] as JsonObject
        assertEquals(true, sFlags[KEY_ENGAGEMENT_OK]!!.jsonPrimitive.boolean)
        assertEquals(false, sFlags[KEY_CLIPPING_DETECTED]!!.jsonPrimitive.boolean)
        assertEquals(true, sFlags[KEY_SNR_ADEQUATE]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `persistedFeaturesJsonReflectsClippedAndDegradedFlags`() {
        val features = sampleFeatures()
        val quality = VoiceQualityScore(
            qualityScore = 0.0,
            perFeatureFlags = mapOf(
                VoiceQuality.FEATURE_KEY_JITTER to FeatureQualityFlag.CLIPPED,
                VoiceQuality.FEATURE_KEY_SHIMMER to FeatureQualityFlag.CLIPPED,
                VoiceQuality.FEATURE_KEY_HNR to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_F0_MEAN to FeatureQualityFlag.OK,
                VoiceQuality.FEATURE_KEY_F0_SD to FeatureQualityFlag.INSUFFICIENT_PERIODS,
                VoiceQuality.FEATURE_KEY_SPEAKING_RATE to FeatureQualityFlag.OK,
                VoiceQuality.FEATURE_KEY_PAUSE_FRACTION to FeatureQualityFlag.CLIPPED,
            ),
            engagementOk = false,
            clippingDetected = true,
            snrAdequate = false,
        )

        val json = buildVoiceFeaturesJson(features = features, quality = quality)
        val root = Json.parseToJsonElement(json).jsonObject
        val qFlags = root[KEY_QUALITY_FLAGS] as JsonObject

        assertEquals(false, qFlags[VoiceQuality.FEATURE_KEY_JITTER]!!.jsonPrimitive.boolean)
        assertEquals(false, qFlags[VoiceQuality.FEATURE_KEY_HNR]!!.jsonPrimitive.boolean)
        assertEquals(false, qFlags[VoiceQuality.FEATURE_KEY_F0_SD]!!.jsonPrimitive.boolean)
        assertEquals(true, qFlags[VoiceQuality.FEATURE_KEY_SPEAKING_RATE]!!.jsonPrimitive.boolean)

        val sFlags = root[KEY_SESSION_FLAGS] as JsonObject
        assertEquals(false, sFlags[KEY_ENGAGEMENT_OK]!!.jsonPrimitive.boolean)
        assertEquals(true, sFlags[KEY_CLIPPING_DETECTED]!!.jsonPrimitive.boolean)
        assertEquals(false, sFlags[KEY_SNR_ADEQUATE]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `persistedFeaturesJsonRoundTripsViaKotlinxSerialization`() {
        val features = sampleFeatures()
        val quality = sampleQuality(allOk = true)

        val json = buildVoiceFeaturesJson(features = features, quality = quality)
        val parsed = Json.parseToJsonElement(json).jsonObject
        val reEmitted = parsed.toString()

        val reParsed = Json.parseToJsonElement(reEmitted).jsonObject
        assertEquals(parsed, reParsed)
        assertEquals(
            features.jitterLocal!!,
            reParsed[VoiceQuality.FEATURE_KEY_JITTER]!!.jsonPrimitive.double,
            1e-12
        )
    }

    @Test
    fun `legacyFeaturesJsonWithoutReservedKeysParsesCleanly`() {
        val legacy = """{"jitter_local":0.0123,"shimmer_local":0.0456,"hnr_db":20.3}"""

        val root = Json.parseToJsonElement(legacy).jsonObject

        assertEquals(0.0123, root[VoiceQuality.FEATURE_KEY_JITTER]!!.jsonPrimitive.double, 1e-12)
        assertNull(root[KEY_QUALITY_FLAGS])
        assertNull(root[KEY_SESSION_FLAGS])
    }

    @Test
    fun `omitsNullFeaturesButAlwaysIncludesPauseFractionAndVoicedSeconds`() {
        val features = VoiceFeatureSet(
            jitterLocal = null,
            shimmerLocal = null,
            hnrDb = null,
            f0MeanHz = null,
            f0SdHz = null,
            speakingRateWpm = null,
            pauseFraction = 1.0,
            voicedSeconds = 0.0,
            totalSeconds = 30.0,
            periodCount = 0,
        )
        val quality = VoiceQualityScore(
            qualityScore = 0.0,
            perFeatureFlags = mapOf(
                VoiceQuality.FEATURE_KEY_JITTER to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_SHIMMER to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_HNR to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_F0_MEAN to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_F0_SD to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_SPEAKING_RATE to FeatureQualityFlag.INSUFFICIENT_VOICING,
                VoiceQuality.FEATURE_KEY_PAUSE_FRACTION to FeatureQualityFlag.OK,
            ),
            engagementOk = false,
            clippingDetected = false,
            snrAdequate = false,
        )

        val json = buildVoiceFeaturesJson(features = features, quality = quality)
        val root = Json.parseToJsonElement(json).jsonObject

        assertNull(root[VoiceQuality.FEATURE_KEY_JITTER])
        assertNull(root[VoiceQuality.FEATURE_KEY_SHIMMER])
        assertNull(root[VoiceQuality.FEATURE_KEY_HNR])
        assertNull(root[VoiceQuality.FEATURE_KEY_F0_SD])
        assertNull(root[VoiceQuality.FEATURE_KEY_SPEAKING_RATE])
        assertEquals(
            1.0,
            root[VoiceQuality.FEATURE_KEY_PAUSE_FRACTION]!!.jsonPrimitive.double,
            1e-12
        )
        assertEquals(0.0, root[KEY_VOICED_SECONDS]!!.jsonPrimitive.double, 1e-12)
        assertNotNull(root[KEY_QUALITY_FLAGS])
        assertNotNull(root[KEY_SESSION_FLAGS])
    }

    private fun sampleFeatures(): VoiceFeatureSet = VoiceFeatureSet(
        jitterLocal = 0.0123,
        shimmerLocal = 0.0456,
        hnrDb = 20.3,
        f0MeanHz = 120.0,
        f0SdHz = 8.2,
        speakingRateWpm = 153.0,
        pauseFraction = 0.18,
        voicedSeconds = 24.6,
        totalSeconds = 30.0,
        periodCount = 250,
    )

    private fun sampleQuality(allOk: Boolean): VoiceQualityScore {
        val flag = if (allOk) FeatureQualityFlag.OK else FeatureQualityFlag.INSUFFICIENT_VOICING
        return VoiceQualityScore(
            qualityScore = if (allOk) 1.0 else 0.0,
            perFeatureFlags = mapOf(
                VoiceQuality.FEATURE_KEY_JITTER to flag,
                VoiceQuality.FEATURE_KEY_SHIMMER to flag,
                VoiceQuality.FEATURE_KEY_HNR to flag,
                VoiceQuality.FEATURE_KEY_F0_MEAN to flag,
                VoiceQuality.FEATURE_KEY_F0_SD to flag,
                VoiceQuality.FEATURE_KEY_SPEAKING_RATE to flag,
                VoiceQuality.FEATURE_KEY_PAUSE_FRACTION to flag,
            ),
            engagementOk = allOk,
            clippingDetected = false,
            snrAdequate = allOk,
        )
    }
}
