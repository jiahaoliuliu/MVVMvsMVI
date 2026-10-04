package com.jiahaoliuliu.mvvmvsmvi

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.*
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DomainAndRepositoryTest {
    @Test fun useCasesDelegateToRepository() = runTest {
        val expected = Task("a", TaskTitle.TESTS)
        var requestedId: String? = null
        val repository = object : TaskRepository {
            override suspend fun loadTasks(): List<Task> = listOf(expected)
            override suspend fun toggleTask(id: String): Task? {
                requestedId = id
                return expected.copy(completed = true)
            }
        }
        assertEquals(listOf(expected), LoadTasksUseCase(repository)())
        assertEquals(expected.copy(completed = true), ToggleTaskUseCase(repository)("a"))
        assertEquals("a", requestedId)
    }

    @Test fun reducerCoversLoadingSuccessAndFailureWithoutMutatingInput() {
        val tasks = listOf(Task("a", TaskTitle.TESTS))
        val original = TasksState(tasks = tasks, hasError = true)
        val loading = reduce(original, TasksResult.Loading)
        assertEquals(TasksState(tasks, isLoading = true), loading)
        assertTrue(original.hasError)
        assertEquals(TasksState(tasks, hasError = true), reduce(loading, TasksResult.Failed))
        assertEquals(TasksState(tasks), reduce(loading, TasksResult.Loaded(tasks)))
    }

    @Test fun reducerAppliesConfirmedSaveAndPreservesTasksOnWriteFailure() {
        val task = Task("a", TaskTitle.TESTS)
        val original = TasksState(tasks = listOf(task), hasSaveError = true)
        val saving = reduce(original, TasksResult.Saving)
        assertTrue(saving.isSaving)
        assertFalse(saving.hasSaveError)
        val saved = reduce(saving, TasksResult.Saved(task.copy(completed = true)))
        assertTrue(saved.tasks.first().completed)
        assertFalse(saved.isSaving)
        assertFalse(original.tasks.first().completed)
        val failed = reduce(saving, TasksResult.SaveFailed)
        assertTrue(failed.hasSaveError)
        assertFalse(failed.isSaving)
        assertEquals(original.tasks, failed.tasks)
        assertEquals(original.tasks, reduce(saving, TasksResult.Saved(null)).tasks)
    }
}
