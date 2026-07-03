package com.diamondrp.launcher.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.diamondrp.launcher.R
import com.diamondrp.launcher.databinding.ActivitySettingsBinding
import com.diamondrp.launcher.network.RetrofitClient
import com.diamondrp.launcher.repository.ConfigRepository
import com.diamondrp.launcher.storage.preferences.PreferencesManager
import com.diamondrp.launcher.utils.LocaleManager
import com.diamondrp.launcher.utils.Resource
import com.diamondrp.launcher.utils.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }
    private val configRepository by lazy { ConfigRepository(RetrofitClient.apiService) }

    private val themeOptions = listOf(
        PreferencesManager.THEME_SYSTEM to "Padrão do sistema",
        PreferencesManager.THEME_DARK to "Escuro",
        PreferencesManager.THEME_LIGHT to "Claro"
    )

    private val languageOptions = listOf(
        "pt" to "Português (Brasil)",
        "en" to "English"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshCurrentValues()
        updateCacheSizeLabel()
        setupListeners()
    }

    private fun refreshCurrentValues() {
        val currentTheme = ThemeManager.getTheme(this)
        binding.txtThemeValue.text = themeOptions.firstOrNull { it.first == currentTheme }?.second ?: ""

        val currentLanguage = LocaleManager.getLanguage(this)
        binding.txtLanguageValue.text = languageOptions.firstOrNull { it.first == currentLanguage }?.second ?: ""
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.itemTheme.setOnClickListener { showThemeDialog() }
        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
        binding.itemClearCache.setOnClickListener { confirmClearCache() }
        binding.itemCheckUpdate.setOnClickListener { checkForUpdate() }
        binding.itemAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun showThemeDialog() {
        val currentTheme = ThemeManager.getTheme(this)
        val currentIndex = themeOptions.indexOfFirst { it.first == currentTheme }.coerceAtLeast(0)
        val labels = themeOptions.map { it.second }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selected = themeOptions[which].first
                ThemeManager.setTheme(this, selected)
                lifecycleScope.launch { preferencesManager.setTheme(selected) }
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val currentLanguage = LocaleManager.getLanguage(this)
        val currentIndex = languageOptions.indexOfFirst { it.first == currentLanguage }.coerceAtLeast(0)
        val labels = languageOptions.map { it.second }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selected = languageOptions[which].first
                LocaleManager.setLanguage(this, selected)
                lifecycleScope.launch { preferencesManager.setLanguage(selected) }
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmClearCache() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_clear_cache)
            .setMessage("Isso vai apagar arquivos temporários e downloads incompletos. Deseja continuar?")
            .setPositiveButton("Limpar") { _, _ -> clearCache() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun clearCache() {
        cacheDir.deleteRecursively()
        externalCacheDir?.deleteRecursively()
        getExternalFilesDir(com.diamondrp.launcher.utils.Constants.DOWNLOAD_FOLDER)?.deleteRecursively()
        updateCacheSizeLabel()
        Toast.makeText(this, "Cache limpo com sucesso.", Toast.LENGTH_SHORT).show()
    }

    private fun updateCacheSizeLabel() {
        val cacheBytes = folderSize(cacheDir) + folderSize(externalCacheDir)
        binding.txtCacheSize.text = formatBytes(cacheBytes)
    }

    private fun folderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun checkForUpdate() {
        binding.progressCheckUpdate.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            when (val result = configRepository.fetchRemoteConfig()) {
                is Resource.Success -> {
                    binding.progressCheckUpdate.visibility = android.view.View.GONE
                    val config = result.data
                    val installedVersion = preferencesManager.installedVersionFlow
                    Toast.makeText(
                        this@SettingsActivity,
                        "Versão do servidor: ${config.version}",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@SettingsActivity, UpdateActivity::class.java).apply {
                        putExtra(UpdateActivity.EXTRA_DOWNLOAD_URL, config.downloadUrl)
                        putExtra(UpdateActivity.EXTRA_CHECKSUM, config.fileChecksum)
                        putExtra(UpdateActivity.EXTRA_VERSION, config.version)
                        putExtra(UpdateActivity.EXTRA_CHANGELOG, config.changelog.toTypedArray())
                    }
                    startActivity(intent)
                }
                is Resource.Error -> {
                    binding.progressCheckUpdate.visibility = android.view.View.GONE
                    Toast.makeText(this@SettingsActivity, result.message, Toast.LENGTH_LONG).show()
                }
                Resource.Loading -> Unit
            }
        }
    }
}
