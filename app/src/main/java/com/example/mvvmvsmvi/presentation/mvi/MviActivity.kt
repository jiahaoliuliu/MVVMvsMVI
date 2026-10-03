package com.example.mvvmvsmvi.presentation.mvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mvvmvsmvi.data.FakeTaskRepository
import com.example.mvvmvsmvi.domain.LoadTasks
import com.example.mvvmvsmvi.presentation.tasks.TasksScreen

class MviActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MviViewModel = viewModel(factory = viewModelFactory {
                initializer { MviViewModel(LoadTasks(FakeTaskRepository())) }
            })
            MaterialTheme {
                TasksScreen(
                    state = model.state.collectAsStateWithLifecycle().value,
                    onRefresh = { model.accept(TasksIntent.Refresh) },
                    onToggle = { model.accept(TasksIntent.Toggle(it)) },
                    onBack = onBackPressedDispatcher::onBackPressed,
                )
            }
        }
    }
}
