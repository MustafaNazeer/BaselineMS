package com.mustafanazeer.baselinems.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mustafanazeer.baselinems.BaselineMSApp
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.battery.gait.GaitTest
import com.mustafanazeer.baselinems.battery.gait.GaitTestViewModel
import com.mustafanazeer.baselinems.battery.sdmt.SdmtTest
import com.mustafanazeer.baselinems.battery.tap.BilateralTapTest
import com.mustafanazeer.baselinems.battery.vision.VisionTest
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.signals.RawSensorWriter
import com.mustafanazeer.baselinems.ui.home.HomeScreen
import com.mustafanazeer.baselinems.ui.home.SessionRunnerScreen
import com.mustafanazeer.baselinems.ui.onboarding.DisclaimerScreen
import com.mustafanazeer.baselinems.ui.onboarding.ProfileSetupScreen
import com.mustafanazeer.baselinems.ui.settings.SettingsScreen
import com.mustafanazeer.baselinems.util.DeviceInfo
import java.io.File

private const val PREFS = "baselinems_prefs"
private const val KEY_DISCLAIMER = "disclaimer_acknowledged"

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BaselineMSApp
    val nav = rememberNavController()

    var resolved by remember { mutableStateOf(false) }
    var disclaimerAcknowledged by remember { mutableStateOf(false) }
    var hasProfile by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        disclaimerAcknowledged = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISCLAIMER, false)
        hasProfile = app.database.userProfileDao().getFirst() != null
        resolved = true
    }

    if (!resolved) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when {
        !disclaimerAcknowledged -> "disclaimer"
        !hasProfile -> "profile"
        else -> "home"
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("disclaimer") {
            DisclaimerScreen(onAcknowledge = {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_DISCLAIMER, true).apply()
                disclaimerAcknowledged = true
                nav.navigate(if (hasProfile) "home" else "profile") {
                    popUpTo("disclaimer") { inclusive = true }
                }
            })
        }
        composable("profile") {
            ProfileSetupScreen(
                userProfileDao = app.database.userProfileDao(),
                onComplete = {
                    hasProfile = true
                    nav.navigate("home") { popUpTo("profile") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen(
                sessionDao = app.database.sessionDao(),
                onStartSession = { nav.navigate("session") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("session") {
            val orchestrator = remember {
                lateinit var built: BatteryOrchestrator
                val gaitTest = GaitTest(
                    viewModelFactory = { coroutineScope ->
                        val sessionId = built.activeSessionId ?: "unknown-session"
                        val targetFile = File(
                            app.filesDir,
                            "sensor_traces/$sessionId/${TestType.GAIT.name}.csv.gz"
                        )
                        GaitTestViewModel(
                            imuSource = app.imuSource,
                            gaitPipeline = GaitPipeline(),
                            rawSensorWriter = RawSensorWriter(targetFile),
                            destinationFile = targetFile,
                            filesDir = app.filesDir,
                            scope = coroutineScope
                        )
                    }
                )
                built = BatteryOrchestrator(
                    modules = listOf(BilateralTapTest(), gaitTest, VisionTest(), SdmtTest()),
                    sessionDao = app.database.sessionDao(),
                    testResultDao = app.database.testResultDao(),
                    deviceInfo = DeviceInfo.summary
                )
                built.start()
                built
            }
            SessionRunnerScreen(orchestrator = orchestrator, onFinished = {
                nav.popBackStack("home", inclusive = false)
            })
        }
        composable("settings") {
            SettingsScreen(
                userProfileDao = app.database.userProfileDao(),
                onEditProfile = { nav.navigate("profile") }
            )
        }
    }
}
