package com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jiahaoliuliu.mvvmvsmvi.data.FakeTaskRepository
import com.jiahaoliuliu.mvvmvsmvi.data.TaskCompletionStore
import com.jiahaoliuliu.mvvmvsmvi.data.taskCompletionDataStore
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksScreen

class MvvmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MvvmViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    val repository = FakeTaskRepository(TaskCompletionStore(applicationContext.taskCompletionDataStore))
                    MvvmViewModel(LoadTasksUseCase(repository), ToggleTaskUseCase(repository))
                }
            })
            MaterialTheme {
                TasksScreen(
                    state = model.state.collectAsStateWithLifecycle().value,
                    onRefresh = model::refresh,
                    onToggle = model::toggle,
                    onBack = onBackPressedDispatcher::onBackPressed,
                )
            }
        }
    }
}
