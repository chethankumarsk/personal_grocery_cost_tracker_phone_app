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
              "browser_download_url": "https://github.com/example/repo/releases/download/v1.1.0/app.apk",
              "size": 7996761,
              "digest": "sha256:3f9f137f41d0e43bb9d6a15ab8c27226dbb73f77fea82ff893d358d3299c7555"
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
        assertEquals(7_996_761L, release.apkSizeBytes)
        assertEquals(
            "3f9f137f41d0e43bb9d6a15ab8c27226dbb73f77fea82ff893d358d3299c7555",
            release.apkSha256,
        )
    }

    @Test
    fun missingOrMalformedDigestIsIgnored() {
        val json = """
            {
              "tag_name": "v1.1.0",
              "html_url": "https://example.test/release",
              "assets": [
                {
                  "name": "app.apk",
                  "browser_download_url": "https://example.test/app.apk",
                  "digest": "sha256:not-a-real-digest"
                }
              ]
            }
        """.trimIndent()

        val release = (
            GitHubReleaseParser.parse(json, currentVersion = "1.0.0", repository = "example/repo")
                as UpdateCheckResult.UpdateAvailable
            ).release

        assertNull(release.apkSha256)
        assertNull(release.apkSizeBytes)
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
