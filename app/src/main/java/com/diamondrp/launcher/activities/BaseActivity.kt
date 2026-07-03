package com.diamondrp.launcher.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.diamondrp.launcher.utils.LocaleManager

/**
 * Todas as Activities do launcher estendem esta classe para garantir que o idioma
 * salvo pelo usuário seja aplicado de forma consistente em toda a aplicação.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }
}
