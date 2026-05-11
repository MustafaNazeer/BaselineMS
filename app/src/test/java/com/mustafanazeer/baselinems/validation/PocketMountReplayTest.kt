package com.mustafanazeer.baselinems.validation

import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.abs

/**
 * Replays the pocket-mounted walking trials captured under the protocol in
 * `docs/qa/pocket-mount-validation-protocol.md` through `GaitPipeline` and emits per trial
 * accuracy results to a CSV. The test is data-driven and skipped when no trial directory is
 * configured.
 *
 * Configuration:
 *   System property `baselinems.pocket.path` or env var `BASELINEMS_POCKET_MOUNT_PATH` points
 *   to the directory containing the captured trial directories. Optional system property
 *   `baselinems.pocket.output` sets the results CSV path (default
 *   `build/validation/pocket-mount-results.csv`).
 *
 * Expected layout:
 *
 *   $BASELINEMS_POCKET_MOUNT_PATH/
 *   ├── trial-01-slow/
 *   │   ├── GAIT.csv.gz       (the 14-column gzipped CSV the application writes per session)
 *   │   └── ground-truth.csv  (cadence_steps_per_minute,mean_stride_length_meters,strides,total_distance_meters)
 *   ├── trial-02-slow/...
 *   ├── trial-04-normal/...
 *   ├── trial-07-brisk/...
 *   └── ...
 *
 * The trial directory name encodes the pace as the trailing segment (`slow`, `normal`, `brisk`)
 * so the results CSV can carry the pace per trial without a separate manifest file.
 *
 * The captured `GAIT.csv.gz` already contains the gravity-removed linear acceleration and the
 * Android-fused rotation vector; no offline preprocessing is required.
 *
 * Loose accuracy bound: mean absolute cadence error under 15 percent and mean absolute mean-
 * stride-length error under 15 percent across all trials. Single subject; do not interpret as a
 * statistical accuracy claim.
 */
class PocketMountReplayTest {

    @Test
    fun `replay pocket mount captures and emit per-trial accuracy CSV`() {
        val datasetRoot = System.getProperty("baselinems.pocket.path")
            ?: System.getenv("BASELINEMS_POCKET_MOUNT_PATH")
        assumeNotNull(
            "set system property baselinems.pocket.path or env BASELINEMS_POCKET_MOUNT_PATH to enable",
            datasetRoot
        )
        val root = File(datasetRoot!!)
        assumeTrue("dataset root does not exist: ${root.absolutePath}", root.exists() && root.isDirectory)

        val outputCsv = File(
            System.getProperty("baselinems.pocket.output", "build/validation/pocket-mount-results.csv")!!
        )
        outputCsv.parentFile?.mkdirs()

        val pipeline = GaitPipeline()
        val results = mutableListOf<TrialResult>()

        outputCsv.bufferedWriter().use { out ->
            out.append("trial,pace,")
            out.append("cadence_recovered,cadence_truth,cadence_pct_error,")
            out.append("stride_length_recovered_m,stride_length_truth_m,stride_length_pct_error,")
            out.append("quality_score,detected_step_count\n")

            val trialDirs = root.listFiles { f -> f.isDirectory && f.name.startsWith("trial-") }
                ?.sortedBy { it.name }
                ?: emptyList()
            assumeTrue("no trial-* directories under ${root.absolutePath}", trialDirs.isNotEmpty())

            for (trialDir in trialDirs) {
                val gzCsv = File(trialDir, "GAIT.csv.gz")
                val truthCsv = File(trialDir, "ground-truth.csv")
                if (!gzCsv.exists()) {
                    System.err.println("skip ${trialDir.name}: missing GAIT.csv.gz")
                    continue
                }
                if (!truthCsv.exists()) {
                    System.err.println("skip ${trialDir.name}: missing ground-truth.csv")
                    continue
                }

                val pace = parsePace(trialDir.name)
                val samples = parseCapturedCsv(gzCsv)
                val truth = parseGroundTruth(truthCsv)
                val recovered = pipeline.process(samples)

                val cadenceErr = pctError(recovered.cadenceStepsPerMinute, truth.cadenceStepsPerMinute)
                val strideErr = pctError(recovered.meanStrideLengthMeters, truth.meanStrideLengthMeters)

                out.append(trialDir.name).append(',')
                out.append(pace).append(',')
                out.append(recovered.cadenceStepsPerMinute.toString()).append(',')
                out.append(truth.cadenceStepsPerMinute.toString()).append(',')
                out.append(cadenceErr.toString()).append(',')
                out.append(recovered.meanStrideLengthMeters.toString()).append(',')
                out.append(truth.meanStrideLengthMeters.toString()).append(',')
                out.append(strideErr.toString()).append(',')
                out.append(recovered.qualityScore.toString()).append(',')
                out.append(recovered.detectedStepCount.toString()).append('\n')

                results += TrialResult(
                    trial = trialDir.name,
                    pace = pace,
                    cadencePctError = cadenceErr,
                    strideLengthPctError = strideErr,
                    qualityScore = recovered.qualityScore
                )
            }
        }

        assumeTrue("no trials processed", results.isNotEmpty())

        val meanCadenceErr = results.map { abs(it.cadencePctError) }.average()
        val meanStrideErr = results.map { abs(it.strideLengthPctError) }.average()
        val meanQuality = results.map { it.qualityScore }.average()

        println("Pocket mount replay summary:")
        println("  trials processed: ${results.size}")
        println("  mean absolute cadence error: ${"%.2f".format(meanCadenceErr)}%")
        println("  mean absolute stride length error: ${"%.2f".format(meanStrideErr)}%")
        println("  mean quality score: ${"%.3f".format(meanQuality)}")
        println("  per-trial results written to: ${outputCsv.absolutePath}")

        assertTrue(
            "mean absolute cadence error $meanCadenceErr exceeds 15% bound on pocket mount data.",
            meanCadenceErr < 15.0
        )
        assertTrue(
            "mean absolute stride length error $meanStrideErr exceeds 15% bound on pocket mount data.",
            meanStrideErr < 15.0
        )
    }

    private data class TrialResult(
        val trial: String,
        val pace: String,
        val cadencePctError: Double,
        val strideLengthPctError: Double,
        val qualityScore: Double
    )

    private data class GroundTruth(
        val cadenceStepsPerMinute: Double,
        val meanStrideLengthMeters: Double
    )

    private fun pctError(recovered: Double, truth: Double): Double =
        if (truth == 0.0) 0.0 else 100.0 * (recovered - truth) / truth

    /**
     * Trial directory naming is `trial-NN-pace`. Returns the trailing pace segment, or "unknown"
     * if the name does not parse.
     */
    private fun parsePace(trialName: String): String {
        val parts = trialName.split('-')
        return if (parts.size >= 3) parts.subList(2, parts.size).joinToString("-") else "unknown"
    }

    /**
     * Parses the 14-column gzipped CSV the application writes per session via `RawSensorWriter`.
     * Columns match `RawSensorWriter.HEADER`:
     *
     *   timestampNanos, accelerometerXYZ, gyroscopeXYZ, linearAccelerationXYZ, rotationVectorWXYZ
     *
     * The rotation vector columns may be NaN for samples captured before the platform's fused
     * rotation sensor produced its first reading; such samples are emitted with a null
     * `rotationVector` so the pipeline falls back to identity rotation (the same behavior the
     * production application exhibits when a frame is missing).
     */
    private fun parseCapturedCsv(gzCsv: File): List<ImuSample> {
        val out = ArrayList<ImuSample>(4096)
        GZIPInputStream(gzCsv.inputStream()).use { gz ->
            BufferedReader(InputStreamReader(gz, Charsets.UTF_8)).use { reader ->
                val header = reader.readLine() ?: return emptyList()
                val columns = header.split(',')
                require(columns.size >= 14) {
                    "${gzCsv.absolutePath}: header has ${columns.size} columns, expected at least 14"
                }
                for (line in reader.lineSequence()) {
                    if (line.isBlank()) continue
                    val parts = line.split(',')
                    if (parts.size < 14) continue
                    val rotW = parts[10].toDouble()
                    val rotX = parts[11].toDouble()
                    val rotY = parts[12].toDouble()
                    val rotZ = parts[13].toDouble()
                    val rotation: Quaternion? =
                        if (rotW.isNaN() || rotX.isNaN() || rotY.isNaN() || rotZ.isNaN()) null
                        else Quaternion(rotW, rotX, rotY, rotZ)
                    out += ImuSample(
                        timestampNanos = parts[0].toLong(),
                        accelerometer = Vector3(parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble()),
                        gyroscope = Vector3(parts[4].toDouble(), parts[5].toDouble(), parts[6].toDouble()),
                        linearAcceleration = Vector3(parts[7].toDouble(), parts[8].toDouble(), parts[9].toDouble()),
                        rotationVector = rotation
                    )
                }
            }
        }
        return out
    }

    /**
     * Parses the per-trial ground truth CSV produced from the video count. Two columns are
     * required by header name: `cadence_steps_per_minute` and `mean_stride_length_meters`.
     */
    private fun parseGroundTruth(csv: File): GroundTruth {
        val lines = csv.readLines().filter { it.isNotBlank() }
        require(lines.size >= 2) { "${csv.absolutePath}: ground-truth.csv needs header + at least one row" }
        val header = lines[0].split(',').map { it.trim() }
        val cadenceIdx = header.indexOf("cadence_steps_per_minute")
        val strideIdx = header.indexOf("mean_stride_length_meters")
        require(cadenceIdx >= 0 && strideIdx >= 0) {
            "${csv.absolutePath}: header missing required columns; got ${header.joinToString(",")}"
        }
        val row = lines[1].split(',').map { it.trim() }
        return GroundTruth(
            cadenceStepsPerMinute = row[cadenceIdx].toDouble(),
            meanStrideLengthMeters = row[strideIdx].toDouble()
        )
    }
}
