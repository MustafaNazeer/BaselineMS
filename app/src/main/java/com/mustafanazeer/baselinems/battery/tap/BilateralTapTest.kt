package com.mustafanazeer.baselinems.battery.tap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType
import kotlinx.coroutines.delay

class BilateralTapTest : TestModule {

    data class TapTestResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>
    ) : TestResultPayload

    override val testType: TestType = TestType.TAP
    override val displayName: String = "Bilateral Tap"
    override val instructions: String =
        "Hold your phone with both hands. Tap the left and right circles, alternating, " +
            "as fast as you can manage. Tap with the hand you usually write or text with " +
            "first, then the other hand. Each round lasts 30 seconds."
    override val estimatedDurationSeconds: Int = 70

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        BilateralTapTestContent(onComplete = onComplete)
    }
}

private const val ROUND_DURATION_MS = 30_000L
private const val COUNTDOWN_SECONDS = 5
internal const val INTER_ROUND_REST_SECONDS = 5

internal sealed class TapPhase {
    data object PreInstructions : TapPhase()
    data class Countdown(val role: HandRole, val secondsRemaining: Int) : TapPhase()
    data class Active(val role: HandRole, val startedAtMs: Long) : TapPhase()
    data class RestingBetweenRounds(val secondsRemaining: Int) : TapPhase()
    data class Done(val result: BilateralTapTest.TapTestResult) : TapPhase()
}

@Composable
internal fun BilateralTapTestContent(
    onComplete: (TestResultPayload) -> Unit,
    initialPhase: TapPhase = TapPhase.PreInstructions
) {
    var phase by remember { mutableStateOf(initialPhase) }
    var dominantEvents by remember { mutableStateOf<List<TapEvent>>(emptyList()) }
    var nonDominantEvents by remember { mutableStateOf<List<TapEvent>>(emptyList()) }
    var dominantOffTarget by remember { mutableStateOf(0) }
    var nonDominantOffTarget by remember { mutableStateOf(0) }
    var liveTapCount by remember { mutableStateOf(0) }
    var elapsedSec by remember { mutableStateOf(0) }

    LaunchedEffect(phase) {
        when (val p = phase) {
            is TapPhase.Countdown -> {
                delay(1000)
                phase = if (p.secondsRemaining > 1) {
                    p.copy(secondsRemaining = p.secondsRemaining - 1)
                } else {
                    elapsedSec = 0
                    TapPhase.Active(role = p.role, startedAtMs = System.currentTimeMillis())
                }
            }
            is TapPhase.Active -> {
                val totalSec = (ROUND_DURATION_MS / 1000).toInt()
                while (elapsedSec < totalSec) {
                    delay(1000)
                    elapsedSec += 1
                }
                if (p.role == HandRole.DOMINANT) {
                    phase = TapPhase.RestingBetweenRounds(secondsRemaining = INTER_ROUND_REST_SECONDS)
                } else {
                    val dominantRound = TapRound(
                        role = HandRole.DOMINANT,
                        durationMs = ROUND_DURATION_MS,
                        events = dominantEvents,
                        offTargetTaps = dominantOffTarget
                    )
                    val nonDominantRound = TapRound(
                        role = HandRole.NON_DOMINANT,
                        durationMs = ROUND_DURATION_MS,
                        events = nonDominantEvents,
                        offTargetTaps = nonDominantOffTarget
                    )
                    val features = TapFeatures.computeSession(dominantRound, nonDominantRound)
                    val result = BilateralTapTest.TapTestResult(
                        qualityScore = features.qualityScore,
                        features = features.toFeatureMap()
                    )
                    phase = TapPhase.Done(result = result)
                }
            }
            is TapPhase.RestingBetweenRounds -> {
                delay(1000)
                phase = if (p.secondsRemaining > 1) {
                    p.copy(secondsRemaining = p.secondsRemaining - 1)
                } else {
                    liveTapCount = 0
                    TapPhase.Countdown(role = HandRole.NON_DOMINANT, secondsRemaining = COUNTDOWN_SECONDS)
                }
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Tap the left and right circles, alternating, as fast as you can manage.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        when (val p = phase) {
            is TapPhase.PreInstructions -> {
                Text(
                    stringResource(R.string.tap_test_pre_instructions_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = {
                    liveTapCount = 0
                    elapsedSec = 0
                    dominantEvents = emptyList()
                    nonDominantEvents = emptyList()
                    dominantOffTarget = 0
                    nonDominantOffTarget = 0
                    phase = TapPhase.Countdown(role = HandRole.DOMINANT, secondsRemaining = COUNTDOWN_SECONDS)
                }) { Text("Start dominant hand round") }
            }
            is TapPhase.Countdown -> {
                val roleLabel = if (p.role == HandRole.DOMINANT) "Dominant hand" else "Non dominant hand"
                Text(roleLabel, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Starting in ${p.secondsRemaining}",
                    style = MaterialTheme.typography.displayMedium
                )
            }
            is TapPhase.Active -> {
                val roleLabel = if (p.role == HandRole.DOMINANT) "Dominant hand" else "Non dominant hand"
                val totalSec = (ROUND_DURATION_MS / 1000).toInt()
                val remaining = (totalSec - elapsedSec).coerceAtLeast(0)
                Text("$roleLabel, $remaining seconds left", style = MaterialTheme.typography.titleMedium)
                Text("Taps so far: $liveTapCount", style = MaterialTheme.typography.bodyMedium)
                TwoTargets(
                    onInTargetTap = { side ->
                        val events = if (p.role == HandRole.DOMINANT) dominantEvents else nonDominantEvents
                        val previous = events.lastOrNull { it.kind == TapKind.VALID }
                        val kind = when {
                            previous == null -> TapKind.VALID
                            previous.side != side -> TapKind.VALID
                            else -> TapKind.NON_ALTERNATING
                        }
                        val event = TapEvent(
                            timestampMs = System.currentTimeMillis() - p.startedAtMs,
                            side = side,
                            kind = kind
                        )
                        if (p.role == HandRole.DOMINANT) {
                            dominantEvents = dominantEvents + event
                        } else {
                            nonDominantEvents = nonDominantEvents + event
                        }
                        if (kind == TapKind.VALID) liveTapCount += 1
                    },
                    onOffTargetTap = {
                        if (p.role == HandRole.DOMINANT) {
                            dominantOffTarget += 1
                        } else {
                            nonDominantOffTarget += 1
                        }
                    }
                )
            }
            is TapPhase.RestingBetweenRounds -> {
                Text(
                    p.secondsRemaining.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.tap_test_rest_copy),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            is TapPhase.Done -> {
                Text(
                    "Tap test recorded.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { onComplete(p.result) }) {
                    Text(stringResource(R.string.tap_test_continue))
                }
            }
        }
    }
}

internal const val TWO_TARGETS_TAG = "TwoTargets"

@Composable
internal fun TwoTargets(onInTargetTap: (TapSide) -> Unit, onOffTargetTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag(TWO_TARGETS_TAG)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = true)
                    val up = waitForUpOrCancellation()
                    if (up != null && !up.isConsumed) {
                        onOffTargetTap()
                    }
                }
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Target(side = TapSide.LEFT, onTap = onInTargetTap)
        Spacer(Modifier.size(32.dp))
        Target(side = TapSide.RIGHT, onTap = onInTargetTap)
    }
}

@Composable
private fun Target(side: TapSide, onTap: (TapSide) -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onTap(side) }
    ) {
        Text(
            text = if (side == TapSide.LEFT) "L" else "R",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.displaySmall
        )
    }
}
