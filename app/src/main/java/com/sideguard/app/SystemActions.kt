package com.sideguard.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Deep-links into system screens and hands an APK to the OS installer. */
object SystemActions {

    private fun tryStart(context: Context, vararg intents: Intent): Boolean {
        for (i in intents) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    /**
     * Opens the Auto Blocker screen directly. Verified component on One UI:
     * the Auto Blocker feature is backed by com.samsung.android.rampart.
     * We cannot toggle it (signature-level permission), only land the user on it.
     */
    fun openAutoBlocker(context: Context) {
        val ok = tryStart(
            context,
            // Primary: verified Auto Blocker main settings activity (rampart).
            Intent("com.samsung.android.rampart.action.MAIN_SETTING_ACTIVITY"),
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.rampart",
                    "com.samsung.android.rampart.ui.MainSettingActivity"
                )
            ),
            // Fallbacks for non-Samsung / older builds.
            Intent("android.settings.PRIVACY_SETTINGS"),
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        )
        if (!ok) Toast.makeText(
            context,
            "Open Settings → Security and privacy → Auto Blocker",
            Toast.LENGTH_LONG
        ).show()
    }

    fun openPlayProtect(context: Context) {
        val ok = tryStart(
            context,
            Intent().setComponent(
                ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.security.settings.VerifyAppsSettingsActivity"
                )
            ),
            Intent("com.google.android.gms.settings.VERIFY_APPS_SETTINGS")
        )
        if (!ok) Toast.makeText(
            context, "Open the Play Store → profile → Play Protect", Toast.LENGTH_LONG
        ).show()
    }

    /** Lets the user allow SideGuard itself as an install source. */
    fun openUnknownSources(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + context.packageName)
        )
        if (!tryStart(context, intent, Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))) {
            Toast.makeText(context, "Open Settings → Apps → Special access", Toast.LENGTH_LONG).show()
        }
    }

    /** Hand the vetted APK to the system package installer. */
    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
