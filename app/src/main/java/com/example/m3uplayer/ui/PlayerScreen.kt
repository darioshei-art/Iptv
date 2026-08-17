package com.example.m3uplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import com.example.m3uplayer.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentItem = uiState.playlist?.items?.getOrNull(uiState.currentIndex)

    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var speedMenuOpen by remember { mutableStateOf(false) }

    // به‌روزرسانی نوار پیشرفت هر نیم‌ثانیه
    LaunchedEffect(uiState.isPlaying, uiState.currentIndex) {
        while (true) {
            position = viewModel.currentPositionMs()
            duration = viewModel.durationMs()
            delay(500)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(currentItem?.title ?: "در حال پخش") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // نمایشگر تصویر برای ویدیو/استریم زنده. اگر رسانه فقط صوتی باشد،
            // PlayerView خودش سطح تصویر را نشان نمی‌دهد و آیکون جایگزین دیده می‌شود.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val player = viewModel.playerOrNull()
                if (player != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false // کنترل‌های خودمان را در Compose داریم
                            }
                        },
                        update = { it.player = player },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (uiState.isBuffering) {
                    CircularProgressIndicator()
                } else if (player == null) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            if (currentItem?.isLive != true) {
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { fraction ->
                        val newPos = (fraction * duration).toLong()
                        viewModel.seekTo(newPos)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position), style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("پخش زنده", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "شافل",
                        tint = if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onClick = { viewModel.previous() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "قبلی")
                }
                FilledIconButton(onClick = { viewModel.playPause() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "پخش/توقف",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { viewModel.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "بعدی")
                }
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        when (uiState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "تکرار",
                        tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { speedMenuOpen = true }) {
                    Text("سرعت: ${uiState.playbackSpeed}x")
                }
                DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x") },
                            onClick = {
                                viewModel.setSpeed(speed)
                                speedMenuOpen = false
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                currentItem?.let { item ->
                    val isFav = uiState.favoriteUris.contains(item.uri)
                    IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "علاقه‌مندی"
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
