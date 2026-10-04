package com.jiahaoliuliu.mvvmvsmvi.presentation.mvi

import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState

/** Progress and completed outcomes supplied by the ViewModel's intent processor. */
sealed interface TasksResult {
    data object Loading : TasksResult
    data class Loaded(val tasks: List<Task>) : TasksResult
    data object Failed : TasksResult
    data object Toggling : TasksResult
    data class Toggled(val updatedTask: Task?) : TasksResult
    data object ToggleFailed : TasksResult
}

/** Pure UI state transitions. Use cases perform the operations and persistence. */
class TasksReducer {
    fun reduce(state: TasksState, result: TasksResult): TasksState = when (result) {
        TasksResult.Loading -> state.copy(isLoading = true, hasError = false, hasToggleError = false)
        is TasksResult.Loaded -> TasksState(tasks = result.tasks)
        TasksResult.Failed -> state.copy(isLoading = false, hasError = true)
        TasksResult.Toggling -> state.copy(isToggling = true, hasToggleError = false)
        is TasksResult.Toggled -> applyToggledTask(state, result.updatedTask)
        TasksResult.ToggleFailed -> state.copy(isToggling = false, hasToggleError = true)
    }

    private fun applyToggledTask(state: TasksState, updatedTask: Task?): TasksState {
        if (updatedTask == null) return state.copy(isToggling = false)

        // Copy the already-toggled task into the UI snapshot; never flip completion here.
        val updatedTasks = state.tasks.map { existingTask ->
            if (existingTask.id == updatedTask.id) updatedTask else existingTask
        }
        return state.copy(isToggling = false, tasks = updatedTasks)
    }
}
