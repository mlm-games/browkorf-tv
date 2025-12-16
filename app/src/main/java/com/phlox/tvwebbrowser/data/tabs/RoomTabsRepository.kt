package com.phlox.tvwebbrowser.data.tabs

import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.model.dao.TabsDao

class RoomTabsRepository(private val dao: TabsDao) : TabsRepository {
    override suspend fun loadTabs(incognito: Boolean) = dao.getAll(incognito)
    override suspend fun insert(tab: WebTabState) = dao.insert(tab)
    override suspend fun update(tab: WebTabState) = dao.update(tab)
    override suspend fun delete(tab: WebTabState) = dao.delete(tab)
    override suspend fun deleteAll(incognito: Boolean) = dao.deleteAll(incognito)
    override suspend fun unselectAll(incognito: Boolean) = dao.unselectAll(incognito)
    override suspend fun updatePositions(tabs: List<WebTabState>) = dao.updatePositions(tabs)
}