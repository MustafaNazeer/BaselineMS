package com.mustafanazeer.baselinems

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.mustafanazeer.baselinems.battery.gait.GaitVolumeCancelBus
import com.mustafanazeer.baselinems.ui.RootScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val volumeBus: GaitVolumeCancelBus = GaitVolumeCancelBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reclaimStrandedSessionsOnStart()
        setContent {
            MaterialTheme {
                Surface { RootScreen() }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null && volumeBus.dispatchVolumeKeyEvent(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null && volumeBus.dispatchVolumeKeyEvent(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun reclaimStrandedSessionsOnStart() {
        val app = application as BaselineMSApp
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            app.database.sessionDao().reclaimStrandedSessions(
                nowEpochMs = now,
                strandedBeforeEpochMs = now - STRANDED_THRESHOLD_MS
            )
        }
    }

    companion object {
        private const val STRANDED_THRESHOLD_MS: Long = 60_000L
    }
}
