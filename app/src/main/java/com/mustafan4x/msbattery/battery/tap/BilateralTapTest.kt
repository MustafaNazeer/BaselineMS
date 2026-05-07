package com.mustafan4x.msbattery.battery.tap

import androidx.compose.runtime.Composable
import com.mustafan4x.msbattery.battery.TestModule
import com.mustafan4x.msbattery.battery.TestResultPayload
import com.mustafan4x.msbattery.data.TestType

class BilateralTapTest : TestModule {

    data class TapTestResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>
    ) : TestResultPayload

    override val testType: TestType = TestType.TAP
    override val displayName: String = "Bilateral Tap"
    override val instructions: String =
        "Hold your phone with both hands. Tap the left and right circles, alternating, " +
            "as fast as you can manage. You will do one round with your dominant hand " +
            "and one round with your non dominant hand. Each round lasts 30 seconds."
    override val estimatedDurationSeconds: Int = 70

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        BilateralTapTestContent(onComplete = onComplete)
    }
}

@Composable
internal fun BilateralTapTestContent(onComplete: (TestResultPayload) -> Unit) {
    androidx.compose.material3.Text(
        "BilateralTapTest UI is implemented in the next task."
    )
}
