package dev.groceryledger.data.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface ApkDownloadResult {
    data class Success(val file: File) : ApkDownloadResult
    data class Failure(val reason: ApkDownloadFailure) : ApkDownloadResult
}

enum class ApkDownloadFailure {
    NO_APK,
    NETWORK,
    HTTP,
    EMPTY,
    TOO_LARGE,
    SIZE_MISMATCH,
    CHECKSUM,
    STORAGE,
}

/**
 * Streams the APK from GitHub into the app cache.
 *
 * The file is written to a temporary name and only promoted to [APK_FILE] after the expected
 * size and (when GitHub reports one) the SHA-256 digest match. A cancelled or failed download
 * leaves no partial APK behind, so the installer can never be handed a truncated file.
 */
class ApkDownloader(
    private val downloadDir: File,
    private val userAgent: String,
) {
    suspend fun download(
        url: String,
        expectedSha256: String? = null,
        expectedSizeBytes: Long? = null,
        onProgress: (Int) -> Unit = {},
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        if (expectedSizeBytes != null && expectedSizeBytes > MAX_APK_BYTES) {
            return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.TOO_LARGE)
        }
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.STORAGE)
        }
        if (expectedSizeBytes != null && downloadDir.usableSpace in 1 until expectedSizeBytes) {
            return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.STORAGE)
        }

        val temp = File(downloadDir, "$APK_FILE.part")
        val target = File(downloadDir, APK_FILE)
        temp.delete()
        target.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", userAgent)
            }

            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.NO_APK)
                }
                else -> return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.HTTP)
            }

            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_APK_BYTES) {
                return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.TOO_LARGE)
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var lastPercent = -1
            onProgress(0)

            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue

                        total += read
                        if (total > MAX_APK_BYTES) {
                            return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.TOO_LARGE)
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)

                        if (declaredSize > 0) {
                            val percent = ((total * 100) / declaredSize).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }

            currentCoroutineContext().ensureActive()

            if (total == 0L) {
                return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.EMPTY)
            }
            if (expectedSizeBytes != null && total != expectedSizeBytes) {
                return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.SIZE_MISMATCH)
            }
            if (expectedSha256 != null &&
                !digest.digest().toHex().equals(expectedSha256, ignoreCase = true)
            ) {
                return@withContext ApkDownloadResult.Failure(ApkDownloadFailure.CHECKSUM)
            }

            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            onProgress(100)
            ApkDownloadResult.Success(target)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            ApkDownloadResult.Failure(ApkDownloadFailure.NETWORK)
        } finally {
            connection?.disconnect()
            if (!target.exists()) temp.delete()
        }
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xFF
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0F])
        }
    }

    companion object {
        const val APK_FILE = "update.apk"

        /** Guards against filling the device from a mis-published or hostile release. */
        const val MAX_APK_BYTES = 100L * 1024 * 1024

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private val HEX_DIGITS = "0123456789abcdef".toCharArray()
    }
}
