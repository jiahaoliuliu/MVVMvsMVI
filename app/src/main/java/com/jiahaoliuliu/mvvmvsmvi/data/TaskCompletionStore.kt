package com.jiahaoliuliu.mvvmvsmvi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// One application-scoped DataStore instance for both activities.
val Context.taskCompletionDataStore by preferencesDataStore(name = "task_completion")

class TaskCompletionStore(private val dataStore: DataStore<Preferences>) {
    private val completedIds = stringSetPreferencesKey("completed_task_ids")

    suspend fun completedTaskIds(): Set<String> = dataStore.data.first()[completedIds].orEmpty()

    suspend fun toggle(id: String): Boolean {
        val saved = dataStore.edit { preferences ->
            val current = preferences[completedIds].orEmpty()
            preferences[completedIds] = if (id in current) current - id else current + id
        }
        return id in saved[completedIds].orEmpty()
    }
}
