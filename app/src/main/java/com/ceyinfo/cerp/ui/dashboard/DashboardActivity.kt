package com.ceyinfo.cerp.ui.dashboard

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.data.repository.SyncRepository
import com.ceyinfo.cerp.databinding.ActivityDashboardBinding
import com.ceyinfo.cerp.ui.buselect.BuSelectActivity
import com.ceyinfo.cerp.ui.documents.DocumentUploadActivity
import com.ceyinfo.cerp.ui.gallery.GalleryActivity
import com.ceyinfo.cerp.ui.login.LoginActivity
import com.ceyinfo.cerp.ui.photos.PhotoUploadActivity
import com.ceyinfo.cerp.ui.queue.QueueActivity
import com.ceyinfo.cerp.ui.reports.DailyReportActivity
import com.ceyinfo.cerp.updater.AppUpdater
import com.ceyinfo.cerp.updater.UpdateDialog
import com.ceyinfo.cerp.util.NetworkMonitor
import com.ceyinfo.cerp.util.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncRepo: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system bar insets to bottom nav
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navBarInsets.bottom)
            insets
        }

        session = SessionManager(this)
        networkMonitor = NetworkMonitor(this)
        syncRepo = SyncRepository(this)

        if (!session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupBottomNav()
        observeNetwork()
        observeQueue()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val updater = AppUpdater(this@DashboardActivity)
            val release = updater.checkForUpdate()
            if (release != null) {
                UpdateDialog.show(this@DashboardActivity, release, updater)
            }
        }
    }

    private fun setupUI() {
        binding.tvBuName.text = session.businessUnitName ?: "No Unit"
        binding.tvUserEmail.text = session.email

        // Avatar initial
        val initial = session.email?.firstOrNull()?.uppercase() ?: "U"
        binding.tvAvatar.text = initial

        // Card clicks
        binding.cardPhotos.setOnClickListener {
            startActivity(Intent(this, PhotoUploadActivity::class.java))
        }
        binding.cardReport.setOnClickListener {
            startActivity(Intent(this, DailyReportActivity::class.java))
        }
        binding.cardGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.cardSync.setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }
        binding.cardQueue.setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }
        binding.cardDocuments.setOnClickListener {
            startActivity(Intent(this, DocumentUploadActivity::class.java))
        }

        binding.btnChangeBu.setOnClickListener {
            if (!networkMonitor.checkNetwork()) {
                Toast.makeText(this, getString(R.string.login_error_network), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, BuSelectActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener { /* already on home */ }

        binding.navReport.setOnClickListener {
            startActivity(Intent(this, DailyReportActivity::class.java))
        }

        binding.fabCamera.setOnClickListener {
            startActivity(Intent(this, PhotoUploadActivity::class.java))
        }

        binding.navGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.navSettings.setOnClickListener {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeNetwork() {
        networkMonitor.isOnline.observe(this) { online ->
            binding.tvStatus.text = if (online) getString(R.string.online) else getString(R.string.offline)

            val dotBg = binding.dotStatus.background as? GradientDrawable
                ?: GradientDrawable().apply { shape = GradientDrawable.OVAL }
            dotBg.setColor(ContextCompat.getColor(this,
                if (online) R.color.success else R.color.error
            ))
            binding.dotStatus.background = dotBg
        }
    }

    private fun observeQueue() {
        syncRepo.getPendingCountLive().observe(this) { count ->
            if (count > 0) {
                binding.cardQueue.visibility = View.VISIBLE
                binding.tvQueueCount.text = "$count item${if (count > 1) "s" else ""} pending sync"
            } else {
                binding.cardQueue.visibility = View.GONE
            }
            binding.tvSyncSubtitle.text = if (count > 0) "$count pending" else "Upload queue"
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@DashboardActivity).logout()
            } catch (_: Exception) { }
            ApiClient.clearSession()
            session.clearSession()
            startActivity(Intent(this@DashboardActivity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
    }
}
