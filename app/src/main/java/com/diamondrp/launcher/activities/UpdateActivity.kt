package com.diamondrp.launcher.activities

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.diamondrp.launcher.databinding.ActivityUpdateBinding
import com.diamondrp.launcher.download.DownloadStatus
import com.diamondrp.launcher.service.DownloadService
import com.diamondrp.launcher.storage.preferences.PreferencesManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Tela de atualização: mostra o changelog e, ao tocar em ATUALIZAR, inicia o
 * [DownloadService] em primeiro plano e observa seu progresso (funciona mesmo
 * que o download continue após a tela ser recriada por rotação, por exemplo).
 */
class UpdateActivity : BaseActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }

    private var downloadUrl: String? = null
    private var checksum: String? = null
    private var version: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readExtras()
        setupListeners()
        observeDownloadProgress()
    }

    private fun readExtras() {
        downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL)
        checksum = intent.getStringExtra(EXTRA_CHECKSUM)
        version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
        val changelog = intent.getStringArrayExtra(EXTRA_CHANGELOG)?.toList().orEmpty()

        binding.txtChangelog.text = if (changelog.isEmpty()) {
            "Melhorias gerais e correções de estabilidade."
        } else {
            changelog.joinToString("\n") { "•  $it" }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnUpdate.setOnClickListener { startDownload() }

        binding.btnPauseResume.setOnClickListener {
            val isPaused = DownloadService.progress.value.status == DownloadStatus.PAUSED
            if (isPaused) resumeDownload() else pauseDownload()
        }

        binding.btnCancel.setOnClickListener { cancelDownload() }
    }

    private fun startDownload() {
        val url = downloadUrl
        if (url.isNullOrBlank()) return

        DownloadService.resetProgress()
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_CHECKSUM, checksum)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun pauseDownload() {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE
        }
        startService(intent)
    }

    private fun resumeDownload() {
        val url = downloadUrl ?: return
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_CHECKSUM, checksum)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun cancelDownload() {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
        }
        startService(intent)
        binding.layoutProgress.visibility = android.view.View.GONE
        setActionButtonsVisible(downloading = false)
    }

    private fun observeDownloadProgress() {
        lifecycleScope.launch {
            DownloadService.progress.collect { progress ->
                if (progress.status == DownloadStatus.IDLE) return@collect

                binding.layoutProgress.visibility = android.view.View.VISIBLE
                setActionButtonsVisible(downloading = true)

                binding.progressDownload.progress = progress.percent
                binding.txtDownloadPercent.text = "${progress.percent}%"
                binding.txtDownloadSpeed.text = formatSpeed(progress.speedBytesPerSecond)
                binding.txtDownloadEta.text = formatEta(progress.etaSeconds)

                binding.txtDownloadStatus.text = when (progress.status) {
                    DownloadStatus.CONNECTING -> "Conectando..."
                    DownloadStatus.DOWNLOADING -> "Baixando arquivos..."
                    DownloadStatus.PAUSED -> "Pausado"
                    DownloadStatus.VERIFYING -> "Verificando integridade..."
                    DownloadStatus.EXTRACTING -> "Extraindo arquivos..."
                    DownloadStatus.COMPLETED -> "Concluído!"
                    DownloadStatus.FAILED -> progress.errorMessage ?: "Falha no download"
                    DownloadStatus.CANCELLED -> "Cancelado"
                    DownloadStatus.IDLE -> ""
                }

                binding.btnPauseResume.text = if (progress.status == DownloadStatus.PAUSED) {
                    getString(com.diamondrp.launcher.R.string.btn_resume)
                } else {
                    getString(com.diamondrp.launcher.R.string.btn_pause)
                }
                binding.btnPauseResume.isEnabled =
                    progress.status == DownloadStatus.DOWNLOADING || progress.status == DownloadStatus.PAUSED

                if (progress.status == DownloadStatus.COMPLETED) {
                    onDownloadCompleted()
                }
            }
        }
    }

    private fun setActionButtonsVisible(downloading: Boolean) {
        binding.btnUpdate.visibility = if (downloading) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnPauseResume.visibility = if (downloading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCancel.visibility = if (downloading) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun onDownloadCompleted() {
        lifecycleScope.launch {
            preferencesManager.setInstalledVersion(version)
            startActivity(Intent(this@UpdateActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond <= 0 -> "-- KB/s"
            bytesPerSecond < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB/s", bytesPerSecond / 1024.0)
            else -> String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
        }
    }

    private fun formatEta(seconds: Long): String {
        if (seconds < 0) return ""
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) {
            getString(com.diamondrp.launcher.R.string.download_time_remaining, "${minutes}min ${secs}s")
        } else {
            getString(com.diamondrp.launcher.R.string.download_time_remaining, "${secs}s")
        }
    }

    companion object {
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_CHECKSUM = "extra_checksum"
        const val EXTRA_VERSION = "extra_version"
        const val EXTRA_FILE_SIZE = "extra_file_size"
        const val EXTRA_CHANGELOG = "extra_changelog"
    }
}
