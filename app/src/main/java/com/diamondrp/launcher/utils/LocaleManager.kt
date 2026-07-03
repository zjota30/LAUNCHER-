package com.diamondrp.launcher.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Gerencia o idioma do app. Usa SharedPreferences (síncrono) pelo mesmo motivo
 * do [ThemeManager]: precisa ser aplicado em attachBaseContext(), antes do DataStore
 * conseguir entregar o valor de forma assíncrona.
 */
object LocaleManager {

    private const val PREFS = "diamond_rp_locale_prefs"
    private const val KEY_LANGUAGE = "language_code"
    const val DEFAULT_LANGUAGE = "pt"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /** Envolve o Context com a configuração de idioma salva. Chamar em attachBaseContext(). */
    fun wrap(context: Context): Context {
        val languageCode = getLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
