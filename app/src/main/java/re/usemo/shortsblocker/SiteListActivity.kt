package re.usemo.shortsblocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import re.usemo.shortsblocker.databinding.ActivitySiteListBinding

class SiteListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySiteListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySiteListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAddSite.setOnClickListener { addSite() }
        binding.siteInput.setOnEditorActionListener { _, _, _ ->
            addSite()
            true
        }

        render()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun addSite() {
        val raw = binding.siteInput.text?.toString().orEmpty()
        val cleaned = Prefs.normalizeSite(raw)
        if (cleaned.isEmpty()) return
        Prefs.addBlockedSite(this, cleaned)
        binding.siteInput.setText("")
        render()
    }

    private fun render() {
        val container = binding.sitesContainer
        container.removeAllViews()

        val sites = Prefs.getBlockedSites(this).sorted()
        binding.textEmpty.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        for (site in sites) {
            val row = inflater.inflate(R.layout.item_site, container, false)
            row.findViewById<TextView>(R.id.siteName).text = site
            row.findViewById<MaterialButton>(R.id.btnRemoveSite).setOnClickListener {
                Prefs.removeBlockedSite(this, site)
                render()
            }
            container.addView(row)
        }
    }
}
