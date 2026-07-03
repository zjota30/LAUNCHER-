package com.diamondrp.launcher.activities

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.diamondrp.launcher.databinding.ActivitySplashBinding
import com.diamondrp.launcher.network.RetrofitClient
import com.diamondrp.launcher.network.models.ServerConfig
import com.diamondrp.launcher.repository.ConfigRepository
import com.diamondrp.launcher.utils.Resource
import kotlinx.coroutines.launch

/**
 * Primeira tela exibida. Responsável por baixar o config.json remoto e decidir
 * se o usuário vai para a tela principal ou para a tela de atualização obrigatória.
 */
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val configRepository by lazy { ConfigRepository(RetrofitClient.apiService) }

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 900L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadConfigAndProceed()
    }

    private fun loadConfigAndProceed() {
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            when (val result = configRepository.fetchRemoteConfig()) {
                is Resource.Success -> {
                    ensureMinimumSplashTime(startTime)
                    handleConfigLoaded(result.data)
                }
                is Resource.Error -> {
                    ensureMinimumSplashTime(startTime)
                    binding.txtStatus.text = result.message
                    // Segue para a tela principal mesmo com falha de rede;
                    // MainActivity deve lidar com estado "offline"/sem config.
                    navigateToMain(config = null)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private suspend fun ensureMinimumSplashTime(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MIN_SPLASH_DURATION_MS) {
            kotlinx.coroutines.delay(MIN_SPLASH_DURATION_MS - elapsed)
        }
    }

    private fun handleConfigLoaded(config: ServerConfig) {
        if (config.maintenance) {
            binding.txtStatus.text = config.maintenanceMessage
                ?: "Servidor em manutenção. Tente novamente mais tarde."
            return
        }

        val needsMandatoryUpdate = isUpdateMandatory(config)
        if (needsMandatoryUpdate) {
            navigateToUpdate(config)
        } else {
            navigateToMain(config)
        }
    }

    private fun isUpdateMandatory(config: ServerConfig): Boolean {
        val minVersion = config.minClientVersion ?: return false
        // Comparação simples de versão semântica (major.minor.patch).
        return compareVersions(minVersion, config.version) > 0
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun navigateToMain(config: ServerConfig?) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToUpdate(config: ServerConfig) {
        val intent = Intent(this, UpdateActivity::class.java).apply {
            putExtra(UpdateActivity.EXTRA_DOWNLOAD_URL, config.downloadUrl)
            putExtra(UpdateActivity.EXTRA_CHECKSUM, config.fileChecksum)
            putExtra(UpdateActivity.EXTRA_VERSION, config.version)
            putExtra(UpdateActivity.EXTRA_FILE_SIZE, config.fileSize)
            putExtra(UpdateActivity.EXTRA_CHANGELOG, config.changelog.toTypedArray())
        }
        startActivity(intent)
        finish()
    }
}
