package com.example.m3uplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.m3uplayer.data.M3UItem
import com.example.m3uplayer.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlayerViewModel,
    onItemSelected: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlist = uiState.playlist
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(playlist?.name ?: "پلی‌لیست") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("جستجو در پلی‌لیست") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            if (playlist == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("پلی‌لیستی بارگذاری نشده است")
                }
                return@Column
            }

            val filtered = remember(query, playlist.items) {
                if (query.isBlank()) playlist.items
                else playlist.items.filter { it.title.contains(query, ignoreCase = true) }
            }

            LazyColumn {
                items(filtered) { item ->
                    val originalIndex = playlist.items.indexOf(item)
                    PlaylistRow(
                        item = item,
                        isFavorite = uiState.favoriteUris.contains(item.uri),
                        onClick = { onItemSelected(originalIndex) },
                        onFavoriteClick = { viewModel.toggleFavorite(item) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    item: M3UItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (item.isLive) {
            Icon(Icons.Default.LiveTv, contentDescription = "پخش زنده", modifier = Modifier.padding(end = 8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.groupTitle != null) {
                Text(item.groupTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "علاقه‌مندی"
            )
        }
    }
}
