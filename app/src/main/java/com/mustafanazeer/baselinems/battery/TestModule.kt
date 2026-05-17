package com.mustafanazeer.baselinems.battery

import androidx.compose.runtime.Composable
import com.mustafanazeer.baselinems.data.TestType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface TestResultPayload {
    val qualityScore: Double
    val features: Map<String, Double>
    val rawSensorRelativePath: String? get() = null

    /**
     * Optional pre serialized JSON string written to `TestResultEntity.featuresJson` in place of
     * the flat `features` map. Modules that need to extend the JSON shape with reserved nested
     * keys (Phase 9 Voice persists `_qualityFlags` and `_sessionFlags` per
     * `docs/data/schema.md` Phase 9 section) supply this override; the orchestrator hands the
     * string straight to Room. Default null preserves the Phase 1 contract for every test that
     * persists a flat feature map.
     */
    val featuresJsonOverride: String? get() = null
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
