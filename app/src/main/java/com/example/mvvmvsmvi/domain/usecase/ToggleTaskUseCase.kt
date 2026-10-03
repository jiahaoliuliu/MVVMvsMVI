package com.example.mvvmvsmvi.domain.usecase

import com.example.mvvmvsmvi.domain.entity.Task

class ToggleTaskUseCase {
    operator fun invoke(tasks: List<Task>, id: String): List<Task> =
        tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it }
}
