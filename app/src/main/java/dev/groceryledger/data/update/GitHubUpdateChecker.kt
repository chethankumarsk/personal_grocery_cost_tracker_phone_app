package dev.groceryledger.data.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface UpdateChecker {
    suspend fun checkForUpdates(): UpdateCheckResult
}

class GitHubUpdateChecker(
    private val currentVersion: String,
    private val repository: String = UpdateConfig.GITHUB_REPOSITORY,
) : UpdateChecker {

    override suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/$repository/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "GroceryLedger/$currentVersion")
        }

        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    GitHubReleaseParser.parse(body, currentVersion, repository)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> UpdateCheckResult.NoReleases
                HttpURLConnection.HTTP_FORBIDDEN,
                HTTP_TOO_MANY_REQUESTS -> UpdateCheckResult.Failure(UpdateFailureReason.RATE_LIMITED)
                else -> UpdateCheckResult.Failure(UpdateFailureReason.SERVER)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            UpdateCheckResult.Failure(UpdateFailureReason.NETWORK)
        } catch (_: JSONException) {
            UpdateCheckResult.Failure(UpdateFailureReason.PARSE)
        } catch (_: Exception) {
            UpdateCheckResult.Failure(UpdateFailureReason.NETWORK)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

object GitHubReleaseParser {
    fun parse(
        jsonText: String,
        currentVersion: String,
        repository: String,
    ): UpdateCheckResult {
        val json = JSONObject(jsonText)
        val tag = json.optString("tag_name").trim()
        if (tag.isEmpty()) return UpdateCheckResult.Failure(UpdateFailureReason.PARSE)

        val version = VersionComparator.normalize(tag)
        val pageUrl = json.optString("html_url").ifBlank {
            "https://github.com/$repository/releases/tag/$tag"
        }
        val title = json.optString("name").ifBlank { version }

        var apkUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").ifBlank { null }
                    break
                }
            }
        }

        val release = ReleaseInfo(
            versionName = version,
            tag = tag,
            title = title,
            notes = json.optString("body"),
            pageUrl = pageUrl,
            apkUrl = apkUrl,
        )

        return if (VersionComparator.isNewer(currentVersion, tag)) {
            UpdateCheckResult.UpdateAvailable(release)
        } else {
            UpdateCheckResult.UpToDate
        }
    }
}
