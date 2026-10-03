package com.example.mvvmvsmvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.mvvmvsmvi.domain.LoadTasks
import com.example.mvvmvsmvi.domain.Task
import com.example.mvvmvsmvi.domain.TaskRepository
import com.example.mvvmvsmvi.domain.TaskTitle
import com.example.mvvmvsmvi.presentation.mvi.MviViewModel
import com.example.mvvmvsmvi.presentation.mvi.TasksIntent
import com.example.mvvmvsmvi.presentation.mvvm.MvvmViewModel
import com.example.mvvmvsmvi.presentation.tasks.TasksState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Run the same behavior contract against both architectures. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class TasksViewModelContract {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    protected abstract fun create(repository: TaskRepository): Harness

    protected fun <T : ViewModel> retain(model: T, type: Class<T>): T =
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = modelClass.cast(model)!!
        })[type]

    @After fun clearModels() { store.clear() }

    @Test fun initialLoadShowsLoadingThenTasks() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        assertTrue(model.state.value.isLoading)
        advanceUntilIdle()
        assertEquals(repository.tasks, model.state.value.tasks)
        assertFalse(model.state.value.isLoading)
        assertFalse(model.state.value.hasError)
    }

    @Test fun toggleTwiceAndUnknownIdPreserveOtherTasks() = runTest {
        val model = create(ControlledRepository())
        advanceUntilIdle()
        val original = model.state.value.tasks
        model.toggle("one")
        advanceUntilIdle()
        assertTrue(model.state.value.tasks.first().completed)
        assertEquals(original.last(), model.state.value.tasks.last())
        model.toggle("one")
        model.toggle("missing")
        advanceUntilIdle()
        assertEquals(original, model.state.value.tasks)
    }

    @Test fun refreshFailureRetainsTasksAndRetryRecovers() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        advanceUntilIdle()
        model.toggle("one")
        advanceUntilIdle()
        val completed = model.state.value.tasks
        repository.fail = true
        model.refresh()
        assertTrue(model.state.value.isLoading)
        advanceUntilIdle()
        assertTrue(model.state.value.hasError)
        assertEquals(completed, model.state.value.tasks)
        repository.fail = false
        model.refresh()
        assertFalse(model.state.value.hasError)
        advanceUntilIdle()
        assertEquals(TasksState(tasks = repository.tasks), model.state.value)
    }

    @Test fun initialFailureCanBeRetried() = runTest {
        val repository = ControlledRepository().apply { fail = true }
        val model = create(repository)
        advanceUntilIdle()
        assertTrue(model.state.value.hasError)
        assertTrue(model.state.value.tasks.isEmpty())
        repository.fail = false
        model.refresh()
        advanceUntilIdle()
        assertEquals(repository.tasks, model.state.value.tasks)
        assertFalse(model.state.value.hasError)
    }

    @Test fun repeatedInputDuringLoadingDoesNotStartMoreRequestsOrToggle() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        model.refresh()
        model.refresh()
        model.toggle("one")
        advanceUntilIdle()
        assertEquals(1, repository.calls)
        assertFalse(model.state.value.tasks.first().completed)
    }

    @Test fun clearingViewModelCancelsLoadWithoutShowingAnError() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        main.dispatcher.scheduler.runCurrent()
        store.clear()
        advanceUntilIdle()
        assertTrue(repository.cancelled)
        assertFalse(model.state.value.hasError)
        assertTrue(model.state.value.tasks.isEmpty())
    }

    protected class Harness(val state: StateFlow<TasksState>, val refresh: () -> Unit, val toggle: (String) -> Unit)

    private class ControlledRepository : TaskRepository {
        val tasks = listOf(Task("one", TaskTitle.COMPOSE), Task("two", TaskTitle.TESTS))
        var calls = 0
        var fail = false
        var cancelled = false
        override suspend fun loadTasks(): List<Task> {
            calls++
            try { delay(100) } catch (error: CancellationException) {
                cancelled = true
                throw error
            }
            if (fail) throw IOException("offline")
            return tasks
        }
    }
}

class MvvmViewModelTest : TasksViewModelContract() {
    override fun create(repository: TaskRepository): Harness {
        val model = retain(MvvmViewModel(LoadTasks(repository)), MvvmViewModel::class.java)
        return Harness(model.state, model::refresh, model::toggle)
    }
}

class MviViewModelTest : TasksViewModelContract() {
    override fun create(repository: TaskRepository): Harness {
        val model = retain(MviViewModel(LoadTasks(repository)), MviViewModel::class.java)
        return Harness(model.state, { model.accept(TasksIntent.Refresh) }, { model.accept(TasksIntent.Toggle(it)) })
    }
}
