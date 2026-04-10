package com.ceyinfo.cerp.ui.queue

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.repository.SyncRepository
import com.ceyinfo.cerp.databinding.ActivityQueueBinding
import com.ceyinfo.cerp.worker.SyncWorkerUtil
import kotlinx.coroutines.launch

class QueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQueueBinding
    private lateinit var syncRepo: SyncRepository
    private lateinit var adapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        syncRepo = SyncRepository(this)
        adapter = QueueAdapter()

        binding.rvQueue.layoutManager = LinearLayoutManager(this)
        binding.rvQueue.adapter = adapter

        binding.appbar.tvAppbarTitle.text = getString(R.string.queue_title)
        binding.appbar.btnBack.setOnClickListener { finish() }

        binding.btnRetryAll.setOnClickListener {
            lifecycleScope.launch {
                syncRepo.resetFailed()
                SyncWorkerUtil.enqueue(this@QueueActivity)
                Toast.makeText(this@QueueActivity, "Retrying all failed items", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearCompleted.setOnClickListener {
            lifecycleScope.launch {
                syncRepo.clearCompleted()
                Toast.makeText(this@QueueActivity, "Cleared completed items", Toast.LENGTH_SHORT).show()
            }
        }

        observeQueue()
    }

    private fun observeQueue() {
        syncRepo.getAllLive().observe(this) { items ->
            adapter.submitList(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.rvQueue.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
