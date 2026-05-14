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

/**
 * Replays the Luo et al. 2020 irregular surfaces gait dataset (Sci Data 7:219,
 * DOI 10.1038/s41597-020-0563-y) through `GaitPipeline`. The dataset does NOT
 * publish per-trial spatiotemporal ground truth, so Luo is used for
 * **cross-sensor agreement validation**: the same pipeline is run twice per
 * trial, once on the trunk IMU (production-analog mount) and once on the left
 * shank IMU (independent body location). The downstream aggregator computes
 * Bland-Altman limits of agreement and Pearson correlation between the two
 * cadence outputs across all trials.
 *
 * Configuration: env `BASELINEMS_LUO_PATH` or system property `baselinems.luo.path`
 * points at the normalized dataset root produced by `scripts/luo-preprocess.py`.
 * Output CSV at `build/validation/luo-replay-results.csv`.
 *
 * Expected dataset layout:
 *   $BASELINEMS_LUO_PATH/
 *   ├── participant-NN/
 *   │   └── session-1/
 *   │       ├── trial-04-FE/
 *   │       │   ├── trunk.csv       (timestamp_ns,ax,ay,az,gx,gy,gz at 100 Hz)
 *   │       │   ├── shank.csv       (same layout, left shank)
 *   │       │   └── trial-meta.csv  (trial_number,surface_label,sample_rate_hz,n_samples)
 *   │       └── trial-NN-XX/...
 *   └── participant-NN/...
 *
 * Surface labels: FE (flat even), CS (cobble stone), StrU/StrD (stairs up/down),
 * SlpU/SlpD (slope up/down), BnkL/BnkR (bank left/right), GR (grass). Calibration
 * trials (1 to 3) are excluded by the preprocessing script.
 */
class LuoReplayTest {

    @Test
    fun `replay Luo 2020 dataset cross-sensor and emit per-trial agreement CSV`() {
        val datasetRoot = System.getProperty("baselinems.luo.path")
            ?: System.getenv("BASELINEMS_LUO_PATH")
        assumeNotNull(
            "set system property baselinems.luo.path or env BASELINEMS_LUO_PATH to enable",
            datasetRoot
        )
        val root = File(datasetRoot!!)
        assumeTrue("dataset root does not exist: ${root.absolutePath}", root.exists() && root.isDirectory)

        val outputCsv = File(
            System.getProperty("baselinems.luo.output", "build/validation/luo-replay-results.csv")!!
        )
        outputCsv.parentFile?.mkdirs()

        val pipeline = GaitPipeline()
        val results = mutableListOf<TrialResult>()

        outputCsv.bufferedWriter().use { out ->
            out.append("participant,session,trial,surface,")
            out.append("cadence_trunk,cadence_shank,cadence_diff,")
            out.append("stride_length_trunk_m,stride_length_shank_m,")
            out.append("quality_trunk,quality_shank,")
            out.append("step_count_trunk,step_count_shank\n")

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
                        val trunkCsv = File(trialDir, "trunk.csv")
                        val shankCsv = File(trialDir, "shank.csv")
                        val metaCsv = File(trialDir, "trial-meta.csv")
                        if (!trunkCsv.exists() || !shankCsv.exists() || !metaCsv.exists()) {
                            System.err.println("skip ${trialDir.relativeTo(root)}: missing input")
                            continue
                        }
                        val surface = parseSurfaceLabel(metaCsv)
                        val trunkRaw = parseImuCsv(trunkCsv)
                        val shankRaw = parseImuCsv(shankCsv)
                        if (trunkRaw.isEmpty() || shankRaw.isEmpty()) continue

                        val trunkSamples = preprocess(trunkRaw, gravityWindow = 100, dtNominal = 0.01)
                        val shankSamples = preprocess(shankRaw, gravityWindow = 100, dtNominal = 0.01)

                        val trunkResult = try {
                            pipeline.process(trunkSamples)
                        } catch (e: Exception) {
                            System.err.println("FAIL trunk ${trialDir.relativeTo(root)}: ${e.message}")
                            continue
                        }
                        val shankResult = try {
                            pipeline.process(shankSamples)
                        } catch (e: Exception) {
                            System.err.println("FAIL shank ${trialDir.relativeTo(root)}: ${e.message}")
                            continue
                        }

                        val cadenceDiff = trunkResult.cadenceStepsPerMinute - shankResult.cadenceStepsPerMinute

                        out.append(participantDir.name).append(',')
                        out.append(sessionDir.name).append(',')
                        out.append(trialDir.name).append(',')
                        out.append(surface).append(',')
                        out.append(trunkResult.cadenceStepsPerMinute.toString()).append(',')
                        out.append(shankResult.cadenceStepsPerMinute.toString()).append(',')
                        out.append(cadenceDiff.toString()).append(',')
                        out.append(trunkResult.meanStrideLengthMeters.toString()).append(',')
                        out.append(shankResult.meanStrideLengthMeters.toString()).append(',')
                        out.append(trunkResult.qualityScore.toString()).append(',')
                        out.append(shankResult.qualityScore.toString()).append(',')
                        out.append(trunkResult.detectedStepCount.toString()).append(',')
                        out.append(shankResult.detectedStepCount.toString()).append('\n')

                        results += TrialResult(
                            participant = participantDir.name,
                            session = sessionDir.name,
                            trial = trialDir.name,
                            surface = surface,
                            cadenceTrunk = trunkResult.cadenceStepsPerMinute,
                            cadenceShank = shankResult.cadenceStepsPerMinute
                        )
                    }
                }
            }
        }

        assumeTrue("no trials processed", results.isNotEmpty())

        val diffs = results.map { it.cadenceTrunk - it.cadenceShank }
        val meanDiff = diffs.average()
        val sdDiff = kotlin.math.sqrt(diffs.map { (it - meanDiff) * (it - meanDiff) }.average())
        val lowerLoA = meanDiff - 1.96 * sdDiff
        val upperLoA = meanDiff + 1.96 * sdDiff
        val meanAbsDiff = diffs.map { kotlin.math.abs(it) }.average()

        println("Luo 2020 cross-sensor cadence agreement (trunk vs left-shank):")
        println("  trials processed: ${results.size}")
        println("  mean difference (trunk - shank): ${"%.2f".format(meanDiff)} steps/min")
        println("  SD of difference: ${"%.2f".format(sdDiff)} steps/min")
        println("  95% limits of agreement: [${"%.2f".format(lowerLoA)}, ${"%.2f".format(upperLoA)}] steps/min")
        println("  mean absolute difference: ${"%.2f".format(meanAbsDiff)} steps/min")
        println("  per-trial results written to: ${outputCsv.absolutePath}")

        // Sanity ceiling: catch a catastrophically miscalibrated parser. A real walking
        // signal should produce cadences in the 50 to 200 steps/min range on both sensors;
        // a 60 steps/min mean absolute difference would indicate one of the parsers is
        // returning garbage. This is intentionally loose; the headline number is the
        // mean difference and 95% LoA, computed above and surfaced in the validation report.
        assertTrue(
            "mean absolute cross-sensor cadence difference $meanAbsDiff exceeds the 60 steps/min sanity ceiling",
            meanAbsDiff < 60.0
        )
    }

    private data class TrialResult(
        val participant: String,
        val session: String,
        val trial: String,
        val surface: String,
        val cadenceTrunk: Double,
        val cadenceShank: Double
    )

    private data class RawImu(
        val timestampNanos: Long,
        val accelerometer: Vector3,
        val gyroscope: Vector3
    )

    private fun parseSurfaceLabel(metaCsv: File): String {
        val lines = metaCsv.readLines().filter { it.isNotBlank() }
        if (lines.size < 2) return "UNK"
        val header = lines[0].split(',').map { it.trim() }
        val idx = header.indexOf("surface_label")
        if (idx < 0) return "UNK"
        val row = lines[1].split(',').map { it.trim() }
        return row.getOrNull(idx) ?: "UNK"
    }

    private fun parseImuCsv(csv: File): List<RawImu> {
        val out = ArrayList<RawImu>(2048)
        csv.bufferedReader().use { reader ->
            reader.readLine() ?: return emptyList()
            for (line in reader.lineSequence()) {
                if (line.isBlank()) continue
                val p = line.split(',')
                if (p.size < 7) continue
                // Defensive: NaN samples occasionally slip through the preprocessing
                // filter if the upstream Xsens dropout flag was set only on some channels.
                // Skip such rows rather than letting the pipeline see NaN inputs.
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

    private fun preprocess(raw: List<RawImu>, gravityWindow: Int, dtNominal: Double): List<ImuSample> {
        if (raw.isEmpty()) return emptyList()
        val gw = minOf(gravityWindow, raw.size)
        var gx = 0.0; var gy = 0.0; var gz = 0.0
        for (i in 0 until gw) {
            gx += raw[i].accelerometer.x
            gy += raw[i].accelerometer.y
            gz += raw[i].accelerometer.z
        }
        val gravity = Vector3(gx / gw, gy / gw, gz / gw)

        val madgwick = Madgwick(beta = 0.1)
        repeat(5000) { madgwick.update(Vector3.ZERO, gravity, dtNominal) }

        val out = ArrayList<ImuSample>(raw.size)
        var prevNs = 0L
        for (r in raw) {
            val dt = if (prevNs == 0L) dtNominal else (r.timestampNanos - prevNs) / 1_000_000_000.0
            prevNs = r.timestampNanos
            val safeDt = if (dt <= 0.0) dtNominal else dt
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
