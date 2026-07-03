package com.diamondrp.launcher.storage.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "diamond_rp_settings")

/**
 * Camada de persistência de preferências do usuário usando Jetpack DataStore
 * (substitui SharedPreferences, evita I/O bloqueante na thread principal).
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val INSTALLED_VERSION = stringPreferencesKey("installed_version")
    }

    val nicknameFlow: Flow<String> = context.dataStore.data.map { it[Keys.NICKNAME] ?: "" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: THEME_SYSTEM }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "pt-BR" }
    val installedVersionFlow: Flow<String> = context.dataStore.data.map { it[Keys.INSTALLED_VERSION] ?: "" }

    suspend fun getNickname(): String = nicknameFlow.first()

    suspend fun setNickname(value: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = value }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = value }
    }

    suspend fun setInstalledVersion(value: String) {
        context.dataStore.edit { it[Keys.INSTALLED_VERSION] = value }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
    }
}
