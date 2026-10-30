package dev.groceryledger.data.update

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.concurrent.thread

class ApkDownloaderTest {
    private lateinit var server: TestHttpServer
    private lateinit var downloadDir: File
    private lateinit var downloader: ApkDownloader

    private val payload = ByteArray(256 * 1024) { index -> (index % 251).toByte() }
    private val payloadSha256 = payload.sha256()

    @Before
    fun setUp() {
        downloadDir = Files.createTempDirectory("apk-downloader").toFile()
        downloader = ApkDownloader(downloadDir = downloadDir, userAgent = "GroceryLedger/test")
        server = TestHttpServer(payload)
    }

    @After
    fun tearDown() {
        server.close()
        downloadDir.deleteRecursively()
    }

    @Test
    fun downloadsAndPromotesAValidApk() {
        val progress = mutableListOf<Int>()

        val result = runBlocking {
            downloader.download(
                url = server.url("/app.apk"),
                expectedSha256 = payloadSha256,
                expectedSizeBytes = payload.size.toLong(),
                onProgress = { progress.add(it) },
            )
        }

        assertTrue(result is ApkDownloadResult.Success)
        val file = (result as ApkDownloadResult.Success).file
        assertArrayEquals(payload, file.readBytes())
        assertEquals(100, progress.last())
        assertFalse(File(downloadDir, "${ApkDownloader.APK_FILE}.part").exists())
    }

    @Test
    fun checksumMismatchDiscardsTheDownload() {
        val result = runBlocking {
            downloader.download(
                url = server.url("/app.apk"),
                expectedSha256 = "0".repeat(64),
                expectedSizeBytes = payload.size.toLong(),
            )
        }

        assertEquals(
            ApkDownloadResult.Failure(ApkDownloadFailure.CHECKSUM),
            result,
        )
        assertNoDownloadedApk()
    }

    @Test
    fun sizeMismatchDiscardsTheDownload() {
        val result = runBlocking {
            downloader.download(
                url = server.url("/app.apk"),
                expectedSizeBytes = payload.size.toLong() + 1,
            )
        }

        assertEquals(
            ApkDownloadResult.Failure(ApkDownloadFailure.SIZE_MISMATCH),
            result,
        )
        assertNoDownloadedApk()
    }

    @Test
    fun missingAssetIsReported() {
        val result = runBlocking { downloader.download(url = server.url("/missing")) }

        assertEquals(
            ApkDownloadResult.Failure(ApkDownloadFailure.NO_APK),
            result,
        )
        assertNoDownloadedApk()
    }

    @Test
    fun oversizedExpectedFileIsRejectedBeforeDownloading() {
        val result = runBlocking {
            downloader.download(
                url = server.url("/app.apk"),
                expectedSizeBytes = ApkDownloader.MAX_APK_BYTES + 1,
            )
        }

        assertEquals(
            ApkDownloadResult.Failure(ApkDownloadFailure.TOO_LARGE),
            result,
        )
        assertNoDownloadedApk()
    }

    private fun assertNoDownloadedApk() {
        assertFalse(File(downloadDir, ApkDownloader.APK_FILE).exists())
        assertFalse(File(downloadDir, "${ApkDownloader.APK_FILE}.part").exists())
    }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return buildString(digest.size * 2) {
            for (byte in digest) {
                val value = byte.toInt() and 0xFF
                append("0123456789abcdef"[value ushr 4])
                append("0123456789abcdef"[value and 0x0F])
            }
        }
    }

    /** Minimal single-connection-at-a-time HTTP server; avoids adding another test dependency. */
    private class TestHttpServer(private val payload: ByteArray) : Closeable {
        private val socket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        @Volatile
        private var running = true

        init {
            thread(isDaemon = true, name = "apk-downloader-test-server") {
                while (running) {
                    val client = try {
                        socket.accept()
                    } catch (_: IOException) {
                        break
                    }
                    runCatching { client.use { respond(it) } }
                }
            }
        }

        fun url(path: String): String = "http://127.0.0.1:${socket.localPort}$path"

        private fun respond(client: java.net.Socket) {
            val reader = client.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }

            val path = requestLine.split(' ').getOrNull(1).orEmpty()
            val output = client.getOutputStream()
            if (path == "/app.apk") {
                output.write(
                    ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Length: ${payload.size}\r\n" +
                        "Connection: close\r\n\r\n").toByteArray(),
                )
                output.write(payload)
            } else {
                output.write(
                    ("HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n").toByteArray(),
                )
            }
            output.flush()
        }

        override fun close() {
            running = false
            runCatching { socket.close() }
        }
    }
}
