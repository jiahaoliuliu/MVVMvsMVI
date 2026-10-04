package com.jiahaoliuliu.mvvmvsmvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.ToggleTaskUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.usecase.LoadTasksUseCase
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.repository.TaskRepository
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.MviViewModel
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.TasksIntent
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm.MvvmViewModel
import com.jiahaoliuliu.mvvmvsmvi.presentation.tasks.TasksState
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
import kotlin.time.Duration.Companion.milliseconds

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
        advanceUntilIdle()
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

    @Test fun failedToggleKeepsCompletionAndRetryPersists() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        advanceUntilIdle()
        repository.failToggle = true
        model.toggle("one")
        assertTrue(model.state.value.isToggling)
        assertFalse(model.state.value.tasks.first().completed)
        advanceUntilIdle()
        assertTrue(model.state.value.hasToggleError)
        assertFalse(model.state.value.isToggling)
        assertFalse(model.state.value.tasks.first().completed)
        repository.failToggle = false
        model.toggle("one")
        advanceUntilIdle()
        assertTrue(model.state.value.tasks.first().completed)
        assertFalse(model.state.value.hasToggleError)
        model.refresh()
        advanceUntilIdle()
        assertTrue(model.state.value.tasks.first().completed)
    }

    @Test fun repeatedInputDuringToggleIsIgnored() = runTest {
        val repository = ControlledRepository()
        val model = create(repository)
        advanceUntilIdle()
        model.toggle("one")
        model.toggle("one")
        model.refresh()
        advanceUntilIdle()
        assertEquals(1, repository.toggles)
        assertEquals(1, repository.calls)
        assertTrue(model.state.value.tasks.first().completed)
    }

    protected class Harness(val state: StateFlow<TasksState>, val refresh: () -> Unit, val toggle: (String) -> Unit)

    private class ControlledRepository : TaskRepository {
        var tasks = listOf(Task("one", TaskTitle.COMPOSE), Task("two", TaskTitle.TESTS))
        var failToggle = false
        var toggles = 0
        var calls = 0
        var fail = false
        var cancelled = false
        override suspend fun toggleTask(id: String): Task? {
            toggles++
            delay(100.milliseconds)
            if (failToggle) throw IOException("write failed")
            val task = tasks.find { it.id == id } ?: return null
            val toggled = task.copy(completed = !task.completed)
            tasks = tasks.map { if (it.id == id) toggled else it }
            return toggled
        }
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
        val model = retain(MvvmViewModel(LoadTasksUseCase(repository), ToggleTaskUseCase(repository)), MvvmViewModel::class.java)
        return Harness(model.state, model::refresh, model::toggle)
    }
}

class MviViewModelTest : TasksViewModelContract() {
    override fun create(repository: TaskRepository): Harness {
        val model = retain(MviViewModel(LoadTasksUseCase(repository), ToggleTaskUseCase(repository)), MviViewModel::class.java)
        return Harness(model.state, { model.accept(TasksIntent.Refresh) }, { model.accept(TasksIntent.Toggle(it)) })
    }
}
