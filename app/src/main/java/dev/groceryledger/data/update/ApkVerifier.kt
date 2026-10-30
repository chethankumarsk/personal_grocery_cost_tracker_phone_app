package dev.groceryledger.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest

sealed interface ApkVerification {
    data object Valid : ApkVerification
    data class Invalid(val reason: ApkVerificationFailure) : ApkVerification
}

enum class ApkVerificationFailure {
    UNREADABLE,
    WRONG_PACKAGE,
    NOT_NEWER,
    SIGNATURE_MISMATCH,
}

/**
 * Refuses an APK that is not a newer Grocery Ledger built with the same signing key.
 *
 * GitHub's SHA-256 digest catches a corrupted download. This check is the stronger one: even
 * if the repository or release asset were replaced, an APK signed with a different key can
 * never be installed over the existing app.
 */
class ApkVerifier(
    private val context: Context,
    private val expectedPackageName: String,
    private val currentVersionCode: Long,
) {
    fun verify(apk: File): ApkVerification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: return ApkVerification.Invalid(ApkVerificationFailure.UNREADABLE)

        if (archive.packageName != expectedPackageName) {
            return ApkVerification.Invalid(ApkVerificationFailure.WRONG_PACKAGE)
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= currentVersionCode) {
            return ApkVerification.Invalid(ApkVerificationFailure.NOT_NEWER)
        }

        val archiveSigners = signerDigests(archive, includeHistory = false)
        val installed = runCatching {
            packageManager.getPackageInfo(expectedPackageName, flags)
        }.getOrNull() ?: return ApkVerification.Invalid(ApkVerificationFailure.UNREADABLE)
        val installedSigners = signerDigests(installed, includeHistory = true)

        if (archiveSigners.isEmpty() || installedSigners.isEmpty()) {
            return ApkVerification.Invalid(ApkVerificationFailure.SIGNATURE_MISMATCH)
        }
        return if (archiveSigners.any { it in installedSigners }) {
            ApkVerification.Valid
        } else {
            ApkVerification.Invalid(ApkVerificationFailure.SIGNATURE_MISMATCH)
        }
    }

    /**
     * Archive APKs expose only their current signers. The installed package additionally
     * reports its rotation history, so an update signed with the rotated key still matches.
     */
    private fun signerDigests(info: PackageInfo, includeHistory: Boolean): Set<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (includeHistory) {
                signingInfo.signingCertificateHistory
            } else {
                signingInfo.apkContentsSigners
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }
        return signatures.map { it.toByteArray().sha256() }.toSet()
    }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return buildString(digest.size * 2) {
            for (byte in digest) {
                val value = byte.toInt() and 0xFF
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0F])
            }
        }
    }

    private companion object {
        val HEX_DIGITS = "0123456789abcdef".toCharArray()
    }
}
