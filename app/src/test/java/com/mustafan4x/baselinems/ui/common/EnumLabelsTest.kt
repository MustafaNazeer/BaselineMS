package com.mustafan4x.baselinems.ui.common

import com.mustafan4x.baselinems.data.Hand
import com.mustafan4x.baselinems.data.MSType
import com.mustafan4x.baselinems.data.Sex
import com.mustafan4x.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Test

class EnumLabelsTest {

    @Test fun sexLabels() {
        assertEquals("Female", Sex.FEMALE.displayLabel())
        assertEquals("Male", Sex.MALE.displayLabel())
        assertEquals("Other", Sex.OTHER.displayLabel())
        assertEquals("Prefer not to say", Sex.UNDISCLOSED.displayLabel())
    }

    @Test fun handLabels() {
        assertEquals("Left", Hand.LEFT.displayLabel())
        assertEquals("Right", Hand.RIGHT.displayLabel())
        assertEquals("Either hand", Hand.AMBIDEXTROUS.displayLabel())
    }

    @Test fun msTypeLabels() {
        assertEquals("Relapsing remitting (RRMS)", MSType.RRMS.displayLabel())
        assertEquals("Primary progressive (PPMS)", MSType.PPMS.displayLabel())
        assertEquals("Secondary progressive (SPMS)", MSType.SPMS.displayLabel())
        assertEquals("Clinically isolated syndrome (CIS)", MSType.CIS.displayLabel())
        assertEquals("Prefer not to say", MSType.UNDISCLOSED.displayLabel())
    }

    @Test fun testTypeLabels() {
        assertEquals("Bilateral Tap", TestType.TAP.displayLabel())
        assertEquals("Gait", TestType.GAIT.displayLabel())
        assertEquals("Low Contrast Vision", TestType.VISION.displayLabel())
        assertEquals("Symbol Digit", TestType.SDMT.displayLabel())
        assertEquals("Voice Reading", TestType.VOICE.displayLabel())
    }
}
