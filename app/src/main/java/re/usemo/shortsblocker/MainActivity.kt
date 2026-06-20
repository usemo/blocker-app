package re.usemo.shortsblocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import re.usemo.shortsblocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchShorts.setOnCheckedChangeListener { _, v -> Prefs.setShortsEnabled(this, v) }
        binding.switchApps.setOnCheckedChangeListener { _, v -> Prefs.setAppsEnabled(this, v) }
        binding.switchSites.setOnCheckedChangeListener { _, v -> Prefs.setSitesEnabled(this, v) }
        binding.switchToast.setOnCheckedChangeListener { _, v -> Prefs.setShowToast(this, v) }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnPickApps.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        binding.btnManageSites.setOnClickListener {
            startActivity(Intent(this, SiteListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.switchShorts.isChecked = Prefs.isShortsEnabled(this)
        binding.switchApps.isChecked = Prefs.isAppsEnabled(this)
        binding.switchSites.isChecked = Prefs.isSitesEnabled(this)
        binding.switchToast.isChecked = Prefs.isShowToast(this)
        updateStatus()
    }

    private fun updateStatus() {
        if (isAccessibilityServiceEnabled()) {
            binding.textStatus.setText(R.string.status_active)
            binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.status_ok))
        } else {
            binding.textStatus.setText(R.string.status_inactive)
            binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warn))
        }
        binding.textAppsCount.text =
            getString(R.string.apps_count, Prefs.getBlockedApps(this).size)
        binding.textSitesCount.text =
            getString(R.string.sites_count, Prefs.getBlockedSites(this).size)
        binding.textBlocked.text =
            getString(R.string.blocked_count, Prefs.getBlockedCount(this))
    }

    /** Checks whether the user has turned on our accessibility service in system settings. */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected =
            ComponentName(this, ShortsBlockerService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (name in splitter) {
            if (name.equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
