package com.example.mvvmvsmvi.presentation.tasks

import com.example.mvvmvsmvi.domain.Task

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)
