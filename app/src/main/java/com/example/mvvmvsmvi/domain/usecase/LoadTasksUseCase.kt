package com.example.mvvmvsmvi.domain.usecase

import com.example.mvvmvsmvi.domain.entity.Task
import com.example.mvvmvsmvi.domain.repository.TaskRepository

class LoadTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): List<Task> = repository.loadTasks()
}
