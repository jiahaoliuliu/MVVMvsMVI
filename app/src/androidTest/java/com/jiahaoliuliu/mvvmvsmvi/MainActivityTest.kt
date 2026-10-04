package com.jiahaoliuliu.mvvmvsmvi

import android.app.Activity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jiahaoliuliu.mvvmvsmvi.presentation.MainActivity
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm.MvvmActivity
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.MviActivity

class MainActivityTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules = RuleChain.outerRule(ResetCompletionRule()).around(compose)

    @Test fun opensMvvmActivity() { open("Open MVVM", MvvmActivity::class.java) }
    @Test fun opensMviActivity() { open("Open MVI", MviActivity::class.java) }

    @Test fun completionIsSharedBetweenArchitectures() {
        compose.onNodeWithText("Open MVVM").performScrollTo().performClick()
        awaitTasks()
        compose.onNodeWithText("Learn Jetpack Compose").performClick()
        awaitTasks()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOn()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Open MVI").performScrollTo().performClick()
        awaitTasks()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOn().performClick()
        awaitTasks()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOff()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Open MVVM").performScrollTo().performClick()
        awaitTasks()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOff()
    }

    private fun awaitTasks() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Learn Jetpack Compose").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("Loading tasks").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Refresh").fetchSemanticsNodes().singleOrNull()?.let {
                    !it.config.contains(SemanticsProperties.Disabled)
                } == true
        }
    }

    private fun open(label: String, expectedActivity: Class<out Activity>) {
        compose.onNodeWithText(compose.activity.getString(R.string.mvvm_explanation)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.mvi_explanation)).assertIsDisplayed()
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("My tasks").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("My tasks").assertIsDisplayed()
        compose.runOnIdle {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            assertTrue(resumed.any { expectedActivity.isInstance(it) })
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Choose an architecture").assertIsDisplayed()
        compose.runOnIdle {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            assertTrue(resumed.any { it is MainActivity })
        }
    }
}
