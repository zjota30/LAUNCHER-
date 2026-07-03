package com.diamondrp.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.diamondrp.launcher.R
import com.diamondrp.launcher.activities.MainActivity
import com.diamondrp.launcher.download.DownloadManager
import com.diamondrp.launcher.download.DownloadProgress
import com.diamondrp.launcher.download.DownloadStatus
import com.diamondrp.launcher.network.RetrofitClient
import com.diamondrp.launcher.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Serviço em primeiro plano responsável por baixar a data do jogo em segundo plano,
 * com suporte a pausar/continuar/cancelar. Expõe o progresso via [DownloadService.progress]
 * (StateFlow estático) para qualquer Activity observar, mesmo que o serviço sobreviva
 * além do ciclo de vida da tela que o iniciou.
 */
class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private val downloadManager by lazy { DownloadManager(okHttpClientForDownload()) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> downloadManager.pause()
            ACTION_RESUME -> handleResume(intent)
            ACTION_CANCEL -> handleCancel()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val url = intent.getStringExtra(EXTRA_URL) ?: return
        val checksum = intent.getStringExtra(EXTRA_CHECKSUM)
        val fileName = Uri_lastSegmentOrDefault(url)
        val destination = File(getExternalFilesDir(Constants.DOWNLOAD_FOLDER), fileName)

        startForeground(Constants.DOWNLOAD_NOTIFICATION_ID, buildNotification(_progress.value))

        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            observeProgressForNotification()
            downloadManager.download(url, destination, checksum)
        }
        observeManagerProgress()
    }

    private fun handleResume(intent: Intent) {
        downloadManager.resetForResume()
        handleStart(intent)
    }

    private fun handleCancel() {
        downloadManager.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun observeManagerProgress() {
        serviceScope.launch {
            downloadManager.progress.collect { progress ->
                _progress.value = progress
                updateNotification(progress)
                if (progress.status == DownloadStatus.COMPLETED ||
                    progress.status == DownloadStatus.CANCELLED ||
                    progress.status == DownloadStatus.FAILED
                ) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        }
    }

    private fun observeProgressForNotification() {
        // noop hook reservado para customizações futuras (ex: sons, vibração ao concluir)
    }

    private fun buildNotification(progress: DownloadProgress): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (progress.status) {
            DownloadStatus.CONNECTING -> "Conectando..."
            DownloadStatus.DOWNLOADING -> "Baixando arquivos do jogo (${progress.percent}%)"
            DownloadStatus.PAUSED -> "Download pausado"
            DownloadStatus.VERIFYING -> "Verificando integridade..."
            DownloadStatus.EXTRACTING -> "Extraindo arquivos..."
            DownloadStatus.COMPLETED -> "Download concluído"
            DownloadStatus.FAILED -> "Falha no download"
            DownloadStatus.CANCELLED -> "Download cancelado"
            DownloadStatus.IDLE -> "Preparando download..."
        }

        return NotificationCompat.Builder(this, Constants.DOWNLOAD_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(progress.status == DownloadStatus.DOWNLOADING || progress.status == DownloadStatus.CONNECTING)
            .setProgress(100, progress.percent, progress.status == DownloadStatus.CONNECTING)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(progress: DownloadProgress) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(Constants.DOWNLOAD_NOTIFICATION_ID, buildNotification(progress))
    }

    private fun okHttpClientForDownload(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun Uri_lastSegmentOrDefault(url: String): String {
        val last = url.substringAfterLast('/').substringBefore('?')
        return last.ifBlank { "gamedata.zip" }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.diamondrp.launcher.action.START"
        const val ACTION_PAUSE = "com.diamondrp.launcher.action.PAUSE"
        const val ACTION_RESUME = "com.diamondrp.launcher.action.RESUME"
        const val ACTION_CANCEL = "com.diamondrp.launcher.action.CANCEL"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_CHECKSUM = "extra_checksum"

        private val _progress = MutableStateFlow(DownloadProgress())
        val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

        fun resetProgress() {
            _progress.value = DownloadProgress()
        }
    }
}
