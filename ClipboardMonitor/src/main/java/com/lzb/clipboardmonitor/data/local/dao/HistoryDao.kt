package com.lzb.clipboardmonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lzb.clipboardmonitor.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM clipboard_history ORDER BY isPinned DESC, timestamp DESC, id DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query(
        """
        SELECT * FROM clipboard_history
        WHERE text LIKE '%' || :query || '%'
           OR result LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, timestamp DESC, id DESC
        """
    )
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAll()

    @Query("UPDATE clipboard_history SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePin(id: Long, isPinned: Boolean)

    @Query("SELECT isPinned FROM clipboard_history WHERE id = :id LIMIT 1")
    suspend fun getPinState(id: Long): Boolean?

    @Query(
        """
        DELETE FROM clipboard_history
        WHERE id NOT IN (
            SELECT id FROM clipboard_history
            ORDER BY isPinned DESC, timestamp DESC, id DESC
            LIMIT :limit
        )
        """
    )
    suspend fun deleteOld(limit: Int)
}
