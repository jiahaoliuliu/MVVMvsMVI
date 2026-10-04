package com.jiahaoliuliu.mvvmvsmvi

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jiahaoliuliu.mvvmvsmvi.data.FakeTaskRepository
import com.jiahaoliuliu.mvvmvsmvi.data.TaskCompletionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class TaskCompletionStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun store(scope: CoroutineScope, name: String = "tasks"): TaskCompletionStore =
        TaskCompletionStore(PreferenceDataStoreFactory.create(scope = scope) {
            temporary.root.resolve("$name.preferences_pb")
        })

    @Test fun togglePersistsAcrossRefreshAndRepositoryInstances() = runTest {
        val storage = store(backgroundScope)
        val first = FakeTaskRepository(storage, 0)
        val original = first.loadTasks()
        assertTrue(first.toggleTask("compose")!!.completed)
        assertFalse(original.first().completed)
        assertTrue(first.loadTasks().first().completed)
        val second = FakeTaskRepository(storage, 0)
        assertTrue(second.loadTasks().first().completed)
        assertFalse(second.toggleTask("compose")!!.completed)
        assertFalse(second.loadTasks().first().completed)
    }

    @Test fun savedChoiceSurvivesClosingAndReopeningDataStore() = runTest {
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            assertTrue(store(firstScope).toggle("compose"))
        } finally {
            firstScope.cancel()
            firstScope.coroutineContext.job.join()
        }
        val second = store(backgroundScope)
        assertEquals(setOf("compose"), second.completedTaskIds())
        assertFalse(second.toggle("compose"))
    }

    @Test fun concurrentTogglesAreAtomicAndDoNotLoseOtherIds() = runTest {
        val storage = store(backgroundScope)
        coroutineScope {
            repeat(20) { launch { storage.toggle("compose") } }
            launch { storage.toggle("tests") }
        }
        assertEquals(setOf("tests"), storage.completedTaskIds())
    }

    @Test fun unknownTaskDoesNotWritePreferences() = runTest {
        val storage = store(backgroundScope)
        val repository = FakeTaskRepository(storage, 0)
        assertNull(repository.toggleTask("missing"))
        assertTrue(storage.completedTaskIds().isEmpty())
    }

    @Test fun thirdLoadFailsAndRetryRetainsSavedCompletion() = runTest {
        val storage = store(backgroundScope)
        val repository = FakeTaskRepository(storage, 0)
        repository.loadTasks()
        repository.toggleTask("compose")
        repository.loadTasks()
        try {
            repository.loadTasks()
            fail("Third load should fail")
        } catch (_: IOException) { /* expected */ }
        assertTrue(repository.loadTasks().first().completed)
        assertEquals(3, FakeTaskRepository(storage, 0).loadTasks().size)
    }
}
