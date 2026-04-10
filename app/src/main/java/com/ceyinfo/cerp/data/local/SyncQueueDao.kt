package com.ceyinfo.cerp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC")
    fun getAllLive(): LiveData<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('pending', 'failed') AND retries < max_retries ORDER BY created_at ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('pending', 'failed')")
    fun getPendingCountLive(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('pending', 'failed')")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM sync_queue WHERE status = 'completed'")
    suspend fun clearCompleted()

    @Query("UPDATE sync_queue SET status = 'pending' WHERE status = 'failed'")
    suspend fun resetFailed()

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): SyncQueueEntity?
}
