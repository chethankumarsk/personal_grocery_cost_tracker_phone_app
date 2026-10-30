package dev.groceryledger.data.update

data class ReleaseInfo(
    val versionName: String,
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?,
    /** SHA-256 of the APK asset as reported by GitHub, if the release provides one. */
    val apkSha256: String? = null,
    /** Size of the APK asset in bytes as reported by GitHub. */
    val apkSizeBytes: Long? = null,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object NoReleases : UpdateCheckResult
    data class Failure(val reason: UpdateFailureReason) : UpdateCheckResult
}

enum class UpdateFailureReason {
    NETWORK,
    RATE_LIMITED,
    SERVER,
    PARSE,
}

/**
 * Why an in-app update could not be downloaded, verified or handed to Android's installer.
 * Verification failures are final: the bad file is deleted instead of being offered again.
 */
enum class UpdateInstallFailure {
    DOWNLOAD,
    CHECKSUM,
    INCOMPLETE,
    TOO_LARGE,
    STORAGE,
    UNREADABLE,
    WRONG_PACKAGE,
    NOT_NEWER,
    SIGNATURE,
    LAUNCH,
}

object UpdateConfig {
    const val GITHUB_REPOSITORY = "chethankumarsk/personal_grocery_cost_tracker_phone_app"
    const val RELEASES_PAGE = "https://github.com/$GITHUB_REPOSITORY/releases"
}

object VersionComparator {
    /**
     * Returns true when [latest] is a newer semver-looking version than [current].
     * Build metadata and pre-release suffixes are ignored. A non-numeric tag is
     * treated as different (and therefore offerable) rather than hiding an update.
     */
    fun isNewer(current: String, latest: String): Boolean {
        val currentCore = parseCore(current)
        val latestCore = parseCore(latest)
        if (currentCore == null || latestCore == null) {
            return normalize(latest) != normalize(current)
        }

        val size = maxOf(currentCore.size, latestCore.size)
        for (index in 0 until size) {
            val currentPart = currentCore.getOrElse(index) { 0 }
            val latestPart = latestCore.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    fun normalize(version: String): String =
        version.trim()
            .removePrefix("v")
            .removePrefix("V")

    private fun parseCore(version: String): List<Int>? {
        val core = normalize(version)
            .substringBefore('+')
            .substringBefore('-')
        if (core.isEmpty()) return null
        val parts = core.split('.')
        if (parts.any { it.toIntOrNull() == null }) return null
        return parts.map { it.toInt() }
    }
}
