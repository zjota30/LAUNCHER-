package com.diamondrp.launcher.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.diamondrp.launcher.storage.preferences.PreferencesManager

/**
 * Gerencia o tema (claro/escuro/sistema) usando SharedPreferences simples (síncrono),
 * pois o tema precisa ser aplicado em Application.onCreate() e em attachBaseContext(),
 * antes que qualquer Activity termine sua inicialização — DataStore é assíncrono
 * demais para esse ponto do ciclo de vida.
 */
object ThemeManager {

    private const val PREFS = "diamond_rp_theme_prefs"
    private const val KEY_THEME = "theme_mode"

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, PreferencesManager.THEME_SYSTEM) ?: PreferencesManager.THEME_SYSTEM
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
        applyTheme(theme)
    }

    fun applyTheme(theme: String = "") {
        val mode = when (theme.ifBlank { null }) {
            PreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            PreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applySavedTheme(context: Context) {
        applyTheme(getTheme(context))
    }
}
