package dev.groceryledger.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a verified APK to Android's package installer. The app never installs silently:
 * Android shows its own confirmation screen, and the first time the user must allow
 * "install unknown apps" for Grocery Ledger.
 */
class ApkInstaller(private val context: Context) {
    fun canInstallPackages(): Boolean =
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

    /** Returns false when permission is missing or Android has no installer activity. */
    fun promptInstall(apk: File): Boolean {
        if (!canInstallPackages()) return false

        return runCatching {
            val uri = FileProvider.getUriForFile(context, authority(), apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun authority(): String = "${context.packageName}.updates"

    fun permissionSettingsIntent(): Intent =
        Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
