package com.jiahaoliuliu.mvvmvsmvi.data

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository
import kotlinx.coroutines.delay
import java.io.IOException

/** Fake task catalog with real DataStore persistence for completion. */
class FakeTaskRepository(
    private val completionStore: TaskCompletionStore,
    private val delayMillis: Long = 600,
) : TaskRepository {
    private var loadCount = 0
    private val tasks = listOf(
        Task("compose", TaskTitle.COMPOSE),
        Task("architectures", TaskTitle.ARCHITECTURES),
        Task("tests", TaskTitle.TESTS),
    )

    override suspend fun loadTasks(): List<Task> {
        delay(delayMillis)
        loadCount++
        if (loadCount % 3 == 0) throw IOException("Demonstration load failure")
        val completed = completionStore.completedTaskIds()
        return tasks.map { it.copy(completed = it.id in completed) }
    }

    override suspend fun toggleTask(id: String): Task? {
        val task = tasks.find { it.id == id } ?: return null
        return task.copy(completed = completionStore.toggle(id))
    }
}
