package re.usemo.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
 * Shorts and apps are handled from accessibility events (immediate). Website
 * blocking is instead driven by a small polling loop, because a loaded page does
 * not keep firing events — so a single event-time check easily misses the moment
 * the page is actually shown. Polling guarantees we see the committed URL.
 *
 * Everything runs locally. The app declares NO internet permission, so nothing
 * about what you do ever leaves the phone. From non-YouTube apps the service only
 * uses the package name (and, for browsers, the address-bar text) — it does not
 * read or store their content.
 */
class ShortsBlockerService : AccessibilityService() {

    private var lastActionTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var lastDiagnosticUrl: String? = null
    private var appliedScope: Set<String>? = null

    private val sitePoll = object : Runnable {
        override fun run() {
            try {
                applyScopeIfNeeded()
                checkBrowserSites()
            } finally {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        applyScopeIfNeeded()
        handler.removeCallbacks(sitePoll)
        handler.postDelayed(sitePoll, POLL_INTERVAL_MS)
    }

    /**
     * Restricts the service so it only observes the apps it actually needs:
     * YouTube, the supported browsers, and the apps the user chose to block.
     * Everything else (banking apps, etc.) is no longer observed at all. This
     * is both better for privacy and friendlier to anti-fraud checks that flag
     * accessibility services which watch every app.
     */
    private fun applyScopeIfNeeded() {
        val blockedApps = Prefs.getBlockedApps(this)
        if (blockedApps == appliedScope) return
        val info = serviceInfo ?: return
        val pkgs = LinkedHashSet<String>()
        pkgs.add(YT_PACKAGE)
        pkgs.addAll(BROWSER_URL_BARS.keys)
        pkgs.addAll(blockedApps)
        info.packageNames = pkgs.toTypedArray()
        serviceInfo = info
        appliedScope = blockedApps
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(sitePoll)
        return super.onUnbind(intent)
    }

    // --- Event-driven: Shorts + apps (need to be immediate) ---

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Never act on our own UI or the Android system UI.
        if (pkg == packageName || pkg == "com.android.systemui") return

        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < MIN_INTERVAL_MS) return

        // Blocked app in the foreground -> go HOME. Cheapest check, no tree scan.
        if (Prefs.isAppsEnabled(this) && Prefs.getBlockedApps(this).contains(pkg)) {
            lastActionTime = now
            performGlobalAction(GLOBAL_ACTION_HOME)
            onBlocked(getString(R.string.toast_blocked_app))
            return
        }

        // YouTube Shorts -> BACK.
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
        }
    }

    // --- Polling: website blocking ---

    private fun checkBrowserSites() {
        if (!Prefs.isSitesEnabled(this) && !Prefs.isDiagnostic(this)) return

        val root = rootInActiveWindow ?: return
        try {
            val pkg = root.packageName?.toString() ?: return
            val urlBarId = BROWSER_URL_BARS[pkg] ?: return

            val rawUrl = readUrlBar(root, urlBarId)

            // Diagnostic mode: surface exactly what the address bar exposes, so we
            // can see what Chrome reports while viewing vs while typing.
            if (Prefs.isDiagnostic(this) && rawUrl != null && rawUrl != lastDiagnosticUrl) {
                lastDiagnosticUrl = rawUrl
                val editing = if (isEditingAddressBar(root, pkg)) "EDYCJA" else "WIDOK"
                Toast.makeText(this, "[$editing] $rawUrl", Toast.LENGTH_SHORT).show()
            }

            if (!Prefs.isSitesEnabled(this)) return
            val blocked = Prefs.getBlockedSites(this)
            if (blocked.isEmpty()) return

            // Don't act while the user is editing the address bar (the omnibox
            // autocompletes as you type, which would block half-typed addresses).
            if (isEditingAddressBar(root, pkg)) return

            val host = rawUrl?.let { extractHost(it) } ?: return
            if (!host.contains('.')) return

            val match = blocked.firstOrNull { host == it || host.endsWith(".$it") }
            if (match != null) {
                val now = SystemClock.uptimeMillis()
                if (now - lastActionTime < MIN_INTERVAL_MS) return
                lastActionTime = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                onBlocked(getString(R.string.toast_blocked_site, match))
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    /**
     * True while the address bar is being edited. We detect this by the presence
     * of the omnibox suggestions list, which only exists while typing — a far more
     * reliable signal than view focus or the soft keyboard.
     */
    private fun isEditingAddressBar(root: AccessibilityNodeInfo, pkg: String): Boolean {
        for (suffix in OMNIBOX_SUGGESTION_ID_SUFFIXES) {
            val nodes = root.findAccessibilityNodeInfosByViewId("$pkg:id/$suffix") ?: continue
            for (node in nodes) {
                val visible = node.isVisibleToUser
                @Suppress("DEPRECATION")
                node.recycle()
                if (visible) return true
            }
        }
        return false
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

    /** Reads the raw address-bar text (first non-blank), lowercased. */
    private fun readUrlBar(root: AccessibilityNodeInfo, urlBarId: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(urlBarId) ?: return null
        var result: String? = null
        for (node in nodes) {
            val text = node.text?.toString()
            @Suppress("DEPRECATION")
            node.recycle()
            if (!text.isNullOrBlank()) {
                result = text.trim().lowercase()
                break
            }
        }
        return result
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

        /** How often we re-check the browser address bar, in milliseconds. */
        private const val POLL_INTERVAL_MS = 400L

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

        /** View-id suffixes of the omnibox suggestion list (present while editing). */
        private val OMNIBOX_SUGGESTION_ID_SUFFIXES = listOf(
            "omnibox_suggestions_dropdown",
            "omnibox_results_container",
            "omnibox_suggestions_list",
            "suggestions_dropdown"
        )
    }
}
