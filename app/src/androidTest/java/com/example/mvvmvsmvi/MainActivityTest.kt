package com.example.mvvmvsmvi

import android.app.Activity
import androidx.compose.ui.test.*
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.mvvmvsmvi.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import com.example.mvvmvsmvi.presentation.mvvm.MvvmActivity
import com.example.mvvmvsmvi.presentation.mvi.MviActivity

class MainActivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun opensMvvmActivity() { open("Open MVVM", MvvmActivity::class.java) }
    @Test fun opensMviActivity() { open("Open MVI", MviActivity::class.java) }

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
