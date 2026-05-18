package com.mustafanazeer.baselinems.battery.gait

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class GaitVolumeCancelBusTest {

    private var clock: Long = 0L

    @Before
    fun setup() {
        GaitVolumeCancelBus.resetForTest()
        GaitVolumeCancelBus.setClockForTest { clock }
    }

    @After
    fun teardown() {
        GaitVolumeCancelBus.resetForTest()
    }

    private fun keyEvent(action: Int, repeatCount: Int = 0): KeyEvent =
        KeyEvent(0L, 0L, action, KeyEvent.KEYCODE_VOLUME_DOWN, repeatCount)

    @Test
    fun `dispatch returns false when bus is not active`() {
        val consumed = GaitVolumeCancelBus.dispatchVolumeKeyEvent(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            keyEvent(KeyEvent.ACTION_DOWN)
        )
        assertFalse(
            "Volume keys outside the capture window must fall through to the platform",
            consumed
        )
        assertEquals(0, GaitVolumeCancelBus.cancelRequested.value)
    }

    @Test
    fun `dispatch ignores volume up even when active`() {
        GaitVolumeCancelBus.setActive(true)
        val consumed = GaitVolumeCancelBus.dispatchVolumeKeyEvent(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0)
        )
        assertFalse("Volume up must not be intercepted", consumed)
    }

    @Test
    fun `hold below 1500 ms does not trigger cancel`() {
        GaitVolumeCancelBus.setActive(true)
        clock = 1000L
        assertTrue(
            GaitVolumeCancelBus.dispatchVolumeKeyEvent(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                keyEvent(KeyEvent.ACTION_DOWN)
            )
        )
        assertNotNull(GaitVolumeCancelBus.holdStartedAt.value)

        clock = 1000L + 1_400L
        assertTrue(
            GaitVolumeCancelBus.dispatchVolumeKeyEvent(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                keyEvent(KeyEvent.ACTION_UP)
            )
        )
        assertNull(GaitVolumeCancelBus.holdStartedAt.value)
        assertEquals(
            "Hold under 1500 ms must not fire a cancel",
            0,
            GaitVolumeCancelBus.cancelRequested.value
        )
    }

    @Test
    fun `hold at or above 1500 ms increments cancelRequested`() {
        GaitVolumeCancelBus.setActive(true)
        clock = 2_000L
        GaitVolumeCancelBus.dispatchVolumeKeyEvent(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            keyEvent(KeyEvent.ACTION_DOWN)
        )

        clock = 2_000L + 1_500L
        GaitVolumeCancelBus.dispatchVolumeKeyEvent(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            keyEvent(KeyEvent.ACTION_UP)
        )

        assertEquals(
            "Holding volume down for the full threshold must fire a cancel",
            1,
            GaitVolumeCancelBus.cancelRequested.value
        )
        assertNull(GaitVolumeCancelBus.holdStartedAt.value)
    }

    @Test
    fun `setting active false clears the in flight hold`() {
        GaitVolumeCancelBus.setActive(true)
        clock = 0L
        GaitVolumeCancelBus.dispatchVolumeKeyEvent(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            keyEvent(KeyEvent.ACTION_DOWN)
        )
        assertNotNull(GaitVolumeCancelBus.holdStartedAt.value)

        GaitVolumeCancelBus.setActive(false)
        assertNull(
            "Leaving the capture screen mid hold must clear the in flight timestamp",
            GaitVolumeCancelBus.holdStartedAt.value
        )
    }
}
