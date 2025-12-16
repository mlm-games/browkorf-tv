package com.phlox.tvwebbrowser.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.SharedPreferencesMigration
import com.phlox.tvwebbrowser.TVBro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun provideTvBroDataStore(context: Context): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        migrations = listOf(
            SharedPreferencesMigration(context, TVBro.MAIN_PREFS_NAME)
        ),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("tvbro_prefs") }
    )
}