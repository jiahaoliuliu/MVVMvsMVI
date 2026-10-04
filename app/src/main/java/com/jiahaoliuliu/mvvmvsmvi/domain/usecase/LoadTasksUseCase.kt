package com.jiahaoliuliu.mvvmvsmvi.domain.usecase

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository

class LoadTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): List<Task> = repository.loadTasks()
}
