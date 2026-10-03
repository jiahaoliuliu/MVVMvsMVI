package com.example.mvvmvsmvi

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.mvvmvsmvi.presentation.mvi.MviActivity
import com.example.mvvmvsmvi.presentation.mvvm.MvvmActivity
import org.junit.Rule
import org.junit.Test

/** Exercise the real activity, ViewModel, use case, and fake for each implementation. */
abstract class TasksActivityContract<A : ComponentActivity>(activity: Class<A>) {
    @get:Rule val compose = createAndroidComposeRule(activity)

    private fun awaitReady() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Loading tasks").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test fun loadsAndTogglesTasks() {
        awaitReady()
        compose.onNodeWithText("Completed: 0 of 3").assertIsDisplayed()
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Completed: 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("Learn Jetpack Compose").performClick().assertIsOff()
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

    @Test fun recreationRetainsCompletionAndRepositoryLoadCount() {
        awaitReady()
        compose.onNodeWithText("Learn Jetpack Compose").performClick()
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
