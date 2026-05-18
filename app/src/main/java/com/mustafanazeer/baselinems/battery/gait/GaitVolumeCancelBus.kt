package com.mustafanazeer.baselinems.battery.gait

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process scoped bus that lets the `GaitCaptureScreen` ask `MainActivity` to intercept the
 * volume down key while a capture is running, and lets the activity push key timestamps back
 * to the screen. The bus is intentionally a singleton because `KeyEvent` dispatch happens on
 * the activity (not the composable) and the composable only learns about presses through the
 * shared flow.
 *
 * The bus does not interact with `SensorManager`, `AudioRecord`, or `AudioFocus`. Volume key
 * events are intercepted only after `isActive` is set to true by the capture screen entering,
 * and released as soon as the screen leaves. When `isActive` is false, `dispatchVolumeKeyEvent`
 * returns false and the activity passes the event up to the platform, which is the path
 * `MediaPlayer` and `AudioFocus` would normally take. Volume keys outside the capture screen
 * therefore behave exactly as the platform default.
 */
object GaitVolumeCancelBus {

    private val _isActive = MutableStateFlow(false)

    private val _holdStartedAt = MutableStateFlow<Long?>(null)
    val holdStartedAt: StateFlow<Long?> = _holdStartedAt.asStateFlow()

    private val _cancelRequested = MutableStateFlow(0)
    val cancelRequested: StateFlow<Int> = _cancelRequested.asStateFlow()

    private var clock: () -> Long = { android.os.SystemClock.elapsedRealtime() }

    fun setActive(active: Boolean) {
        _isActive.value = active
        if (!active) _holdStartedAt.value = null
    }

    internal fun setClockForTest(provider: () -> Long) {
        clock = provider
    }

    internal fun resetForTest() {
        _isActive.value = false
        _holdStartedAt.value = null
        _cancelRequested.value = 0
        clock = { android.os.SystemClock.elapsedRealtime() }
    }

    /**
     * Returns true if the key event was consumed for the long press cancel gesture and the
     * activity should not propagate it further. Only consumes `KEYCODE_VOLUME_DOWN` while a
     * capture screen is active. Other volume key codes and any keys outside the capture window
     * fall through to the platform default.
     */
    fun dispatchVolumeKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (!_isActive.value) return false
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) _holdStartedAt.value = clock()
            }
            KeyEvent.ACTION_UP -> {
                val started = _holdStartedAt.value
                _holdStartedAt.value = null
                if (started != null && clock() - started >= HOLD_THRESHOLD_MS) {
                    _cancelRequested.value = _cancelRequested.value + 1
                }
            }
        }
        return true
    }

    const val HOLD_THRESHOLD_MS: Long = 1_500L
}
