package dev.groceryledger.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GitHubReleaseParserTest {
    private val releaseJson = """
        {
          "tag_name": "v1.1.0",
          "name": "Grocery Ledger 1.1.0",
          "body": "Faster entry and a new price history view.",
          "html_url": "https://github.com/example/repo/releases/tag/v1.1.0",
          "assets": [
            {
              "name": "GroceryLedger-1.1.0-release.apk",
              "browser_download_url": "https://github.com/example/repo/releases/download/v1.1.0/app.apk"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun newerReleaseIsAvailable() {
        val result = GitHubReleaseParser.parse(
            jsonText = releaseJson,
            currentVersion = "1.0.0",
            repository = "example/repo",
        )

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val release = (result as UpdateCheckResult.UpdateAvailable).release
        assertEquals("1.1.0", release.versionName)
        assertEquals("v1.1.0", release.tag)
        assertEquals("Grocery Ledger 1.1.0", release.title)
        assertEquals(
            "https://github.com/example/repo/releases/tag/v1.1.0",
            release.pageUrl,
        )
        assertEquals(
            "https://github.com/example/repo/releases/download/v1.1.0/app.apk",
            release.apkUrl,
        )
    }

    @Test
    fun currentReleaseIsUpToDate() {
        val result = GitHubReleaseParser.parse(
            jsonText = releaseJson,
            currentVersion = "1.1.0",
            repository = "example/repo",
        )

        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    @Test
    fun missingTagIsAParseFailure() {
        val result = GitHubReleaseParser.parse(
            jsonText = """{"name":"No tag"}""",
            currentVersion = "1.0.0",
            repository = "example/repo",
        )

        assertEquals(
            UpdateCheckResult.Failure(UpdateFailureReason.PARSE),
            result,
        )
    }

    @Test
    fun missingAssetsStillProducesARelease() {
        val result = GitHubReleaseParser.parse(
            jsonText = """{"tag_name":"v1.1.0","html_url":"https://example.test/release"}""",
            currentVersion = "1.0.0",
            repository = "example/repo",
        )

        val release = (result as UpdateCheckResult.UpdateAvailable).release
        assertNull(release.apkUrl)
        assertEquals("1.1.0", release.title)
    }
}
