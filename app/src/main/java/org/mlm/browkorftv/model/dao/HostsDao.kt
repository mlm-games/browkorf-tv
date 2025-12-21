package org.mlm.browkorftv.model.dao

import androidx.room.*
import org.mlm.browkorftv.model.HostConfig

@Dao
interface HostsDao {
    @Query("SELECT * FROM hosts WHERE host_name = :name")
    suspend fun findByHostName(name: String): HostConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HostConfig): Long

    @Update
    suspend fun update(item: HostConfig)
}