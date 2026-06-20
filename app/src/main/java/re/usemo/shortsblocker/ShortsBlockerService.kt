package re.usemo.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Accessibility service that powers all blocking. It observes foreground apps and:
 *
 *  - in YouTube, detects the Shorts ("reel") player and leaves it (BACK);
 *  - when a blocked app comes to the foreground, sends the user HOME;
 *  - in supported browsers, reads the address bar and leaves blocked sites (BACK).
 *
 * Everything runs locally. The app declares NO internet permission, so nothing
 * about what you do ever leaves the phone. From non-YouTube apps the service only
 * uses the package name (and, for browsers, the address-bar text) — it does not
 * read or store their content.
 */
class ShortsBlockerService : AccessibilityService() {

    private var lastActionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Never act on our own UI or the Android system UI.
        if (pkg == packageName || pkg == "com.android.systemui") return

        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < MIN_INTERVAL_MS) return

        // 1) Blocked app in the foreground -> go HOME. Cheapest check, no tree scan.
        if (Prefs.isAppsEnabled(this) && Prefs.getBlockedApps(this).contains(pkg)) {
            lastActionTime = now
            performGlobalAction(GLOBAL_ACTION_HOME)
            onBlocked(getString(R.string.toast_blocked_app))
            return
        }

        // 2) YouTube Shorts -> BACK.
        if (pkg == YT_PACKAGE && Prefs.isShortsEnabled(this)) {
            val root = rootInActiveWindow ?: return
            try {
                if (anyVisible(root, REEL_IDS)) {
                    lastActionTime = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    onBlocked(getString(R.string.toast_blocked_shorts))
                }
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
            return
        }

        // 3) Blocked website in a supported browser -> BACK.
        val urlBarId = BROWSER_URL_BARS[pkg]
        if (urlBarId != null && Prefs.isSitesEnabled(this)) {
            val blocked = Prefs.getBlockedSites(this)
            if (blocked.isEmpty()) return
            val root = rootInActiveWindow ?: return
            try {
                val host = readHostIfNotEditing(root, urlBarId) ?: return
                val match = blocked.firstOrNull { host == it || host.endsWith(".$it") }
                if (match != null) {
                    lastActionTime = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    onBlocked(getString(R.string.toast_blocked_site, match))
                }
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }
    }

    private fun onBlocked(message: String) {
        val count = Prefs.incrementBlocked(this)
        if (Prefs.isShowToast(this)) {
            Toast.makeText(this, "$message ($count)", Toast.LENGTH_SHORT).show()
        }
    }

    /** Returns true if any of the given view ids is present and visible. */
    private fun anyVisible(root: AccessibilityNodeInfo, ids: List<String>): Boolean {
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id) ?: continue
            for (node in nodes) {
                val visible = node.isVisibleToUser
                @Suppress("DEPRECATION")
                node.recycle()
                if (visible) return true
            }
        }
        return false
    }

    /**
     * Returns the host shown in the address bar, but ONLY when the bar is not
     * being edited. While the user is typing (the field is focused), we return
     * null so we never act on a half-typed address or Chrome's autocomplete —
     * that previously closed the keyboard and bounced the user off the page.
     */
    private fun readHostIfNotEditing(root: AccessibilityNodeInfo, urlBarId: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(urlBarId) ?: return null
        var host: String? = null
        for (node in nodes) {
            val editing = node.isFocused
            val text = node.text?.toString()
            @Suppress("DEPRECATION")
            node.recycle()
            if (editing) return null
            if (!text.isNullOrBlank()) {
                host = extractHost(text)
                break
            }
        }
        return host
    }

    /** Strips scheme, "www.", path and any trailing text, leaving a bare host. */
    private fun extractHost(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.removePrefix("https://").removePrefix("http://")
        s = s.removePrefix("www.")
        s = s.substringBefore('/')
        s = s.substringBefore(' ')
        return s
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up between interruptions.
    }

    companion object {
        private const val YT_PACKAGE = "com.google.android.youtube"

        /** Minimum time between two block actions, in milliseconds. */
        private const val MIN_INTERVAL_MS = 700L

        /** Resource ids unique to the Shorts ("reel") player across app versions. */
        private val REEL_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/reel_player_underlay",
            "com.google.android.youtube:id/reel_dyn_underlay"
        )

        /** Browser package -> address-bar view id. */
        private val BROWSER_URL_BARS = mapOf(
            "com.android.chrome" to "com.android.chrome:id/url_bar",
            "com.chrome.beta" to "com.chrome.beta:id/url_bar",
            "com.chrome.dev" to "com.chrome.dev:id/url_bar",
            "com.brave.browser" to "com.brave.browser:id/url_bar",
            "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
            "com.opera.browser" to "com.opera.browser:id/url_field",
            "com.sec.android.app.sbrowser"
                    to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "org.mozilla.firefox"
                    to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        )
    }
}
