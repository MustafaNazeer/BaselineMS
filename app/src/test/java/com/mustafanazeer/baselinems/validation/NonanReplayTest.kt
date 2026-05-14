package com.mustafanazeer.baselinems.validation

import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Madgwick
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Replays the NONAN GaitPrint young adults dataset (Likens et al. 2023, Sci Data
 * DOI 10.1038/s41597-023-02704-z) through `GaitPipeline` and emits per-trial
 * accuracy results to a CSV. Configuration mirrors `SantosReplayTest`: env var
 * `BASELINEMS_NONAN_PATH` (or system property `baselinems.nonan.path`) points at
 * the normalized dataset root produced by `scripts/nonan-preprocess.py`.
 *
 * NONAN provides the pelvis sensor as the production-analog mount (closest to a
 * front-pocket smartphone of any open dataset). The two-session-per-participant
 * structure (one week apart per protocol) supports test-retest ICC reporting.
 *
 * Expected dataset layout:
 *   $BASELINEMS_NONAN_PATH/
 *   ├── participant-NN/
 *   │   ├── session-1/
 *   │   │   ├── trial-NN/
 *   │   │   │   ├── smartphone.csv     (timestamp_ns,ax,ay,az,gx,gy,gz at 200 Hz)
 *   │   │   │   └── ground-truth.csv   (cadence_steps_per_minute,mean_stride_length_meters)
 *   │   │   └── ...
 *   │   └── session-2/...
 *   └── participant-NN/...
 *
 * Sample rate is 200 Hz; the BaselineMS pipeline is rate-agnostic so no rate
 * conversion is applied. The gravity window (first 1 s of samples = 200 samples
 * at 200 Hz) and Madgwick pre-warm `dt` are adjusted from the Santos values
 * accordingly.
 */
class NonanReplayTest {

    @Test
    fun `replay NONAN GaitPrint dataset and emit per-trial accuracy CSV`() {
        val datasetRoot = System.getProperty("baselinems.nonan.path")
            ?: System.getenv("BASELINEMS_NONAN_PATH")
        assumeNotNull(
            "set system property baselinems.nonan.path or env BASELINEMS_NONAN_PATH to enable",
            datasetRoot
        )
        val root = File(datasetRoot!!)
        assumeTrue("dataset root does not exist: ${root.absolutePath}", root.exists() && root.isDirectory)

        val outputCsv = File(
            System.getProperty("baselinems.nonan.output", "build/validation/nonan-replay-results.csv")!!
        )
        outputCsv.parentFile?.mkdirs()

        val pipeline = GaitPipeline()
        val results = mutableListOf<TrialResult>()

        outputCsv.bufferedWriter().use { out ->
            out.append("participant,session,trial,")
            out.append("cadence_recovered,cadence_truth,cadence_pct_error,")
            out.append("stride_length_recovered_m,stride_length_truth_m,stride_length_pct_error,")
            out.append("quality_score,detected_step_count\n")

            val participantDirs = root.listFiles { f -> f.isDirectory && f.name.startsWith("participant") }
                ?.sortedBy { it.name }
                ?: emptyList()
            assumeTrue("no participant-* directories under ${root.absolutePath}", participantDirs.isNotEmpty())

            for (participantDir in participantDirs) {
                val sessionDirs = participantDir.listFiles { f -> f.isDirectory && f.name.startsWith("session") }
                    ?.sortedBy { it.name } ?: continue
                for (sessionDir in sessionDirs) {
                    val trialDirs = sessionDir.listFiles { f -> f.isDirectory && f.name.startsWith("trial") }
                        ?.sortedBy { it.name } ?: continue
                    for (trialDir in trialDirs) {
                        val imuCsv = File(trialDir, "smartphone.csv")
                        val truthCsv = File(trialDir, "ground-truth.csv")
                        if (!imuCsv.exists() || !truthCsv.exists()) {
                            System.err.println("skip ${trialDir.relativeTo(root)}: missing input")
                            continue
                        }
                        val raw = parseSmartphoneCsv(imuCsv)
                        if (raw.isEmpty()) continue
                        val samples = preprocessTrial(raw)
                        val truth = parseGroundTruth(truthCsv)
                        val recovered = try {
                            pipeline.process(samples)
                        } catch (e: Exception) {
                            System.err.println("FAIL ${trialDir.relativeTo(root)}: ${e.message}")
                            continue
                        }

                        val cadenceErr = pctError(recovered.cadenceStepsPerMinute, truth.cadenceStepsPerMinute)
                        val strideErr = pctError(recovered.meanStrideLengthMeters, truth.meanStrideLengthMeters)

                        out.append(participantDir.name).append(',')
                        out.append(sessionDir.name).append(',')
                        out.append(trialDir.name).append(',')
                        out.append(recovered.cadenceStepsPerMinute.toString()).append(',')
                        out.append(truth.cadenceStepsPerMinute.toString()).append(',')
                        out.append(cadenceErr.toString()).append(',')
                        out.append(recovered.meanStrideLengthMeters.toString()).append(',')
                        out.append(truth.meanStrideLengthMeters.toString()).append(',')
                        out.append(strideErr.toString()).append(',')
                        out.append(recovered.qualityScore.toString()).append(',')
                        out.append(recovered.detectedStepCount.toString()).append('\n')

                        results += TrialResult(
                            participant = participantDir.name,
                            session = sessionDir.name,
                            trial = trialDir.name,
                            cadencePctError = cadenceErr,
                            strideLengthPctError = strideErr,
                            qualityScore = recovered.qualityScore
                        )
                    }
                }
            }
        }

        assumeTrue("no trials processed", results.isNotEmpty())

        val meanCadenceErr = results.map { abs(it.cadencePctError) }.average()
        val meanStrideErr = results.map { abs(it.strideLengthPctError) }.average()

        println("NONAN GaitPrint replay summary:")
        println("  trials processed: ${results.size}")
        println("  mean absolute cadence error: ${"%.2f".format(meanCadenceErr)}%")
        println("  mean absolute stride length error: ${"%.2f".format(meanStrideErr)}%")
        println("  per-trial results written to: ${outputCsv.absolutePath}")

        // Pelvis mount is the closest production analog. A 50 percent sanity ceiling
        // on cadence catches a wildly miscalibrated parser without locking in a
        // hard pass/fail threshold; the headline numbers belong in the validation
        // report, not the test.
        assertTrue(
            "mean absolute cadence error $meanCadenceErr exceeds the 50% sanity ceiling",
            meanCadenceErr < 50.0
        )
    }

    private data class TrialResult(
        val participant: String, val session: String, val trial: String,
        val cadencePctError: Double, val strideLengthPctError: Double, val qualityScore: Double
    )

    private data class GroundTruth(
        val cadenceStepsPerMinute: Double,
        val meanStrideLengthMeters: Double
    )

    private data class RawImu(
        val timestampNanos: Long, val accelerometer: Vector3, val gyroscope: Vector3
    )

    private fun pctError(recovered: Double, truth: Double): Double =
        if (truth == 0.0) 0.0 else 100.0 * (recovered - truth) / truth

    private fun parseSmartphoneCsv(csv: File): List<RawImu> {
        val out = ArrayList<RawImu>(16384)
        csv.bufferedReader().use { reader ->
            reader.readLine() ?: return emptyList()
            for (line in reader.lineSequence()) {
                if (line.isBlank()) continue
                val p = line.split(',')
                if (p.size < 7) continue
                val parsed = try {
                    RawImu(
                        timestampNanos = p[0].toLong(),
                        accelerometer = Vector3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble()),
                        gyroscope = Vector3(p[4].toDouble(), p[5].toDouble(), p[6].toDouble())
                    )
                } catch (_: NumberFormatException) {
                    continue
                }
                out += parsed
            }
        }
        return out
    }

    private fun parseGroundTruth(csv: File): GroundTruth {
        val lines = csv.readLines().filter { it.isNotBlank() }
        require(lines.size >= 2)
        val header = lines[0].split(',').map { it.trim() }
        val cadenceIdx = header.indexOf("cadence_steps_per_minute")
        val strideIdx = header.indexOf("mean_stride_length_meters")
        require(cadenceIdx >= 0 && strideIdx >= 0)
        val row = lines[1].split(',').map { it.trim() }
        return GroundTruth(
            cadenceStepsPerMinute = row[cadenceIdx].toDouble(),
            meanStrideLengthMeters = row[strideIdx].toDouble()
        )
    }

    private fun preprocessTrial(raw: List<RawImu>): List<ImuSample> {
        if (raw.isEmpty()) return emptyList()
        val gravityWindow = minOf(200, raw.size) // 1 s of samples at 200 Hz
        var gx = 0.0; var gy = 0.0; var gz = 0.0
        for (i in 0 until gravityWindow) {
            gx += raw[i].accelerometer.x
            gy += raw[i].accelerometer.y
            gz += raw[i].accelerometer.z
        }
        val gravity = Vector3(gx / gravityWindow, gy / gravityWindow, gz / gravityWindow)

        val madgwick = Madgwick(beta = 0.1)
        repeat(5000) { madgwick.update(Vector3.ZERO, gravity, 0.005) }

        val out = ArrayList<ImuSample>(raw.size)
        var prevTimestampNanos = 0L
        for (r in raw) {
            val dt = if (prevTimestampNanos == 0L) 0.005 else (r.timestampNanos - prevTimestampNanos) / 1_000_000_000.0
            prevTimestampNanos = r.timestampNanos
            val safeDt = if (dt <= 0.0) 0.005 else dt
            madgwick.update(r.gyroscope, r.accelerometer, safeDt)
            val orientation: Quaternion = madgwick.orientation()
            val linear = Vector3(
                r.accelerometer.x - gravity.x,
                r.accelerometer.y - gravity.y,
                r.accelerometer.z - gravity.z
            )
            out += ImuSample(
                timestampNanos = r.timestampNanos,
                accelerometer = r.accelerometer,
                gyroscope = r.gyroscope,
                linearAcceleration = linear,
                rotationVector = orientation
            )
        }
        return out
    }
}
