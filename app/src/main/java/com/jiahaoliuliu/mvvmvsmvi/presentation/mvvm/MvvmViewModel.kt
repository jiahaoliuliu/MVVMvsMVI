package com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MvvmViewModel(
    private val loadTaskUseCase: LoadTasksUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.isLoading || state.value.isToggling) return
        mutableState.update { it.copy(isLoading = true, hasError = false, hasToggleError = false) }
        viewModelScope.launch {
            try {
                val tasks = loadTaskUseCase()
                mutableState.value = TasksState(tasks = tasks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun toggle(id: String) {
        if (state.value.isLoading || state.value.isToggling) return
        mutableState.update { it.copy(isToggling = true, hasToggleError = false) }
        viewModelScope.launch {
            try {
                val toggled = toggleTaskUseCase(id)
                mutableState.update { state ->
                    state.copy(
                        isToggling = false,
                        tasks = state.tasks.map { if (toggled != null && it.id == toggled.id) toggled else it })
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isToggling = false, hasToggleError = true) }
            }
        }
    }
}
