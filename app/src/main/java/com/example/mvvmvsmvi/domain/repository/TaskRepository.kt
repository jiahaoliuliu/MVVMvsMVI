package com.example.mvvmvsmvi.domain.repository

import com.example.mvvmvsmvi.domain.entity.Task

interface TaskRepository {
    suspend fun loadTasks(): List<Task>
}
