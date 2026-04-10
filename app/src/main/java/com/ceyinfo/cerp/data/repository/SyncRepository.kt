package com.ceyinfo.cerp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.ceyinfo.cerp.data.local.AppDatabase
import com.ceyinfo.cerp.data.local.SyncQueueEntity

class SyncRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).syncQueueDao()

    fun getAllLive(): LiveData<List<SyncQueueEntity>> = dao.getAllLive()

    fun getPendingCountLive(): LiveData<Int> = dao.getPendingCountLive()

    suspend fun addToQueue(item: SyncQueueEntity) = dao.insert(item)

    suspend fun getPendingItems() = dao.getPendingItems()

    suspend fun updateItem(item: SyncQueueEntity) = dao.update(item)

    suspend fun deleteItem(item: SyncQueueEntity) = dao.delete(item)

    suspend fun clearCompleted() = dao.clearCompleted()

    suspend fun resetFailed() = dao.resetFailed()

    suspend fun getById(id: String) = dao.getById(id)

    suspend fun getPendingCount() = dao.getPendingCount()
}
