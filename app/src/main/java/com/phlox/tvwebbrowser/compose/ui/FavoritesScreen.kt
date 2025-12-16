package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommand
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.vm.FavoritesViewModel
import com.phlox.tvwebbrowser.model.FavoriteItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun FavoritesScreen(backStack: NavBackStack<NavKey>) {
    val vm: FavoritesViewModel = koinViewModel()
    val bus: BrowserCommandBus = koinInject()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Favorites", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            Text("Home page shortcuts", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            HomeSlotsRow(
                homeItems = state.homePage,
                onEditSlot = { order -> backStack.add(AppKey.HomePageSlotEditor(order)) }
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.add(AppKey.BookmarkEditor(id = null)) }) { Text("Add") }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.bookmarks, key = { it.id }) { item ->
                    BookmarkRow(
                        item = item,
                        onOpen = {
                            bus.trySend(BrowserCommand.Navigate(item.url.orEmpty(), inNewTab = false))
                            backStack.removeLastOrNull()
                        },
                        onEdit = { backStack.add(AppKey.BookmarkEditor(item.id)) },
                        onDelete = { vm.delete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSlotsRow(
    homeItems: List<FavoriteItem>,
    onEditSlot: (Int) -> Unit
) {
    // Typical home grid is 8 items; your HomePageLinksMode.BOOKMARKS uses order 0..7
    val slots = (0 until 8).map { order ->
        val item = homeItems.firstOrNull { it.order == order }
        order to item
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0..3) {
                val (order, item) = slots[i]
                SlotCard(order, item?.title ?: "Empty", onClick = { onEditSlot(order) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 4..7) {
                val (order, item) = slots[i]
                SlotCard(order, item?.title ?: "Empty", onClick = { onEditSlot(order) })
            }
        }
    }
}

@Composable
private fun SlotCard(order: Int, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, tonalElevation = 2.dp) {
        Column(Modifier.width(220.dp).padding(12.dp)) {
            Text("Slot $order", style = MaterialTheme.typography.bodySmall)
            Text(label, maxLines = 1)
        }
    }
}

@Composable
private fun BookmarkRow(
    item: FavoriteItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(onClick = onOpen) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title ?: item.url.orEmpty(), maxLines = 1)
                Text(item.url.orEmpty(), maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onOpen) { Text("Open") }
            Button(onClick = onEdit) { Text("Edit") }
            Button(onClick = onDelete) { Text("Delete") }
        }
    }
}