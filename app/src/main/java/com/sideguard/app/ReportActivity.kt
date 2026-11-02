package com.sideguard.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sideguard.app.databinding.ActivityReportBinding

class ReportActivity : AppCompatActivity() {

    private lateinit var b: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReportBinding.inflate(layoutInflater)
        setContentView(b.root)

        val report = ReportHolder.report
        if (report == null) {
            finish()
            return
        }

        val risky = report.localRiskScore
        val mismatch = report.pinOutcome == SignatureStore.PinOutcome.MISMATCH
        val vtBad = report.vtVerdict.startsWith("⚠")

        b.banner.text = when {
            vtBad || mismatch -> "HIGH RISK — do not install without checking"
            risky >= 4 -> "CAUTION — heavy permissions"
            else -> "LOOKS OK — review below"
        }
        b.banner.setBackgroundColor(
            when {
                vtBad || mismatch -> Color.parseColor("#B00020")
                risky >= 4 -> Color.parseColor("#E8A200")
                else -> Color.parseColor("#1B7F3B")
            }
        )

        val sb = StringBuilder()
        sb.append("File: ${report.fileName}\n")
        sb.append("App: ${report.appLabel ?: "(unknown)"}\n")
        sb.append("Package: ${report.packageName ?: "(unreadable — may be corrupt)"}\n")
        sb.append("Version: ${report.versionName ?: "?"}\n\n")

        sb.append("SHA-256:\n${report.sha256}\n\n")
        sb.append("Signing cert SHA-256:\n${report.certSha256 ?: "(none)"}\n\n")

        sb.append("Signature pinning: ")
        sb.append(
            when (report.pinOutcome) {
                SignatureStore.PinOutcome.FIRST_SEEN -> "first time seeing this app — cert will be trusted on install"
                SignatureStore.PinOutcome.MATCH -> "MATCHES the key you trusted before ✓"
                SignatureStore.PinOutcome.MISMATCH -> "⚠ DIFFERENT signer than before — possible tampering/repackage"
                SignatureStore.PinOutcome.UNKNOWN -> "couldn't read a certificate"
            }
        )
        sb.append("\n\n")

        sb.append("VirusTotal: ${report.vtVerdict}\n\n")

        sb.append("High-risk permissions (${report.dangerousPermissions.size}):\n")
        if (report.dangerousPermissions.isEmpty()) {
            sb.append("  none of the watched ones\n")
        } else {
            report.dangerousPermissions.forEach { sb.append("  • $it\n") }
        }
        sb.append("\nAll requested permissions (${report.allPermissions.size}):\n")
        report.allPermissions.forEach { sb.append("  $it\n") }

        b.details.text = sb.toString()

        b.btnInstall.setOnClickListener {
            val file = ReportHolder.apkFile
            if (file == null || report.packageName == null) {
                Toast.makeText(this, "Nothing staged to install", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Trust this signer for future pin checks.
            report.certSha256?.let { SignatureStore.pin(this, report.packageName!!, it) }
            SystemActions.installApk(this, file)
        }
        b.btnCancel.setOnClickListener { finish() }
    }
}
