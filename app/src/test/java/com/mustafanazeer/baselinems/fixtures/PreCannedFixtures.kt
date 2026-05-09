package com.mustafanazeer.baselinems.fixtures

object PreCannedFixtures {

    fun healthyControlNormal(seed: Long = 1L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 115.2,
        strideLengthMeters = 1.442,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.03,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun msTypicalNormal(seed: Long = 2L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 94.4,
        strideLengthMeters = 0.906,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.05,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun slowWalk(seed: Long = 3L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 80.0,
        strideLengthMeters = 0.85,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.06,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun briskWalk(seed: Long = 4L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 130.0,
        strideLengthMeters = 1.55,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.025,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun mildAsymmetry(seed: Long = 5L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 100.0,
        strideLengthMeters = 1.30,
        asymmetryRatio = 1.10,
        stepTimeCv = 0.04,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun severeAsymmetry(seed: Long = 6L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 90.0,
        strideLengthMeters = 1.05,
        asymmetryRatio = 1.30,
        stepTimeCv = 0.08,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun noisyMsNormal(seed: Long = 7L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 94.4,
        strideLengthMeters = 0.906,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.05,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.5,
        sampleRateHz = 100.0,
        seed = seed
    )
}
