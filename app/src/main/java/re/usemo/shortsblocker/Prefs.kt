package re.usemo.shortsblocker

import android.content.Context

/** Tiny SharedPreferences wrapper holding the app's local settings. */
object Prefs {

    private const val FILE = "shorts_blocker_prefs"

    private const val KEY_SHORTS = "shorts_enabled"
    private const val KEY_APPS_ENABLED = "apps_enabled"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_SITES_ENABLED = "sites_enabled"
    private const val KEY_BLOCKED_SITES = "blocked_sites"
    private const val KEY_TOAST = "show_toast"
    private const val KEY_BLOCKED_COUNT = "blocked_count"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- Shorts blocking (kept separate, as requested) ---

    fun isShortsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHORTS, true)

    fun setShortsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_SHORTS, value).apply()

    // --- App blocking ---

    fun isAppsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_APPS_ENABLED, true)

    fun setAppsEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_APPS_ENABLED, value).apply()

    fun getBlockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(context: Context, value: Set<String>) =
        // Store a copy; the Set returned by getStringSet must not be mutated.
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, HashSet(value)).apply()

    // --- Website blocking ---

    fun isSitesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SITES_ENABLED, true)

    fun setSitesEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_SITES_ENABLED, value).apply()

    fun getBlockedSites(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_SITES, emptySet()) ?: emptySet()

    fun setBlockedSites(context: Context, value: Set<String>) =
        prefs(context).edit().putStringSet(KEY_BLOCKED_SITES, HashSet(value)).apply()

    fun addBlockedSite(context: Context, site: String) {
        val cleaned = normalizeSite(site)
        if (cleaned.isEmpty()) return
        setBlockedSites(context, getBlockedSites(context) + cleaned)
    }

    fun removeBlockedSite(context: Context, site: String) {
        setBlockedSites(context, getBlockedSites(context) - site)
    }

    /** Strips scheme, "www." and paths so we keep a bare domain like "facebook.com". */
    fun normalizeSite(raw: String): String {
        var s = raw.trim().lowercase()
        if (s.isEmpty()) return ""
        s = s.removePrefix("https://").removePrefix("http://")
        s = s.removePrefix("www.")
        s = s.substringBefore('/')
        s = s.substringBefore('?')
        return s.trim()
    }

    // --- Misc ---

    fun isShowToast(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TOAST, true)

    fun setShowToast(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_TOAST, value).apply()

    fun getBlockedCount(context: Context): Int =
        prefs(context).getInt(KEY_BLOCKED_COUNT, 0)

    fun incrementBlocked(context: Context): Int {
        val next = getBlockedCount(context) + 1
        prefs(context).edit().putInt(KEY_BLOCKED_COUNT, next).apply()
        return next
    }
}
