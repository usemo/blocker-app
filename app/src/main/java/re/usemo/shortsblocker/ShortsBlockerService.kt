package re.usemo.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Accessibility service that detects the YouTube Shorts ("reel") player and
 * immediately leaves it by performing the global BACK action. The result is
 * that whenever a Short opens, the user is bounced out of it right away, so
 * Shorts effectively never play.
 *
 * Everything runs locally on the device. The app declares no internet
 * permission, so nothing about what you watch ever leaves the phone.
 */
class ShortsBlockerService : AccessibilityService() {

    private var lastActionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != YT_PACKAGE) return
        if (!Prefs.isEnabled(this)) return

        // Throttle so we don't scan the tree on every content change and so we
        // don't fire BACK repeatedly while the screen transitions.
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < MIN_INTERVAL_MS) return

        val root = rootInActiveWindow ?: return
        try {
            if (isShortsVisible(root)) {
                lastActionTime = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                val count = Prefs.incrementBlocked(this)
                if (Prefs.isShowToast(this)) {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_blocked, count),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    /**
     * Returns true if a Shorts player view is currently visible. We look for the
     * internal "reel_*" view ids, which belong to the Shorts player only (not the
     * Shorts tab button in the bottom bar), to avoid false positives.
     */
    private fun isShortsVisible(root: AccessibilityNodeInfo): Boolean {
        for (id in REEL_IDS) {
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

    override fun onInterrupt() {
        // No-op: nothing to clean up between interruptions.
    }

    companion object {
        private const val YT_PACKAGE = "com.google.android.youtube"

        /** Minimum time between two BACK actions, in milliseconds. */
        private const val MIN_INTERVAL_MS = 700L

        /**
         * Resource ids that are unique to the Shorts ("reel") player. We check
         * several because YouTube changes them between app versions.
         */
        private val REEL_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/reel_player_underlay",
            "com.google.android.youtube:id/reel_dyn_underlay"
        )
    }
}
