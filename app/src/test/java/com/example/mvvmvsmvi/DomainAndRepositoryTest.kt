package com.example.mvvmvsmvi

import com.example.mvvmvsmvi.data.FakeTaskRepository
import com.example.mvvmvsmvi.domain.*
import com.example.mvvmvsmvi.presentation.mvi.*
import com.example.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DomainAndRepositoryTest {
    @Test fun loadUseCaseDelegatesToRepository() = runTest {
        val expected = listOf(Task("a", TaskTitle.TESTS))
        var calls = 0
        val repository = object : TaskRepository {
            override suspend fun loadTasks(): List<Task> { calls++; return expected }
        }
        assertEquals(expected, LoadTasks(repository)())
        assertEquals(1, calls)
    }

    @Test fun toggleIsImmutableAndUnknownIdIsHarmless() {
        val original = listOf(Task("a", TaskTitle.TESTS), Task("b", TaskTitle.COMPOSE))
        val updated = ToggleTask()(original, "a")
        assertFalse(original.first().completed)
        assertTrue(updated.first().completed)
        assertEquals(original.last(), updated.last())
        assertEquals(original, ToggleTask()(original, "missing"))
        assertTrue(ToggleTask()(emptyList(), "missing").isEmpty())
    }

    @Test fun fakeFailsEveryThirdLoadAndRecoversWithFreshData() = runTest {
        val repository = FakeTaskRepository(delayMillis = 0)
        repeat(2) { assertEquals(3, repository.loadTasks().size) }
        try {
            repository.loadTasks()
            fail("Third load should fail")
        } catch (_: IOException) { /* expected */ }
        assertTrue(repository.loadTasks().none { it.completed })
        assertEquals(3, repository.loadTasks().map { it.id }.distinct().size)
    }

    @Test fun eachFakeInstanceStartsWithTheSameScenario() = runTest {
        val first = FakeTaskRepository(0)
        first.loadTasks()
        first.loadTasks()
        assertEquals(3, FakeTaskRepository(0).loadTasks().size)
    }

    @Test fun reducerCoversLoadingSuccessFailureAndToggleWithoutMutatingInput() {
        val tasks = listOf(Task("a", TaskTitle.TESTS))
        val original = TasksState(tasks = tasks, hasError = true)
        val loading = reduce(original, TasksResult.Loading)
        assertEquals(TasksState(tasks, isLoading = true), loading)
        assertTrue(original.hasError)
        assertEquals(TasksState(tasks, hasError = true), reduce(loading, TasksResult.Failed))
        assertEquals(TasksState(tasks), reduce(loading, TasksResult.Loaded(tasks)))
        val toggled = reduce(TasksState(tasks), TasksResult.Toggled("a"))
        assertTrue(toggled.tasks.first().completed)
        assertFalse(tasks.first().completed)
    }
}
