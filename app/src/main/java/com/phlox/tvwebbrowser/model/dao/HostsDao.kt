package com.phlox.tvwebbrowser.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phlox.tvwebbrowser.model.HostConfig

@Dao
interface HostsDao {
    @Query("SELECT * FROM hosts WHERE host_name=:name")
    suspend fun findByHostName(name: String): HostConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: HostConfig): Long

    @Query("DELETE FROM hosts")
    suspend fun deleteAll()
}