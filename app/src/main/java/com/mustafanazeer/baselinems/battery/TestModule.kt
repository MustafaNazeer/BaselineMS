package com.mustafanazeer.baselinems.battery

import androidx.compose.runtime.Composable
import com.mustafanazeer.baselinems.data.TestType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface TestResultPayload {
    val qualityScore: Double
    val features: Map<String, Double>
    val rawSensorRelativePath: String? get() = null
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
