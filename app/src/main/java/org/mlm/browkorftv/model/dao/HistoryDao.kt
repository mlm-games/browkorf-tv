package org.mlm.browkorftv.model.dao

import androidx.room.*
import androidx.room.RoomWarnings.Companion.QUERY_MISMATCH
import org.mlm.browkorftv.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history")
    suspend fun getAll(): List<HistoryItem>

    @Insert
    suspend fun insert(item: HistoryItem): Long

    @Delete
    suspend fun delete(vararg item: HistoryItem)

    @Query("DELETE FROM history WHERE time < :time")
    suspend fun deleteWhereTimeLessThan(time: Long)

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query("SELECT * FROM history ORDER BY time DESC LIMIT :limit")
    suspend fun last(limit: Int = 1): List<HistoryItem>

    @Query("SELECT * FROM history ORDER BY time DESC LIMIT :limit")
    fun lastFlow(limit: Int = 1): Flow<List<HistoryItem>>

    @SuppressWarnings(QUERY_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT \"\" as id, title, url, favicon, count(url) as cnt, max(time) as time FROM history GROUP BY title, url, favicon ORDER BY cnt DESC, time DESC LIMIT 8")
    suspend fun frequentlyUsedUrls(): List<HistoryItem>

    @SuppressWarnings(QUERY_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT \"\" as id, title, url, favicon, count(url) as cnt, max(time) as time FROM history GROUP BY title, url, favicon ORDER BY cnt DESC, time DESC LIMIT 8")
    fun frequentlyUsedUrlsFlow(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history ORDER BY time DESC LIMIT 100 OFFSET :offset")
    suspend fun allByLimitOffset(offset: Long): List<HistoryItem>

    @Query("SELECT * FROM history WHERE (title LIKE :titleQuery ESCAPE '\\') OR (url LIKE :urlQuery ESCAPE '\\') ORDER BY time DESC LIMIT 100")
    suspend fun search(titleQuery: String, urlQuery: String): List<HistoryItem>

    @Query("UPDATE history SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
}