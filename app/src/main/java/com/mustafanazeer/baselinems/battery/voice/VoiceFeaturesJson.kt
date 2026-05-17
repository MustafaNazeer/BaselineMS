package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.dsp.voice.FeatureQualityFlag
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceQuality
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds the Phase 9 `featuresJson` payload for a Voice session: the bare keyed flat acoustic
 * features plus the two reserved nested objects (`_qualityFlags` and `_sessionFlags`). The shape
 * is documented in `docs/data/schema.md` Phase 9 section and consumed by the Phase 9 Reporting
 * layer's `TrendsRepository` deserializer.
 *
 * Underscore prefixed keys mark reserved metadata; the orchestrator treats the returned string as
 * opaque on the persistence side. Older Phase 8 sessions persisted as a flat `Map<String, Double>`
 * still deserialize cleanly per the backward compatibility note in the schema doc.
 */
internal fun buildVoiceFeaturesJson(
    features: VoiceFeatureSet,
    quality: VoiceQualityScore
): String {
    val obj: JsonObject = buildJsonObject {
        features.jitterLocal?.let { put(VoiceQuality.FEATURE_KEY_JITTER, it) }
        features.shimmerLocal?.let { put(VoiceQuality.FEATURE_KEY_SHIMMER, it) }
        features.hnrDb?.let { put(VoiceQuality.FEATURE_KEY_HNR, it) }
        features.f0MeanHz?.let { put(VoiceQuality.FEATURE_KEY_F0_MEAN, it) }
        features.f0SdHz?.let { put(VoiceQuality.FEATURE_KEY_F0_SD, it) }
        features.speakingRateWpm?.let { put(VoiceQuality.FEATURE_KEY_SPEAKING_RATE, it) }
        put(VoiceQuality.FEATURE_KEY_PAUSE_FRACTION, features.pauseFraction)
        put(KEY_VOICED_SECONDS, features.voicedSeconds)

        putJsonObject(KEY_QUALITY_FLAGS) {
            for ((key, flag) in quality.perFeatureFlags) {
                put(key, flag == FeatureQualityFlag.OK)
            }
        }
        putJsonObject(KEY_SESSION_FLAGS) {
            put(KEY_ENGAGEMENT_OK, quality.engagementOk)
            put(KEY_CLIPPING_DETECTED, quality.clippingDetected)
            put(KEY_SNR_ADEQUATE, quality.snrAdequate)
        }
    }
    return obj.toString()
}

internal const val KEY_VOICED_SECONDS: String = "voiced_seconds"
internal const val KEY_QUALITY_FLAGS: String = "_qualityFlags"
internal const val KEY_SESSION_FLAGS: String = "_sessionFlags"
internal const val KEY_ENGAGEMENT_OK: String = "engagementOk"
internal const val KEY_CLIPPING_DETECTED: String = "clippingDetected"
internal const val KEY_SNR_ADEQUATE: String = "snrAdequate"

private inline fun JsonObjectBuilder.putJsonObject(
    key: String,
    crossinline builder: JsonObjectBuilder.() -> Unit
) {
    put(key, buildJsonObject(builder))
}
