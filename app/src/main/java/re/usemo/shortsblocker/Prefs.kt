package re.usemo.shortsblocker

import android.content.Context

/** Tiny SharedPreferences wrapper holding the app's local settings. */
object Prefs {

    private const val FILE = "shorts_blocker_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOAST = "show_toast"
    private const val KEY_BLOCKED = "blocked_count"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun isShowToast(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TOAST, true)

    fun setShowToast(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_TOAST, value).apply()

    fun getBlockedCount(context: Context): Int =
        prefs(context).getInt(KEY_BLOCKED, 0)

    fun incrementBlocked(context: Context): Int {
        val next = getBlockedCount(context) + 1
        prefs(context).edit().putInt(KEY_BLOCKED, next).apply()
        return next
    }
}
