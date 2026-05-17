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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mustafanazeer.baselinems.BaselineMSApp
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.battery.gait.GaitTest
import com.mustafanazeer.baselinems.battery.gait.GaitTestViewModel
import com.mustafanazeer.baselinems.battery.sdmt.SdmtTest
import com.mustafanazeer.baselinems.battery.tap.BilateralTapTest
import com.mustafanazeer.baselinems.battery.vision.VisionTest
import com.mustafanazeer.baselinems.battery.voice.VoiceTestModule
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.data.TrendsRepository
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.signals.RawSensorWriter
import com.mustafanazeer.baselinems.ui.home.HomeScreen
import com.mustafanazeer.baselinems.ui.home.SessionRunnerScreen
import com.mustafanazeer.baselinems.ui.onboarding.DisclaimerScreen
import com.mustafanazeer.baselinems.ui.onboarding.ProfileSetupScreen
import com.mustafanazeer.baselinems.ui.reports.AboutCitationsScreen
import com.mustafanazeer.baselinems.ui.reports.ReportsRoute
import com.mustafanazeer.baselinems.ui.reports.TestDetailRoute
import com.mustafanazeer.baselinems.ui.settings.SettingsScreen
import com.mustafanazeer.baselinems.util.DeviceInfo
import java.io.File

private const val PREFS = "baselinems_prefs"
private const val KEY_DISCLAIMER = "disclaimer_acknowledged"

object Routes {
    const val Disclaimer = "disclaimer"
    const val Profile = "profile"
    const val Home = "home"
    const val Session = "session"
    const val Settings = "settings"
    const val Reports = "reports"
    const val AboutCitations = "reports/about"
    const val TestDetailPrefix = "reports/detail"
    const val TestDetailArg = "testType"
    const val TestDetailPattern = "$TestDetailPrefix/{$TestDetailArg}"
    fun testDetail(testType: TestType): String = "$TestDetailPrefix/${testType.name}"
}

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
        !disclaimerAcknowledged -> Routes.Disclaimer
        !hasProfile -> Routes.Profile
        else -> Routes.Home
    }

    val trendsRepository = remember(app) {
        TrendsRepository(
            sessionDao = app.database.sessionDao(),
            testResultDao = app.database.testResultDao(),
            steadinessSteadyLabel = context.getString(R.string.phase9_voice_steadiness_steady),
            steadinessMostlySteadyLabel = context.getString(R.string.phase9_voice_steadiness_mostly_steady),
            steadinessVariedLabel = context.getString(R.string.phase9_voice_steadiness_varied),
            steadinessUnmeasurableLabel = context.getString(R.string.phase9_voice_steadiness_unmeasurable),
            tapDisplayName = context.getString(R.string.phase9_reports_card_tap),
            gaitDisplayName = context.getString(R.string.phase9_reports_card_gait),
            visionDisplayName = context.getString(R.string.phase9_reports_card_vision),
            sdmtDisplayName = context.getString(R.string.phase9_reports_card_sdmt),
            voiceDisplayName = context.getString(R.string.phase9_reports_card_voice)
        )
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.Disclaimer) {
            DisclaimerScreen(onAcknowledge = {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_DISCLAIMER, true).apply()
                disclaimerAcknowledged = true
                nav.navigate(if (hasProfile) Routes.Home else Routes.Profile) {
                    popUpTo(Routes.Disclaimer) { inclusive = true }
                }
            })
        }
        composable(Routes.Profile) {
            ProfileSetupScreen(
                userProfileDao = app.database.userProfileDao(),
                onComplete = {
                    hasProfile = true
                    nav.navigate(Routes.Home) { popUpTo(Routes.Profile) { inclusive = true } }
                }
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                sessionDao = app.database.sessionDao(),
                onStartSession = { nav.navigate(Routes.Session) },
                onOpenSettings = { nav.navigate(Routes.Settings) },
                onViewTrends = { nav.navigate(Routes.Reports) }
            )
        }
        composable(Routes.Session) {
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
                    modules = listOf(
                        BilateralTapTest(),
                        gaitTest,
                        VisionTest(),
                        SdmtTest(),
                        VoiceTestModule()
                    ),
                    sessionDao = app.database.sessionDao(),
                    testResultDao = app.database.testResultDao(),
                    deviceInfo = DeviceInfo.summary
                )
                built.start()
                built
            }
            SessionRunnerScreen(orchestrator = orchestrator, onFinished = {
                nav.popBackStack(Routes.Home, inclusive = false)
            })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                userProfileDao = app.database.userProfileDao(),
                onEditProfile = { nav.navigate(Routes.Profile) }
            )
        }
        composable(Routes.Reports) {
            ReportsRoute(
                repository = trendsRepository,
                onBack = { nav.popBackStack() },
                onCardSelected = { testType ->
                    nav.navigate(Routes.testDetail(testType))
                },
                onRunFirstCheckIn = {
                    nav.navigate(Routes.Session) {
                        popUpTo(Routes.Home) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = Routes.TestDetailPattern,
            arguments = listOf(navArgument(Routes.TestDetailArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString(Routes.TestDetailArg)
            val testType = raw?.let { runCatching { TestType.valueOf(it) }.getOrNull() }
            if (testType == null) {
                nav.popBackStack()
            } else {
                TestDetailRoute(
                    repository = trendsRepository,
                    testType = testType,
                    onBack = { nav.popBackStack() },
                    onAbout = { nav.navigate(Routes.AboutCitations) }
                )
            }
        }
        composable(Routes.AboutCitations) {
            AboutCitationsScreen(onBack = { nav.popBackStack() })
        }
    }
}
