package com.sideguard.app

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest

/** Result of statically vetting an APK file before install. */
data class ApkReport(
    val fileName: String,
    val packageName: String?,
    val appLabel: String?,
    val versionName: String?,
    val sha256: String,
    val certSha256: String?,
    val dangerousPermissions: List<String>,
    val allPermissions: List<String>,
    val pinOutcome: SignatureStore.PinOutcome,
    var vtVerdict: String = "Not checked"
) {
    /** 0 = clean-ish, higher = riskier. Drives the banner colour. */
    val localRiskScore: Int
        get() {
            var s = dangerousPermissions.size
            if (pinOutcome == SignatureStore.PinOutcome.MISMATCH) s += 10
            return s
        }
}

object ApkAnalyzer {

    /**
     * Permissions worth surfacing to a human before they sideload something.
     * Not exhaustive — these are the ones that most often signal spyware/adware
     * or over-reach in off-store apps.
     */
    private val HIGH_RISK = mapOf(
        "android.permission.READ_SMS" to "Read your text messages",
        "android.permission.SEND_SMS" to "Send texts (can cost money / 2FA theft)",
        "android.permission.RECEIVE_SMS" to "Intercept incoming texts (2FA theft)",
        "android.permission.READ_CALL_LOG" to "Read your call history",
        "android.permission.WRITE_CALL_LOG" to "Modify your call history",
        "android.permission.READ_CONTACTS" to "Read your contacts",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Monitor outgoing calls",
        "android.permission.CALL_PHONE" to "Place calls without you",
        "android.permission.RECORD_AUDIO" to "Record from the microphone",
        "android.permission.CAMERA" to "Use the camera",
        "android.permission.ACCESS_FINE_LOCATION" to "Track precise location",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Track location in background",
        "android.permission.READ_PHONE_STATE" to "Read phone number / IMEI",
        "android.permission.SYSTEM_ALERT_WINDOW" to "Draw over other apps (overlay attacks)",
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to "Accessibility control (high-risk)",
        "android.permission.BIND_DEVICE_ADMIN" to "Device admin (can lock/wipe phone)",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Install other apps",
        "android.permission.QUERY_ALL_PACKAGES" to "See every app you have installed",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Auto-start on boot",
        "android.permission.READ_EXTERNAL_STORAGE" to "Read your files",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Write to your files",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "Full access to all files",
        "android.permission.POST_NOTIFICATIONS" to "Post notifications (ad spam)",
        "android.permission.FOREGROUND_SERVICE" to "Run persistently in background"
    )

    @Suppress("DEPRECATION")
    fun analyze(context: Context, apkFile: File, originalName: String): ApkReport {
        val pm = context.packageManager
        val path = apkFile.absolutePath

        val sha256 = sha256Of(apkFile)

        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        val info: PackageInfo? = try {
            pm.getPackageArchiveInfo(path, flags)
        } catch (e: Exception) {
            null
        }

        val perms = info?.requestedPermissions?.toList() ?: emptyList()
        val dangerous = perms.mapNotNull { p -> HIGH_RISK[p]?.let { "$it  [$p]" } }

        val certSha = info?.let { certSha256Of(it) }

        var label: String? = null
        var versionName: String? = null
        val pkg = info?.packageName
        if (info != null) {
            versionName = info.versionName
            // applicationInfo has no base path set for an archive; patch it so the label resolves.
            info.applicationInfo?.let { ai ->
                ai.sourceDir = path
                ai.publicSourceDir = path
                label = try {
                    pm.getApplicationLabel(ai).toString()
                } catch (e: Exception) {
                    null
                }
            }
        }

        val pinOutcome = if (pkg != null && certSha != null) {
            SignatureStore.check(context, pkg, certSha)
        } else {
            SignatureStore.PinOutcome.UNKNOWN
        }

        return ApkReport(
            fileName = originalName,
            packageName = pkg,
            appLabel = label,
            versionName = versionName,
            sha256 = sha256,
            certSha256 = certSha,
            dangerousPermissions = dangerous,
            allPermissions = perms,
            pinOutcome = pinOutcome
        )
    }

    private fun sha256Of(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().toHex()
    }

    @Suppress("DEPRECATION")
    private fun certSha256Of(info: PackageInfo): String? {
        val signingInfo = info.signingInfo
        val sigs = when {
            signingInfo != null && signingInfo.hasMultipleSigners() ->
                signingInfo.apkContentsSigners
            signingInfo != null ->
                signingInfo.apkContentsSigners
            else -> info.signatures
        } ?: return null
        val first = sigs.firstOrNull() ?: return null
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(first.toByteArray()).toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
