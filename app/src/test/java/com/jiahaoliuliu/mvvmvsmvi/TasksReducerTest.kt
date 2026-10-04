package com.jiahaoliuliu.mvvmvsmvi

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.TasksReducer
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.TasksResult
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
import org.junit.Assert.*
import org.junit.Test

class TasksReducerTest {
    private val reducer = TasksReducer()

    @Test fun reducerCoversLoadingSuccessAndFailureWithoutMutatingInput() {
        val tasks = listOf(Task("a", TaskTitle.TESTS))
        val original = TasksState(tasks = tasks, hasError = true)
        val loading = reducer.reduce(original, TasksResult.Loading)
        assertEquals(TasksState(tasks, isLoading = true), loading)
        assertTrue(original.hasError)
        assertEquals(TasksState(tasks, hasError = true), reducer.reduce(loading, TasksResult.Failed))
        assertEquals(TasksState(tasks), reducer.reduce(loading, TasksResult.Loaded(tasks)))
    }

    @Test fun reducerAppliesConfirmedToggleAndPreservesTasksOnWriteFailure() {
        val task = Task("a", TaskTitle.TESTS)
        val original = TasksState(tasks = listOf(task), hasToggleError = true)
        val toggling = reducer.reduce(original, TasksResult.Toggling)
        assertTrue(toggling.isToggling)
        assertFalse(toggling.hasToggleError)
        val toggled = reducer.reduce(toggling, TasksResult.Toggled(task.copy(completed = true)))
        assertTrue(toggled.tasks.first().completed)
        assertFalse(toggled.isToggling)
        assertFalse(original.tasks.first().completed)
        val failed = reducer.reduce(toggling, TasksResult.ToggleFailed)
        assertTrue(failed.hasToggleError)
        assertFalse(failed.isToggling)
        assertEquals(original.tasks, failed.tasks)
        assertEquals(original.tasks, reducer.reduce(toggling, TasksResult.Toggled(null)).tasks)
    }
    @Test fun applyingCompletedToggleTwiceDoesNotToggleAgainOrChangeOtherTasks() {
        val first = Task("a", TaskTitle.COMPOSE)
        val second = Task("b", TaskTitle.TESTS)
        val original = TasksState(tasks = listOf(first, second), isToggling = true)
        val result = TasksResult.Toggled(first.copy(completed = true))
        val once = reducer.reduce(original, result)
        val twice = reducer.reduce(once, result)
        assertEquals(once, twice)
        assertTrue(twice.tasks.first().completed)
        assertEquals(second, twice.tasks.last())
        assertFalse(original.tasks.first().completed)
        assertFalse(twice.isToggling)
    }
}
