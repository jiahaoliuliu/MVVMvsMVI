package com.example.mvvmvsmvi.data

import com.example.mvvmvsmvi.domain.Task
import com.example.mvvmvsmvi.domain.TaskRepository
import com.example.mvvmvsmvi.domain.TaskTitle
import kotlinx.coroutines.delay
import java.io.IOException

/** A fresh instance per activity; retained by its ViewModel across rotation. */
class FakeTaskRepository(private val delayMillis: Long = 600) : TaskRepository {
    private var loadCount = 0

    override suspend fun loadTasks(): List<Task> {
        delay(delayMillis)
        loadCount++
        if (loadCount % 3 == 0) throw IOException("Demonstration load failure")
        return listOf(
            Task("compose", TaskTitle.COMPOSE),
            Task("architectures", TaskTitle.ARCHITECTURES),
            Task("tests", TaskTitle.TESTS),
        )
    }
}
