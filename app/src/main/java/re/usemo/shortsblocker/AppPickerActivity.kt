package re.usemo.shortsblocker

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import re.usemo.shortsblocker.databinding.ActivityAppPickerBinding

class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private val adapter = AppAdapter()

    private data class AppInfo(val pkg: String, val label: String, val icon: Drawable)

    private var allApps: List<AppInfo> = emptyList()
    private val selected = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        selected.addAll(Prefs.getBlockedApps(this))
        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.appList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadApps() {
        binding.progress.visibility = View.VISIBLE
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
            val apps = resolved
                .asSequence()
                .map { it.activityInfo.packageName }
                .filter { it != packageName }
                .distinct()
                .mapNotNull { pkg ->
                    try {
                        val ai = pm.getApplicationInfo(pkg, 0)
                        AppInfo(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai))
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.label.lowercase() }
                .toList()

            runOnUiThread {
                allApps = apps
                binding.progress.visibility = View.GONE
                filter(binding.searchInput.text?.toString().orEmpty())
            }
        }.start()
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        val list = if (q.isEmpty()) allApps
        else allApps.filter { it.label.lowercase().contains(q) || it.pkg.contains(q) }
        adapter.submit(list)
    }

    private fun toggle(pkg: String) {
        if (selected.contains(pkg)) selected.remove(pkg) else selected.add(pkg)
        Prefs.setBlockedApps(this, selected)
    }

    private inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        private var items: List<AppInfo> = emptyList()

        fun submit(list: List<AppInfo>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = items[position]
            holder.label.text = app.label
            holder.icon.setImageDrawable(app.icon)
            holder.check.isChecked = selected.contains(app.pkg)
            holder.itemView.setOnClickListener {
                toggle(app.pkg)
                holder.check.isChecked = selected.contains(app.pkg)
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.appIcon)
            val label: TextView = v.findViewById(R.id.appLabel)
            val check: MaterialCheckBox = v.findViewById(R.id.appCheck)
        }
    }
}
