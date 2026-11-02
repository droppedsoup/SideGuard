package com.sideguard.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sideguard.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    private val pickApk = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vet(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnVet.setOnClickListener {
            pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
        }

        // If launched by tapping an APK elsewhere (browser, Files), vet it straight away.
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { vet(it) }
        }
        b.btnAutoBlocker.setOnClickListener { SystemActions.openAutoBlocker(this) }
        b.btnAddTile.setOnClickListener { requestAutoBlockerTile() }
        b.btnPlayProtect.setOnClickListener { SystemActions.openPlayProtect(this) }
        b.btnUnknownSources.setOnClickListener { SystemActions.openUnknownSources(this) }
        b.btnVpn.setOnClickListener { VpnStatus.openMullvad(this) }
        b.btnVtKey.setOnClickListener { promptVtKey() }
    }

    override fun onResume() {
        super.onResume()
        refreshVpn()
        b.vtState.text = if (VirusTotalClient.hasKey(this))
            "VirusTotal: key set ✓" else "VirusTotal: no key (lookups off)"
    }

    private fun refreshVpn() {
        val s = VpnStatus.read(this)
        b.vpnState.text = when {
            s.vpnActive -> "VPN tunnel: ACTIVE ✓" +
                (if (s.mullvadInstalled) " (Mullvad installed)" else "")
            s.mullvadInstalled -> "VPN tunnel: OFF — Mullvad installed but not connected"
            else -> "VPN tunnel: OFF — Mullvad not found"
        }
    }

    private fun vet(uri: Uri) {
        b.status.text = "Copying and analyzing…"
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val name = queryName(uri) ?: "picked.apk"
                    val dir = File(cacheDir, "apks").apply { mkdirs() }
                    val dest = File(dir, "staged.apk")
                    contentResolver.openInputStream(uri)!!.use { input ->
                        dest.outputStream().use { input.copyTo(it) }
                    }
                    val report = ApkAnalyzer.analyze(this@MainActivity, dest, name)
                    if (VirusTotalClient.hasKey(this@MainActivity)) {
                        report.vtVerdict = when (val r =
                            VirusTotalClient.lookup(this@MainActivity, report.sha256)) {
                            is VirusTotalClient.Result.Found ->
                                if (r.malicious + r.suspicious > 0)
                                    "⚠ ${r.malicious} malicious / ${r.suspicious} suspicious of ${r.total} engines"
                                else "Clean on all ${r.total} engines"
                            VirusTotalClient.Result.NotInDatabase ->
                                "Unknown to VirusTotal (never scanned)"
                            VirusTotalClient.Result.NoKey -> "No key"
                            is VirusTotalClient.Result.Error -> "VT error: ${r.message}"
                        }
                    }
                    report to dest
                }
                ReportHolder.report = result.first
                ReportHolder.apkFile = result.second
                b.status.text = ""
                startActivity(Intent(this@MainActivity, ReportActivity::class.java))
            } catch (e: Exception) {
                b.status.text = ""
                Toast.makeText(this@MainActivity, "Couldn't read that file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun queryName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun requestAutoBlockerTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(
                this,
                "Add it manually: swipe down, edit tiles, drag in \"Auto Blocker\"",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val sbm = getSystemService(android.app.StatusBarManager::class.java)
            sbm.requestAddTileService(
                android.content.ComponentName(this, AutoBlockerTileService::class.java),
                "Auto Blocker",
                android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_launcher),
                mainExecutor
            ) { /* result code — no action needed */ }
        } catch (e: Exception) {
            Toast.makeText(this, "Add it from the Quick Settings edit screen", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptVtKey() {
        val input = EditText(this).apply {
            hint = "VirusTotal API key"
            setText(VirusTotalClient.getKey(this@MainActivity) ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("VirusTotal API key")
            .setMessage("Free key from virustotal.com. Only the APK's hash is sent, never the file.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                VirusTotalClient.setKey(this, input.text.toString())
                onResume()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
