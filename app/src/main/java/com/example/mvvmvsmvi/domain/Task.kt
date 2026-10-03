package com.example.mvvmvsmvi.domain

data class Task(val id: String, val title: TaskTitle, val completed: Boolean = false)

enum class TaskTitle { COMPOSE, ARCHITECTURES, TESTS }

interface TaskRepository {
    suspend fun loadTasks(): List<Task>
}

class LoadTasks(private val repository: TaskRepository) {
    suspend operator fun invoke(): List<Task> = repository.loadTasks()
}

class ToggleTask {
    operator fun invoke(tasks: List<Task>, id: String): List<Task> =
        tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it }
}
