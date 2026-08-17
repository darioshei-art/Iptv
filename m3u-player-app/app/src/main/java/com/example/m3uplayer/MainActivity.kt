package com.example.m3uplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.m3uplayer.player.PlayerViewModel
import com.example.m3uplayer.ui.HomeScreen
import com.example.m3uplayer.ui.PlayerScreen
import com.example.m3uplayer.ui.PlaylistScreen
import com.example.m3uplayer.ui.theme.M3UPlayerTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            M3UPlayerTheme {
                val navController = rememberNavController()

                // اگر اپ از طریق باز کردن فایل m3u اجرا شده، مستقیم برو صفحه پلی‌لیست
                val startedFromFile = remember { intent?.data != null }
                LaunchedEffect(startedFromFile) {
                    if (startedFromFile) navController.navigate("playlist")
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = playerViewModel,
                            onOpenPlaylist = { navController.navigate("playlist") }
                        )
                    }
                    composable("playlist") {
                        PlaylistScreen(
                            viewModel = playerViewModel,
                            onItemSelected = { index ->
                                playerViewModel.playItemAt(index)
                                navController.navigate("player")
                            }
                        )
                    }
                    composable("player") {
                        PlayerScreen(viewModel = playerViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** وقتی کاربر فایل m3u را از فایل‌منیجر/مرورگر باز می‌کند، این‌جا دریافت می‌شود */
    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        when (uri.scheme) {
            "http", "https" -> playerViewModel.openRemoteM3U(uri.toString())
            else -> playerViewModel.openLocalM3U(uri)
        }
    }
}
