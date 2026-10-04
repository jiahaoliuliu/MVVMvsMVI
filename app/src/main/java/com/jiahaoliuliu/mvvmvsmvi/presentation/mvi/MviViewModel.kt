package com.jiahaoliuliu.mvvmvsmvi.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
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

sealed interface TasksResult {
    data object Loading : TasksResult
    data class Loaded(val tasks: List<Task>) : TasksResult
    data object Failed : TasksResult
    data object Saving : TasksResult
    data class Saved(val task: Task?) : TasksResult
    data object SaveFailed : TasksResult
}

/** Pure reducer: persistence happens in the processor, never here. */
fun reduce(state: TasksState, result: TasksResult): TasksState = when (result) {
    TasksResult.Loading -> state.copy(isLoading = true, hasError = false, hasSaveError = false)
    is TasksResult.Loaded -> TasksState(tasks = result.tasks)
    TasksResult.Failed -> state.copy(isLoading = false, hasError = true)
    TasksResult.Saving -> state.copy(isSaving = true, hasSaveError = false)
    is TasksResult.Saved -> state.copy(
        isSaving = false,
        tasks = state.tasks.map { if (result.task != null && it.id == result.task.id) result.task else it },
    )
    TasksResult.SaveFailed -> state.copy(isSaving = false, hasSaveError = true)
}

class MviViewModel(
    private val loadTasks: LoadTasksUseCase,
    private val toggleTask: ToggleTaskUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()
    private val intents = Channel<TasksIntent>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (intent in intents) {
                when (intent) {
                    TasksIntent.Refresh -> load()
                    is TasksIntent.Toggle -> save(intent.id)
                }
            }
        }
        accept(TasksIntent.Refresh)
    }

    fun accept(intent: TasksIntent) {
        if (state.value.isLoading || state.value.isSaving) return
        val result = when (intent) {
            TasksIntent.Refresh -> TasksResult.Loading
            is TasksIntent.Toggle -> TasksResult.Saving
        }
        mutableState.update { reduce(it, result) }
        check(intents.trySend(intent).isSuccess) { "Intent processor is closed" }
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

    private suspend fun save(id: String) {
        try {
            val task = toggleTask(id)
            mutableState.update { reduce(it, TasksResult.Saved(task)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update { reduce(it, TasksResult.SaveFailed) }
        }
    }

    override fun onCleared() { intents.close() }
}
