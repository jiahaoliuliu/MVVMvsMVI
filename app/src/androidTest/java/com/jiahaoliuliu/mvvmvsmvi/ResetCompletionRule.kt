package com.jiahaoliuliu.mvvmvsmvi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.jiahaoliuliu.mvvmvsmvi.data.taskCompletionDataStore
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Clears persisted state before the activity launch rule, never deletes an active store file. */
class ResetCompletionRule : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { context.taskCompletionDataStore.edit { it.clear() } }
    }
}
