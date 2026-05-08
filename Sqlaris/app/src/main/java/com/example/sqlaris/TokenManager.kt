package com.example.sqlaris

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val SESSIONS_KEY = stringPreferencesKey("user_sessions_list")
        private val CURRENT_SESSION_NAME_KEY = stringPreferencesKey("current_session_name")
    }

    val sessions: Flow<List<UserSession>> = context.dataStore.data.map { preferences ->
        val json = preferences[SESSIONS_KEY]
        if (json == null) {
            emptyList()
        } else {
            val type = object : TypeToken<List<UserSession>>() {}.type
            gson.fromJson(json, type)
        }
    }

    val selectedSessionName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_SESSION_NAME_KEY]
    }

    val currentToken: Flow<String?> = combine(sessions, selectedSessionName) { sessions, selectedName ->
        sessions.find { it.name == selectedName }?.token
    }

    suspend fun setSelectedSession(name: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_SESSION_NAME_KEY] = name
        }
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SESSIONS_KEY]
            val type = object : TypeToken<MutableList<UserSession>>() {}.type
            val list: MutableList<UserSession> = if (currentJson == null) {
                mutableListOf()
            } else {
                gson.fromJson(currentJson, type)
            }
            
            val index = list.indexOfFirst { it.name == session.name }
            if (index != -1) {
                list[index] = session
            } else {
                list.add(session)
            }
            
            preferences[SESSIONS_KEY] = gson.toJson(list)
        }
    }

    suspend fun updateSessionTables(sessionName: String, localTables: List<LocalTable>) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SESSIONS_KEY]
            val type = object : TypeToken<MutableList<UserSession>>() {}.type
            val list: MutableList<UserSession>? = if (currentJson != null) {
                gson.fromJson(currentJson, type)
            } else null
            
            list?.let {
                val index = it.indexOfFirst { s -> s.name == sessionName }
                if (index != -1) {
                    it[index] = it[index].copy(tables = localTables)
                    preferences[SESSIONS_KEY] = gson.toJson(it)
                }
            }
        }
    }

    suspend fun removeTable(sessionName: String, tableName: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SESSIONS_KEY]
            val type = object : TypeToken<MutableList<UserSession>>() {}.type
            val list: MutableList<UserSession>? = if (currentJson != null) {
                gson.fromJson(currentJson, type)
            } else null
            
            list?.let {
                val index = it.indexOfFirst { s -> s.name == sessionName }
                if (index != -1) {
                    val currentTables = it[index].tables.toMutableList()
                    currentTables.removeAll { t -> t.name == tableName }
                    it[index] = it[index].copy(tables = currentTables)
                    preferences[SESSIONS_KEY] = gson.toJson(it)
                }
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.remove(SESSIONS_KEY)
            preferences.remove(CURRENT_SESSION_NAME_KEY)
        }
    }
}
