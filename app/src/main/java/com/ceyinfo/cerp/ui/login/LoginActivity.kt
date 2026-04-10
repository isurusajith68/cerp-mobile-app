package com.ceyinfo.cerp.ui.login

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.LoginRequest
import com.ceyinfo.cerp.data.model.Organization
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.databinding.ActivityLoginBinding
import com.ceyinfo.cerp.databinding.ItemOrgBinding
import com.ceyinfo.cerp.ui.buselect.BuSelectActivity
import com.ceyinfo.cerp.ui.dashboard.DashboardActivity
import com.ceyinfo.cerp.util.NetworkMonitor
import com.ceyinfo.cerp.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: let content draw behind status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        networkMonitor = NetworkMonitor(this)

        // If already logged in with a BU selected, go to dashboard
        if (session.isLoggedIn && session.businessUnitId != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        // If logged in but no BU, go to BU select
        if (session.isLoggedIn && session.businessUnitId == null) {
            startActivity(Intent(this, BuSelectActivity::class.java))
            finish()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin(null) }

        // Enter key on password field triggers login
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            attemptLogin(null)
            true
        }
    }

    private fun attemptLogin(organizationId: String?) {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString()?.trim() ?: ""

        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return
        }

        if (!networkMonitor.checkNetwork()) {
            showError(getString(R.string.login_error_network))
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@LoginActivity)
                val response = api.login(LoginRequest(email, password, organizationId))

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!

                    // Multi-org: show picker dialog
                    if (data.selectOrgRequired == true && !data.organizations.isNullOrEmpty()) {
                        setLoading(false)
                        showOrgPicker(data.organizations)
                        return@launch
                    }

                    // Single org: save session and proceed
                    session.isLoggedIn = true
                    session.userId = data.userId
                    session.email = data.email
                    session.organizationId = data.organizationId
                    session.isOwner = data.isOwner

                    startActivity(Intent(this@LoginActivity, BuSelectActivity::class.java))
                    finish()
                } else {
                    val msg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "Invalid email or password"
                    showError(msg)
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showOrgPicker(organizations: List<Organization>) {
        val dialog = BottomSheetDialog(this, R.style.Theme_Cerp_BottomSheet)
        val view = layoutInflater.inflate(R.layout.dialog_org_picker, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rv_orgs)
        rv.layoutManager = LinearLayoutManager(this)

        val colors = intArrayOf(
            R.color.primary, R.color.level_division, R.color.level_project,
            R.color.level_site, R.color.info, R.color.success
        )

        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = organizations.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val binding = ItemOrgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return object : RecyclerView.ViewHolder(binding.root) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val org = organizations[position]
                val binding = ItemOrgBinding.bind(holder.itemView)

                binding.tvOrgName.text = org.name
                binding.tvInitial.text = org.name.firstOrNull()?.uppercase() ?: "O"

                // Cycle icon background color
                val colorRes = colors[position % colors.size]
                val bg = binding.bgIcon.background as? GradientDrawable
                    ?: GradientDrawable().apply { cornerRadius = 12f * holder.itemView.resources.displayMetrics.density }
                bg.setColor(ContextCompat.getColor(holder.itemView.context, colorRes))
                binding.bgIcon.background = bg

                holder.itemView.setOnClickListener {
                    dialog.dismiss()
                    attemptLogin(org.id)
                }
            }
        }

        dialog.show()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        if (loading) binding.tvError.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
    }
}
