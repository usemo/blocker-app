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

        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(this, isChecked)
        }
        binding.switchToast.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setShowToast(this, isChecked)
        }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.switchEnabled.isChecked = Prefs.isEnabled(this)
        binding.switchToast.isChecked = Prefs.isShowToast(this)
        updateStatus()
    }

    private fun updateStatus() {
        val serviceOn = isAccessibilityServiceEnabled()
        if (serviceOn) {
            binding.textStatus.setText(R.string.status_active)
            binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.status_ok))
        } else {
            binding.textStatus.setText(R.string.status_inactive)
            binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warn))
        }
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
