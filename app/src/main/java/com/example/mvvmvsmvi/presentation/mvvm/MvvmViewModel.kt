package com.example.mvvmvsmvi.presentation.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmvsmvi.domain.LoadTasks
import com.example.mvvmvsmvi.domain.ToggleTask
import com.example.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MvvmViewModel(
    private val loadTasks: LoadTasks,
    private val toggleTask: ToggleTask = ToggleTask(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (state.value.isLoading) return
        mutableState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            try {
                val tasks = loadTasks()
                mutableState.value = TasksState(tasks = tasks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun toggle(id: String) {
        if (state.value.isLoading) return
        mutableState.update { it.copy(tasks = toggleTask(it.tasks, id)) }
    }
}
