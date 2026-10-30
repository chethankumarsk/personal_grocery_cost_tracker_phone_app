package dev.groceryledger.data.update

import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Exercises the real Android package parser against the installed debug APK. This is the
 * verification the in-app updater relies on before opening Android's installer.
 */
@RunWith(AndroidJUnit4::class)
class ApkVerifierTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageManager: PackageManager = context.packageManager
    private val packageName = context.packageName
    private val installedApk = File(
        packageManager.getApplicationInfo(packageName, 0).sourceDir,
    )
    private val versionCode = PackageInfoCompat.getLongVersionCode(
        packageManager.getPackageInfo(packageName, 0),
    )

    @Test
    fun acceptsSamePackageSignedApkWhenItIsNewer() {
        val verifier = ApkVerifier(context, packageName, currentVersionCode = versionCode - 1)

        assertEquals(ApkVerification.Valid, verifier.verify(installedApk))
    }

    @Test
    fun rejectsAnApkThatIsNotNewer() {
        val verifier = ApkVerifier(context, packageName, currentVersionCode = versionCode)

        assertEquals(
            ApkVerification.Invalid(ApkVerificationFailure.NOT_NEWER),
            verifier.verify(installedApk),
        )
    }

    @Test
    fun rejectsADifferentPackageName() {
        val verifier = ApkVerifier(
            context,
            expectedPackageName = "com.example.not.groceryledger",
            currentVersionCode = 0,
        )

        assertEquals(
            ApkVerification.Invalid(ApkVerificationFailure.WRONG_PACKAGE),
            verifier.verify(installedApk),
        )
    }

    @Test
    fun rejectsAFileThatIsNotAnApk() {
        val file = File(context.cacheDir, "not-an-apk.bin").apply {
            writeText("this is not an APK")
        }
        try {
            val verifier = ApkVerifier(context, packageName, currentVersionCode = 0)

            assertEquals(
                ApkVerification.Invalid(ApkVerificationFailure.UNREADABLE),
                verifier.verify(file),
            )
        } finally {
            file.delete()
        }
    }
}
