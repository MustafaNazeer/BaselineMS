package com.mustafan4x.msbattery.ui.common

import com.mustafan4x.msbattery.data.Hand
import com.mustafan4x.msbattery.data.MSType
import com.mustafan4x.msbattery.data.Sex
import com.mustafan4x.msbattery.data.TestType

fun Sex.displayLabel(): String = when (this) {
    Sex.FEMALE -> "Female"
    Sex.MALE -> "Male"
    Sex.OTHER -> "Other"
    Sex.UNDISCLOSED -> "Prefer not to say"
}

fun Hand.displayLabel(): String = when (this) {
    Hand.LEFT -> "Left"
    Hand.RIGHT -> "Right"
    Hand.AMBIDEXTROUS -> "Either hand"
}

fun MSType.displayLabel(): String = when (this) {
    MSType.RRMS -> "Relapsing remitting (RRMS)"
    MSType.PPMS -> "Primary progressive (PPMS)"
    MSType.SPMS -> "Secondary progressive (SPMS)"
    MSType.CIS -> "Clinically isolated syndrome (CIS)"
    MSType.UNDISCLOSED -> "Prefer not to say"
}

fun TestType.displayLabel(): String = when (this) {
    TestType.TAP -> "Bilateral Tap"
    TestType.GAIT -> "Gait"
    TestType.VISION -> "Low Contrast Vision"
    TestType.SDMT -> "Symbol Digit"
    TestType.VOICE -> "Voice Reading"
}
