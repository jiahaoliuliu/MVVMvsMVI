package com.jiahaoliuliu.mvvmvsmvi.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
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

class MviViewModel(
    private val loadTasksUseCase: LoadTasksUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val reducer: TasksReducer = TasksReducer(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()
    private val intents = Channel<TasksIntent>(Channel.UNLIMITED)

    init {
        startIntentProcessing()
        // Starts the full cycle by loading the content
        accept(TasksIntent.Refresh)
    }

    private fun startIntentProcessing() {
        viewModelScope.launch {
            for (intent in intents) {
                when (intent) {
                    TasksIntent.Refresh -> load()
                    is TasksIntent.Toggle -> toggle(intent.id)
                }
            }
        }
    }

    fun accept(intent: TasksIntent) {
        if (state.value.isLoading || state.value.isToggling) return
        val result = when (intent) {
            TasksIntent.Refresh -> TasksResult.Loading
            is TasksIntent.Toggle -> TasksResult.Toggling
        }
        mutableState.update { reducer.reduce(it, result) }
        check(intents.trySend(intent).isSuccess) { "Intent processor is closed" }
    }

    private suspend fun load() {
        try {
            val tasks = loadTasksUseCase()
            mutableState.update { reducer.reduce(it, TasksResult.Loaded(tasks)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { reducer.reduce(it, TasksResult.Failed) }
        }
    }

    private suspend fun toggle(id: String) {
        try {
            val updatedTask = toggleTaskUseCase(id)
            mutableState.update { reducer.reduce(it, TasksResult.Toggled(updatedTask)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { reducer.reduce(it, TasksResult.ToggleFailed) }
        }
    }

    override fun onCleared() {
        intents.close()
    }
}
