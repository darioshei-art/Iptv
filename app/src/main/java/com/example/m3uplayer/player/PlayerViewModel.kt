package com.example.m3uplayer.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.m3uplayer.data.M3UItem
import com.example.m3uplayer.data.M3UPlaylist
import com.example.m3uplayer.data.M3URepository
import com.example.m3uplayer.data.db.AppDatabase
import com.example.m3uplayer.data.db.FavoriteItemEntity
import com.example.m3uplayer.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val playlist: M3UPlaylist? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val favoriteUris: Set<String> = emptySet()
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = M3URepository(app)
    private val db = AppDatabase.get(app)
    private var controller: MediaController? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        connectController()
        viewModelScope.launch {
            db.playlistDao().observeFavorites().collect { favs ->
                _uiState.value = _uiState.value.copy(favoriteUris = favs.map { it.uri }.toSet())
            }
        }
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: -1
            _uiState.value = _uiState.value.copy(currentIndex = index)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // اگر یک آیتم لینکش خراب بود، خطا نشان بده و خودکار برو سراغ بعدی
            _uiState.value = _uiState.value.copy(errorMessage = "خطا در پخش: ${error.errorCodeName}")
            controller?.seekToNextMediaItem()
        }
    }

    // ---------- باز کردن پلی‌لیست ----------

    fun openLocalM3U(uri: Uri) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        when (val result = repository.loadFromLocalUri(uri)) {
            is com.example.m3uplayer.data.LoadResult.Success -> applyPlaylist(result.playlist)
            is com.example.m3uplayer.data.LoadResult.Error ->
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
        }
    }

    fun openRemoteM3U(url: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        when (val result = repository.loadFromUrl(url)) {
            is com.example.m3uplayer.data.LoadResult.Success -> applyPlaylist(result.playlist)
            is com.example.m3uplayer.data.LoadResult.Error ->
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
        }
    }

    private fun applyPlaylist(playlist: M3UPlaylist, startIndex: Int = 0) {
        val items = playlist.items.map { MediaItem.fromUri(it.uri) }
        controller?.apply {
            setMediaItems(items, startIndex, 0L)
            prepare()
            playWhenReady = true
        }
        _uiState.value = _uiState.value.copy(playlist = playlist, currentIndex = startIndex)
    }

    // ---------- کنترل پخش ----------

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun playItemAt(index: Int) {
        controller?.seekTo(index, 0L)
        controller?.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun setSpeed(speed: Float) {
        controller?.setPlaybackParameters(PlaybackParameters(speed))
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun toggleRepeat() {
        val next = when (_uiState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
        _uiState.value = _uiState.value.copy(repeatMode = next)
    }

    fun toggleShuffle() {
        val enabled = !_uiState.value.shuffleEnabled
        controller?.shuffleModeEnabled = enabled
        _uiState.value = _uiState.value.copy(shuffleEnabled = enabled)
    }

    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L
    fun durationMs(): Long = controller?.duration?.coerceAtLeast(0) ?: 0L

    /** برای اتصال PlayerView (نمایش تصویر ویدیو) در لایه UI */
    fun playerOrNull(): Player? = controller

    // ---------- علاقه‌مندی‌ها ----------

    fun toggleFavorite(item: M3UItem) = viewModelScope.launch {
        if (_uiState.value.favoriteUris.contains(item.uri)) {
            db.playlistDao().removeFavorite(item.uri)
        } else {
            db.playlistDao().addFavorite(
                FavoriteItemEntity(item.uri, item.title, item.logoUrl, item.groupTitle)
            )
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
