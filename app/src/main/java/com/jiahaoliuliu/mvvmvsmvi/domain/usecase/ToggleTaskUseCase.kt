package com.jiahaoliuliu.mvvmvsmvi.domain.usecase

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository

class ToggleTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(id: String): Task? = repository.toggleTask(id)
}
