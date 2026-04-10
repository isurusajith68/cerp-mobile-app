package com.ceyinfo.cerp.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.local.SyncQueueEntity
import com.ceyinfo.cerp.data.model.DailyReportRequest
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.data.repository.SyncRepository
import com.ceyinfo.cerp.databinding.ActivityDailyReportBinding
import com.ceyinfo.cerp.util.NetworkMonitor
import com.ceyinfo.cerp.util.SessionManager
import com.ceyinfo.cerp.worker.SyncWorkerUtil
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReportBinding
    private lateinit var session: SessionManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncRepo: SyncRepository
    private var selectedDate: String = ""

    private val weatherOptions = arrayOf("sunny", "cloudy", "rainy", "windy", "stormy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        networkMonitor = NetworkMonitor(this)
        syncRepo = SyncRepository(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Date picker
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        selectedDate = dateFormat.format(Date())
        binding.etDate.setText(selectedDate)
        binding.etDate.setOnClickListener { showDatePicker() }

        // Weather dropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            weatherOptions.map { it.replaceFirstChar { c -> c.uppercase() } })
        binding.dropdownWeather.setAdapter(adapter)

        // Safety incidents - show/hide notes
        binding.etSafetyIncidents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.toString()?.toIntOrNull() ?: 0
                binding.tilSafetyNotes.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        })

        binding.btnSaveDraft.setOnClickListener { saveReport(submit = false) }
        binding.btnSubmit.setOnClickListener { saveReport(submit = true) }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.etDate.setText(selectedDate)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveReport(submit: Boolean) {
        val workSummary = binding.etWorkSummary.text?.toString()?.trim() ?: ""
        if (workSummary.isEmpty()) {
            binding.etWorkSummary.error = "Work summary is required"
            return
        }

        val request = DailyReportRequest(
            reportDate = selectedDate,
            workSummary = workSummary,
            weather = binding.dropdownWeather.text?.toString()?.lowercase()?.ifEmpty { null },
            temperatureHigh = binding.etTempHigh.text?.toString()?.toDoubleOrNull(),
            temperatureLow = binding.etTempLow.text?.toString()?.toDoubleOrNull(),
            workforceCount = binding.etWorkforce.text?.toString()?.toIntOrNull(),
            subcontractorCount = binding.etSubcontractor.text?.toString()?.toIntOrNull(),
            issues = binding.etIssues.text?.toString()?.trim()?.ifEmpty { null },
            safetyIncidents = binding.etSafetyIncidents.text?.toString()?.toIntOrNull(),
            safetyNotes = binding.etSafetyNotes.text?.toString()?.trim()?.ifEmpty { null },
            materialNotes = binding.etMaterialNotes.text?.toString()?.trim()?.ifEmpty { null },
            equipmentNotes = binding.etEquipmentNotes.text?.toString()?.trim()?.ifEmpty { null }
        )

        if (networkMonitor.checkNetwork()) {
            submitOnline(request, submit)
        } else {
            queueOffline(request)
        }
    }

    private fun submitOnline(request: DailyReportRequest, submit: Boolean) {
        binding.progress.visibility = View.VISIBLE
        binding.btnSaveDraft.isEnabled = false
        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@DailyReportActivity)
                val createResponse = api.createDailyReport(request)

                if (createResponse.isSuccessful && createResponse.body()?.success == true) {
                    if (submit) {
                        val reportId = createResponse.body()!!.data!!.id
                        api.submitDailyReport(reportId)
                    }
                    Toast.makeText(this@DailyReportActivity,
                        getString(R.string.report_saved), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Fallback to queue
                    queueOffline(request)
                }
            } catch (e: Exception) {
                queueOffline(request)
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnSaveDraft.isEnabled = true
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun queueOffline(request: DailyReportRequest) {
        lifecycleScope.launch {
            val item = SyncQueueEntity(
                type = SyncQueueEntity.TYPE_DAILY_REPORT,
                metadataJson = Gson().toJson(request),
                businessUnitId = session.businessUnitId ?: ""
            )
            syncRepo.addToQueue(item)
            SyncWorkerUtil.enqueue(this@DailyReportActivity)

            Toast.makeText(this@DailyReportActivity,
                "Report queued for sync", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
    }
}
