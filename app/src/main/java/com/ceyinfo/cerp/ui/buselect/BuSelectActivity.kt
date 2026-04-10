package com.ceyinfo.cerp.ui.buselect

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.BusinessUnit
import com.ceyinfo.cerp.data.model.SelectUnitRequest
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.databinding.ActivityBuSelectBinding
import com.ceyinfo.cerp.ui.dashboard.DashboardActivity
import com.ceyinfo.cerp.ui.login.LoginActivity
import com.ceyinfo.cerp.util.SessionManager
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class BuSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuSelectBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: BuAdapter

    private var allBusinessUnits: List<BusinessUnit> = emptyList()
    private var treeNodes: List<BuTreeNode> = emptyList()
    private var currentFilter: String? = null
    private var currentSearch: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityBuSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        binding.tvUserEmail.text = session.email

        // Avatar initial from email
        val initial = session.email?.firstOrNull()?.uppercase() ?: "U"
        binding.tvAvatar.text = initial

        binding.rvBusinessUnits.layoutManager = LinearLayoutManager(this)
        binding.btnLogout.setOnClickListener { logout() }

        setupSearch()
        loadBusinessUnits()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearch = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    private fun setupLevelChips(bus: List<BusinessUnit>) {
        binding.chipGroupLevel.removeAllViews()

        val allChip = Chip(this).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                currentFilter = null
                applyFilters()
            }
        }
        binding.chipGroupLevel.addView(allChip)

        val levelCounts = bus.groupBy { it.level?.lowercase() ?: "unknown" }
            .mapValues { it.value.size }
            .toSortedMap(compareBy {
                when (it) {
                    "organization" -> 0; "division" -> 1; "project" -> 2; "site" -> 3; else -> 4
                }
            })

        for ((level, count) in levelCounts) {
            val displayName = level.replaceFirstChar { it.uppercase() }
            val chip = Chip(this).apply {
                text = "$displayName ($count)"
                isCheckable = true
                setOnClickListener {
                    currentFilter = level
                    applyFilters()
                }
            }
            binding.chipGroupLevel.addView(chip)
        }
    }

    private fun applyFilters() {
        val isSearching = currentSearch.isNotEmpty() || currentFilter != null

        if (isSearching) {
            // Flat filtered list (no tree) when searching
            var filtered = allBusinessUnits

            if (currentFilter != null) {
                filtered = filtered.filter { it.level?.lowercase() == currentFilter }
            }
            if (currentSearch.isNotEmpty()) {
                val q = currentSearch.lowercase()
                filtered = filtered.filter {
                    it.name.lowercase().contains(q) ||
                    (it.code?.lowercase()?.contains(q) == true)
                }
            }

            filtered = filtered.sortedWith(compareBy({ it.levelOrder ?: 99 }, { it.name }))

            // Show as flat list (depth 0, no children)
            val flatNodes = filtered.map { BuTreeNode(it, 0, false, false) }
            treeNodes = flatNodes
            rebuildAdapter()

            binding.tvCount.text = "${filtered.size} of ${allBusinessUnits.size} units"
        } else {
            // Full tree view
            treeNodes = BuTreeBuilder.buildTree(allBusinessUnits)
            rebuildAdapter()

            binding.tvCount.text = "${allBusinessUnits.size} business units"
        }

        binding.layoutEmpty.visibility =
            if (treeNodes.isEmpty() && allBusinessUnits.isNotEmpty()) View.VISIBLE else View.GONE
        binding.rvBusinessUnits.visibility =
            if (treeNodes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun rebuildAdapter() {
        adapter = BuAdapter(
            allBus = allBusinessUnits,
            onSelect = { bu -> selectUnit(bu) },
            onToggle = { position -> toggleTreeNode(position) }
        )
        binding.rvBusinessUnits.adapter = adapter
        adapter.submitNodes(treeNodes)
    }

    private fun toggleTreeNode(position: Int) {
        treeNodes = BuTreeBuilder.toggleNode(treeNodes, position, allBusinessUnits)
        adapter.submitNodes(treeNodes)
    }

    private fun loadBusinessUnits() {
        // Always fetch fresh from API (projects & sites with descendants)
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@BuSelectActivity)
                val response = api.getPermittedUnits()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    session.savePermittedBUs(data)
                    allBusinessUnits = data
                    setupLevelChips(data)
                    applyFilters()
                }
            } catch (e: Exception) {
                // Fallback to cached
                val cached = session.getPermittedBUs()
                if (cached.isNotEmpty()) {
                    allBusinessUnits = cached
                    setupLevelChips(cached)
                    applyFilters()
                }
                Toast.makeText(this@BuSelectActivity, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun selectUnit(bu: BusinessUnit) {
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@BuSelectActivity)
                val response = api.selectUnitMobile(SelectUnitRequest(bu.id))

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    session.businessUnitId = data.businessUnitId
                    session.businessUnitName = data.businessUnitName

                    startActivity(Intent(this@BuSelectActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@BuSelectActivity,
                        response.body()?.message ?: "Failed to select unit",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BuSelectActivity, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@BuSelectActivity).logout()
            } catch (_: Exception) { }
            ApiClient.clearSession()
            session.clearSession()
            startActivity(Intent(this@BuSelectActivity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }
}
