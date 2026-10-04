package com.jiahaoliuliu.mvvmvsmvi

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
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

}
