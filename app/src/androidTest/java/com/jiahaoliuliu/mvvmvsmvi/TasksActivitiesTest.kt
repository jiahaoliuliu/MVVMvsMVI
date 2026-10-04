package com.jiahaoliuliu.mvvmvsmvi

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.MviActivity
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm.MvvmActivity
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

/** Exercise the real activity, ViewModel, use case, and fake for each implementation. */
abstract class TasksActivityContract<A : ComponentActivity>(private val activity: Class<A>) {
    val compose = createAndroidComposeRule(activity)
    @get:Rule val rules = RuleChain.outerRule(ResetCompletionRule()).around(compose)

    private fun awaitReady() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Loading tasks").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Refresh").fetchSemanticsNodes().singleOrNull()?.let {
                    !it.config.contains(SemanticsProperties.Disabled)
                } == true
        }
    }

    @Test fun loadsAndTogglesTasks() {
        awaitReady()
        compose.onNodeWithText("Completed: 0 of 3").assertIsDisplayed()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOff().performClick()
        awaitReady()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOn()
        compose.onNodeWithText("Completed: 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("Learn Jetpack Compose").performClick()
        awaitReady()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOff()
    }

    @Test fun refreshFailureAndRetryFollowTheSameScenario() {
        awaitReady()
        compose.onNodeWithText("Refresh").performClick()
        awaitReady()
        compose.onNodeWithText("Refresh").performClick()
        awaitReady()
        compose.onNodeWithText("Could not load tasks. Please retry.").assertIsDisplayed()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        awaitReady()
        compose.onNodeWithText("Could not load tasks. Please retry.").assertDoesNotExist()
        compose.onNodeWithText("Completed: 0 of 3").assertIsDisplayed()
    }

    @Test fun reopeningActivityLoadsSavedCompletion() {
        awaitReady()
        compose.onNodeWithText("Learn Jetpack Compose").performClick()
        awaitReady()
        compose.activityRule.scenario.close()
        ActivityScenario.launch(activity).use {
            awaitReady()
            compose.onNodeWithText("Learn Jetpack Compose").assertIsOn()
            compose.onNodeWithText("Refresh").performClick()
            awaitReady()
            compose.onNodeWithText("Learn Jetpack Compose").assertIsOn()
        }
    }

    @Test fun recreationRetainsCompletionAndRepositoryLoadCount() {
        awaitReady()
        compose.onNodeWithText("Learn Jetpack Compose").performClick()
        awaitReady()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Completed: 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("Refresh").performClick()
        awaitReady()
        compose.onNodeWithText("Refresh").performClick()
        awaitReady()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
}

class MvvmActivityTest : TasksActivityContract<MvvmActivity>(MvvmActivity::class.java)
class MviActivityTest : TasksActivityContract<MviActivity>(MviActivity::class.java)
