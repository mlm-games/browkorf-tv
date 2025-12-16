package com.phlox.tvwebbrowser.data.history

import com.phlox.tvwebbrowser.model.HistoryItem
import com.phlox.tvwebbrowser.model.dao.HistoryDao

class RoomHistoryRepository(private val dao: HistoryDao) : HistoryRepository {
    override suspend fun count() = dao.count()
    override suspend fun last(limit: Int) = dao.last(limit)
    override suspend fun insert(item: HistoryItem) = dao.insert(item)
    override suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)
    override suspend fun deleteWhereTimeLessThan(time: Long) = dao.deleteWhereTimeLessThan(time)
    override suspend fun frequentlyUsedUrls() = dao.frequentlyUsedUrls()

    override suspend fun delete(vararg items: HistoryItem) = dao.delete(*items)
    override suspend fun clearAll() = dao.deleteWhereTimeLessThan(Long.MAX_VALUE)
    override suspend fun page(offset: Long) = dao.allByLimitOffset(offset)
    override suspend fun search(query: String) = dao.search("%$query%", "%$query%")
}