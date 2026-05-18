package com.mustafanazeer.baselinems.ui.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.data.SessionDao
import com.mustafanazeer.baselinems.data.SessionEntity
import com.mustafanazeer.baselinems.ui.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class EmptySessionDao : SessionDao {
    override suspend fun insert(session: SessionEntity) {}
    override suspend fun update(session: SessionEntity) {}
    override suspend fun delete(session: SessionEntity) {}
    override fun observeAll(): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun getById(id: String): SessionEntity? = null
    override fun observeCompletedSessionCount(): Flow<Int> = flowOf(0)
    override suspend fun reclaimStrandedSessions(
        nowEpochMs: Long,
        strandedBeforeEpochMs: Long
    ): Int = 0
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HomeScreenViewTrendsTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun viewTrendsOutlinedButtonRenders() {
        composeRule.setContent {
            HomeScreen(
                sessionDao = EmptySessionDao(),
                onStartSession = {},
                onOpenSettings = {},
                onViewTrends = {}
            )
        }
        composeRule.onNodeWithText("View trends").assertIsDisplayed()
    }

    @Test
    fun tappingViewTrendsFiresCallback() {
        var fired = false
        composeRule.setContent {
            HomeScreen(
                sessionDao = EmptySessionDao(),
                onStartSession = {},
                onOpenSettings = {},
                onViewTrends = { fired = true }
            )
        }
        composeRule.onNodeWithText("View trends").performClick()
        assertTrue(fired)
    }

    @Test
    fun tappingViewTrendsTransitionsNavStateToReportsRoute() {
        var currentRoute: String? = null
        composeRule.setContent {
            val nav = rememberNavController()
            LaunchedEffect(nav) {
                nav.currentBackStackEntryFlow.collect { entry ->
                    currentRoute = entry.destination.route
                }
            }
            NavHost(navController = nav, startDestination = Routes.Home) {
                composable(Routes.Home) {
                    HomeScreen(
                        sessionDao = EmptySessionDao(),
                        onStartSession = {},
                        onOpenSettings = {},
                        onViewTrends = { nav.navigate(Routes.Reports) }
                    )
                }
                composable(Routes.Reports) {
                    androidx.compose.material3.Text("Reports stub")
                }
            }
        }
        composeRule.onNodeWithText("View trends").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { currentRoute == Routes.Reports }
        assertEquals(Routes.Reports, currentRoute)
    }
}
