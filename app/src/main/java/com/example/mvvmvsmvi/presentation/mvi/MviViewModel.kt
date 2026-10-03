package com.example.mvvmvsmvi.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.example.mvvmvsmvi.domain.entity.Task
import com.example.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.example.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TasksIntent {
    data object Refresh : TasksIntent
    data class Toggle(val id: String) : TasksIntent
}

sealed interface TasksResult {
    data object Loading : TasksResult
    data class Loaded(val tasks: List<Task>) : TasksResult
    data object Failed : TasksResult
    data class Toggled(val id: String) : TasksResult
}

/** Pure reducer: no coroutines, repositories, or Android dependencies. */
fun reduce(state: TasksState, result: TasksResult): TasksState = when (result) {
    TasksResult.Loading -> state.copy(isLoading = true, hasError = false)
    is TasksResult.Loaded -> TasksState(tasks = result.tasks)
    TasksResult.Failed -> state.copy(isLoading = false, hasError = true)
    is TasksResult.Toggled -> state.copy(tasks = ToggleTaskUseCase()(state.tasks, result.id))
}

class MviViewModel(private val loadTasks: LoadTasksUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()
    private val intents = Channel<TasksIntent>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (intent in intents) {
                when (intent) {
                    TasksIntent.Refresh -> load()
                    is TasksIntent.Toggle -> mutableState.update { reduce(it, TasksResult.Toggled(intent.id)) }
                }
            }
        }
        accept(TasksIntent.Refresh)
    }

    fun accept(intent: TasksIntent) {
        // Match MVVM: ignore input during an active load; do not queue stale clicks.
        if (state.value.isLoading) return
        if (intent == TasksIntent.Refresh) {
            mutableState.update { reduce(it, TasksResult.Loading) }
        }
        check(intents.trySend(intent).isSuccess) { "Intent buffer is full" }
    }

    private suspend fun load() {
        try {
            val tasks = loadTasks()
            mutableState.update { reduce(it, TasksResult.Loaded(tasks)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { reduce(it, TasksResult.Failed) }
        }
    }

    override fun onCleared() { intents.close() }
}
