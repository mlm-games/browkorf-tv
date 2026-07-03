package org.mlm.browkorftv.model.dao

import androidx.room.*
import org.mlm.browkorftv.model.Download

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    suspend fun getAll(): List<Download>

    @Insert
    fun insert(download: Download): Long

    @Update
    fun update(vararg downloads: Download)

    @Delete
    suspend fun delete(download: Download)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("SELECT * FROM downloads ORDER BY time DESC LIMIT 100 OFFSET :offset")
    suspend fun allByLimitOffset(offset: Long): List<Download>
}