package com.sideguard.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks up an APK's SHA-256 against VirusTotal. ONLY the hash is sent — never
 * the file itself — so no app contents leave the phone. Needs a free API key,
 * stored locally. If no key is set, lookups are skipped.
 */
object VirusTotalClient {

    private const val PREFS = "sideguard_vt"
    private const val KEY = "api_key"

    fun hasKey(context: Context): Boolean = getKey(context)?.isNotBlank() == true

    fun getKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun setKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, key.trim()).apply()
    }

    sealed class Result {
        data class Found(val malicious: Int, val suspicious: Int, val total: Int) : Result()
        object NotInDatabase : Result()
        object NoKey : Result()
        data class Error(val message: String) : Result()
    }

    /** Blocking network call — run off the main thread. */
    fun lookup(context: Context, sha256: String): Result {
        val key = getKey(context)?.trim()
        if (key.isNullOrBlank()) return Result.NoKey

        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://www.virustotal.com/api/v3/files/$sha256")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("x-apikey", key)
                connectTimeout = 15000
                readTimeout = 15000
            }
            when (val code = conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val stats = JSONObject(body)
                        .getJSONObject("data")
                        .getJSONObject("attributes")
                        .getJSONObject("last_analysis_stats")
                    val malicious = stats.optInt("malicious", 0)
                    val suspicious = stats.optInt("suspicious", 0)
                    val total = malicious + suspicious +
                        stats.optInt("harmless", 0) +
                        stats.optInt("undetected", 0) +
                        stats.optInt("timeout", 0)
                    Result.Found(malicious, suspicious, total)
                }
                404 -> Result.NotInDatabase
                401 -> Result.Error("API key rejected (401)")
                429 -> Result.Error("Rate limit hit (429) — wait a minute")
                else -> Result.Error("HTTP $code")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "network error")
        } finally {
            conn?.disconnect()
        }
    }
}
