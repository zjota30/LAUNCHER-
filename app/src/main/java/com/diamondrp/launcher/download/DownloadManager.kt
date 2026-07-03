package com.diamondrp.launcher.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Motor de download próprio do launcher (não usa DownloadManager do Android para
 * ter controle total de pausa/continuação via HTTP Range, velocidade e verificação
 * de integridade). Uma instância cuida de um download por vez.
 */
class DownloadManager(private val client: OkHttpClient) {

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    @Volatile private var currentCall: Call? = null
    @Volatile private var isCancelled = false
    @Volatile private var isPaused = false

    /**
     * Baixa [url] para [destinationFile], retomando automaticamente se o arquivo
     * já existir parcialmente (HTTP Range). Lança nada — erros são refletidos em [progress].
     */
    suspend fun download(url: String, destinationFile: File, expectedChecksum: String?) {
        isCancelled = false
        isPaused = false
        destinationFile.parentFile?.mkdirs()

        try {
            downloadWithResume(url, destinationFile)

            if (isCancelled) {
                destinationFile.delete()
                _progress.value = _progress.value.copy(status = DownloadStatus.CANCELLED)
                return
            }
            if (isPaused) {
                _progress.value = _progress.value.copy(status = DownloadStatus.PAUSED)
                return
            }

            if (!expectedChecksum.isNullOrBlank()) {
                _progress.value = _progress.value.copy(status = DownloadStatus.VERIFYING)
                val validFile = verifyChecksum(destinationFile, expectedChecksum)
                if (!validFile) {
                    destinationFile.delete()
                    _progress.value = _progress.value.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Arquivo corrompido (checksum inválido)."
                    )
                    return
                }
            }

            if (destinationFile.name.endsWith(".zip", ignoreCase = true)) {
                _progress.value = _progress.value.copy(status = DownloadStatus.EXTRACTING)
                val extractDir = File(destinationFile.parentFile, "extracted")
                extractZip(destinationFile, extractDir)
                destinationFile.delete()
            }

            _progress.value = _progress.value.copy(status = DownloadStatus.COMPLETED)
        } catch (e: Exception) {
            if (!isCancelled && !isPaused) {
                _progress.value = _progress.value.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Falha desconhecida no download."
                )
            }
        }
    }

    private fun downloadWithResume(url: String, destinationFile: File) {
        val existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L

        _progress.value = _progress.value.copy(
            status = DownloadStatus.CONNECTING,
            bytesDownloaded = existingBytes
        )

        val requestBuilder = Request.Builder().url(url)
        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        val call = client.newCall(requestBuilder.build())
        currentCall = call

        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Servidor retornou HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Corpo de resposta vazio.")
            val isPartial = response.code == 206
            val alreadyDownloaded = if (isPartial) existingBytes else 0L
            val totalBytes = alreadyDownloaded + body.contentLength()

            _progress.value = _progress.value.copy(
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = alreadyDownloaded,
                totalBytes = totalBytes
            )

            RandomAccessFile(destinationFile, "rw").use { raf ->
                raf.seek(alreadyDownloaded)
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = alreadyDownloaded
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L

                    while (true) {
                        if (isCancelled || isPaused) break

                        val read = input.read(buffer)
                        if (read == -1) break

                        raf.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLastUpdate += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 500) {
                            val speed = (bytesSinceLastUpdate * 1000) / elapsed
                            _progress.value = _progress.value.copy(
                                bytesDownloaded = downloaded,
                                totalBytes = totalBytes,
                                speedBytesPerSecond = speed
                            )
                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0
                        }
                    }

                    _progress.value = _progress.value.copy(bytesDownloaded = downloaded, totalBytes = totalBytes)
                }
            }
        }
    }

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    private fun extractZip(zipFile: File, destinationDir: File) {
        destinationDir.mkdirs()
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8 * 1024)
            while (entry != null) {
                val outFile = File(destinationDir, entry.name)
                // Proteção contra Zip Path Traversal (Zip Slip)
                if (!outFile.canonicalPath.startsWith(destinationDir.canonicalPath + File.separator)) {
                    throw SecurityException("Entrada de arquivo inválida no ZIP: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out ->
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun pause() {
        isPaused = true
        currentCall?.cancel()
    }

    fun cancel() {
        isCancelled = true
        currentCall?.cancel()
    }

    fun resetForResume() {
        isPaused = false
        isCancelled = false
    }
}
