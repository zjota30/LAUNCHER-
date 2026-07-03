package com.diamondrp.launcher.download

enum class DownloadStatus {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    EXTRACTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val errorMessage: String? = null
) {
    val percent: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val etaSeconds: Long
        get() {
            if (speedBytesPerSecond <= 0 || totalBytes <= 0) return -1
            val remainingBytes = (totalBytes - bytesDownloaded).coerceAtLeast(0)
            return remainingBytes / speedBytesPerSecond
        }
}
