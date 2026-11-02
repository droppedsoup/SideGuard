package com.sideguard.app

import android.content.Context

/**
 * Remembers the signing certificate we last saw for each package, so that a
 * later update signed by a *different* key (a classic tampering / repackaging
 * sign) can be flagged. Stored locally in SharedPreferences — nothing leaves
 * the phone.
 */
object SignatureStore {

    enum class PinOutcome {
        FIRST_SEEN,   // no record yet; this becomes the trusted cert if installed
        MATCH,        // same signer as before — good
        MISMATCH,     // different signer than what we pinned — danger
        UNKNOWN       // couldn't read a cert at all
    }

    private const val PREFS = "sideguard_pins"

    fun check(context: Context, pkg: String, certSha256: String): PinOutcome {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val known = prefs.getString(pkg, null) ?: return PinOutcome.FIRST_SEEN
        return if (known.equals(certSha256, ignoreCase = true)) PinOutcome.MATCH
        else PinOutcome.MISMATCH
    }

    /** Call once the user actually chooses to install, to trust this signer. */
    fun pin(context: Context, pkg: String, certSha256: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(pkg, certSha256)
            .apply()
    }

    fun forget(context: Context, pkg: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(pkg)
            .apply()
    }
}
