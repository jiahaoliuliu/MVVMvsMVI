package com.jiahaoliuliu.mvvmvsmvi

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksScreen
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TasksScreenLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun refreshStaysAtBottomWhileLongListScrolls() {
        render(100)
        val before = compose.onNodeWithText("Refresh").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("tasks_list").fetchSemanticsNode().boundsInRoot
        assertTrue(before.top >= viewport.bottom)
        compose.onNodeWithTag("tasks_list").performScrollToIndex(100)
        val after = compose.onNodeWithText("Refresh").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
    }

    @Test fun refreshIsBelowContentEvenWithEmptyList() {
        render(0)
        val button = compose.onNodeWithText("Refresh").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("tasks_list").fetchSemanticsNode().boundsInRoot
        assertTrue(button.top >= viewport.bottom)
    }

    @Test fun togglingDoesNotShiftTasksOrRefresh() {
        val state = mutableStateOf(TasksState(tasks = listOf(Task("compose", TaskTitle.COMPOSE))))
        compose.setContent {
            MaterialTheme { TasksScreen(state.value, {}, {}, {}) }
        }
        val taskBefore = compose.onNodeWithText("Learn Jetpack Compose").fetchSemanticsNode().boundsInRoot
        val refreshBefore = compose.onNodeWithText("Refresh").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { state.value = state.value.copy(isToggling = true) }
        compose.onNodeWithText("Learn Jetpack Compose").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Toggling task"))
        compose.onNodeWithText("Refresh").assertIsNotEnabled()
        compose.onNodeWithText("Toggling task").assertDoesNotExist()
        assertEquals(taskBefore, compose.onNodeWithText("Learn Jetpack Compose").fetchSemanticsNode().boundsInRoot)
        assertEquals(refreshBefore, compose.onNodeWithText("Refresh").fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle {
            state.value = state.value.copy(isToggling = false, tasks = state.value.tasks.map { it.copy(completed = true) })
        }
        compose.onNodeWithText("Learn Jetpack Compose").assertIsOn().assertIsEnabled()
        assertEquals(taskBefore, compose.onNodeWithText("Learn Jetpack Compose").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun loadingIsCenteredBetweenSubtitleAndRefresh() {
        compose.setContent {
            MaterialTheme {
                TasksScreen(
                    TasksState(tasks = List(100) { Task("task_$it", TaskTitle.COMPOSE) }, isLoading = true),
                    {}, {}, {},
                )
            }
        }
        val area = compose.onNodeWithTag("loading_area").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("loading_content").fetchSemanticsNode().boundsInRoot
        val subtitle = compose.onNodeWithTag("tasks_subtitle").fetchSemanticsNode().boundsInRoot
        val refresh = compose.onNodeWithText("Refresh").assertIsDisplayed().assertIsNotEnabled()
            .fetchSemanticsNode().boundsInRoot
        assertEquals(area.center.x, content.center.x, 1f)
        assertEquals(area.center.y, content.center.y, 1f)
        assertTrue(area.top >= subtitle.bottom)
        assertTrue(area.bottom <= refresh.top)
        compose.onNodeWithText("Loading tasks").assertIsDisplayed()
        compose.onNodeWithTag("tasks_list").assertDoesNotExist()
    }

    private fun render(count: Int) {
        compose.setContent {
            MaterialTheme {
                TasksScreen(
                    state = TasksState(tasks = List(count) { Task("task_$it", TaskTitle.COMPOSE) }),
                    onRefresh = {},
                    onToggle = {},
                    onBack = {},
                )
            }
        }
    }
}
