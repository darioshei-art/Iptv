package com.example.m3uplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.m3uplayer.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onOpenPlaylist: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var urlDialogOpen by remember { mutableStateOf(false) }

    // انتخاب فایل m3u/m3u8 از حافظه گوشی
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openLocalM3U(uri)
            onOpenPlaylist()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("پخش‌کننده M3U") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("مهم‌ترین قابلیت این اپ: باز کردن فایل‌های .m3u / .m3u8", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegurl", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("باز کردن فایل m3u")
                }
                OutlinedButton(
                    onClick = { urlDialogOpen = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("افزودن از لینک")
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        uiState.errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("پلی‌لیست فعلی", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val playlist = uiState.playlist
            if (playlist == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("هنوز پلی‌لیستی باز نشده", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleSmall)
                        Text("${playlist.items.size} آیتم", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onOpenPlaylist) { Text("مشاهده و پخش") }
                    }
                }
            }
        }
    }

    if (urlDialogOpen) {
        UrlInputDialog(
            onDismiss = { urlDialogOpen = false },
            onConfirm = { url ->
                urlDialogOpen = false
                viewModel.openRemoteM3U(url)
                onOpenPlaylist()
            }
        )
    }
}

@Composable
private fun UrlInputDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن پلی‌لیست از لینک") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("https://example.com/playlist.m3u8") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("باز کردن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
