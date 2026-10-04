package com.jiahaoliuliu.mvvmvsmvi.presentation.tasks

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isToggling: Boolean = false,
    val hasToggleError: Boolean = false,
)
