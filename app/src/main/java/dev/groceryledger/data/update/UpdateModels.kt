package dev.groceryledger.data.update

data class ReleaseInfo(
    val versionName: String,
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?,
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
