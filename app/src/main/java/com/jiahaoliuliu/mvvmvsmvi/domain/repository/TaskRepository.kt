package com.jiahaoliuliu.mvvmvsmvi.domain.repository

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task

interface TaskRepository {
    suspend fun loadTasks(): List<Task>
    suspend fun toggleTask(id: String): Task?
}
