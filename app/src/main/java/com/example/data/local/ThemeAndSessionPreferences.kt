package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ThemeMode
import com.example.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "sso_timetable_prefs")

class ThemeAndSessionPreferences(private val context: Context) : PreferencesRepository {

    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyFacultyId = stringPreferencesKey("active_faculty_id")
    private val keySemesterId = stringPreferencesKey("selected_semester_id")

    override val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        val stored = prefs[keyThemeMode]
        ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    override val activeFacultyId: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[keyFacultyId]
    }

    override val selectedSemesterId: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[keySemesterId]
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { prefs ->
            prefs[keyThemeMode] = mode.name
        }
    }

    override suspend fun setActiveFacultyId(facultyId: String?) {
        context.appDataStore.edit { prefs ->
            if (facultyId == null) prefs.remove(keyFacultyId)
            else prefs[keyFacultyId] = facultyId
        }
    }

    override suspend fun setSelectedSemesterId(semesterId: String) {
        context.appDataStore.edit { prefs ->
            prefs[keySemesterId] = semesterId
        }
    }
}
