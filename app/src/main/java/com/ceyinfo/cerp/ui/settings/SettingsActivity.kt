package com.ceyinfo.cerp.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.ceyinfo.cerp.BuildConfig
import com.ceyinfo.cerp.databinding.ActivitySettingsBinding
import com.ceyinfo.cerp.updater.AppUpdater
import com.ceyinfo.cerp.updater.UpdateDialog
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var updater: AppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updater = AppUpdater(this)

        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

        binding.btnBack.setOnClickListener { finish() }

        binding.rowCheckUpdate.setOnClickListener { checkForUpdate() }
    }

    private fun checkForUpdate() {
        binding.progressUpdate.visibility = View.VISIBLE
        binding.icUpdateArrow.visibility = View.GONE
        binding.tvUpdateStatus.text = "Checking..."

        lifecycleScope.launch {
            val release = updater.checkForUpdate(forceCheck = true)

            binding.progressUpdate.visibility = View.GONE
            binding.icUpdateArrow.visibility = View.VISIBLE

            if (release != null) {
                val version = release.tagName.removePrefix("v")
                binding.tvUpdateStatus.text = "New version available: v$version"
                UpdateDialog.show(this@SettingsActivity, release, updater)
            } else {
                binding.tvUpdateStatus.text = "You're on the latest version"
                Toast.makeText(this@SettingsActivity, "App is up to date", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
