package app.marlboroadvance.mpvex.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.marlboroadvance.mpvex.domain.music.PlaybackQueue
import app.marlboroadvance.mpvex.domain.music.Song
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** MediaItem extras key carrying the song's MediaStore album id. */
private const val EXTRA_ALBUM_ID = "app.marlboroadvance.mpvex.playback.ALBUM_ID"

/** One entry of the playback queue, as shown on the queue screen (Phase E). */
data class QueueItem(
  val songId: Long,
  val title: String,
  val artist: String,
  val albumId: Long?,
)

/** A snapshot of the music player's state for the UI. */
data class MusicPlayerState(
  val isConnected: Boolean = false,
  val currentSongId: Long? = null,
  val title: String? = null,
  val artist: String? = null,
  val albumId: Long? = null,
  val currentIndex: Int = -1,
  val queueSize: Int = 0,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val hasNext: Boolean = false,
  val hasPrevious: Boolean = false,
  val shuffleEnabled: Boolean = false,
  val repeatMode: Int = Player.REPEAT_MODE_OFF,
  val queueItems: List<QueueItem> = emptyList(),
)

/**
 * UI-side handle to [MusicPlaybackService] (Phase D).
 *
 * A single process-wide instance connects to the service's media session
 * through a MediaController and mirrors the player state as a StateFlow.
 * Playback itself lives in the service: this controller can be recreated
 * (tab switches, activity recreation) without interrupting the music.
 */
class MusicPlayerController private constructor(context: Context) {

  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var controller: MediaController? = null
  private var pendingQueue: PendingQueue? = null

  /** MediaStore durations of the current queue, parallel to the player's items. */
  private var queueDurationsMs: List<Long> = emptyList()

  private val _state = MutableStateFlow(MusicPlayerState())
  val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

  private data class PendingQueue(
    val songs: List<Song>,
    val startIndex: Int,
    val artworkUriByAlbumId: Map<Long, String>,
  )

  private val playerListener = object : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
      refresh(player)
    }
  }

  /** Connects to the playback service. Safe to call repeatedly. */
  fun connect() {
    if (controllerFuture != null) return
    val future = try {
      val token = SessionToken(
        appContext,
        ComponentName(appContext, MusicPlaybackService::class.java),
      )
      MediaController.Builder(appContext, token).buildAsync()
    } catch (e: Exception) {
      // A session configuration problem must never crash the Music tab;
      // leave the player disconnected so a later connect() can retry.
      return
    }
    controllerFuture = future
    Futures.addCallback(
      future,
      object : FutureCallback<MediaController> {
        override fun onSuccess(result: MediaController) {
          controller = result
          result.addListener(playerListener)
          startProgressUpdates(result)
          pendingQueue?.let { pending ->
            playQueueInternal(result, pending)
            pendingQueue = null
          }
          refresh(result)
        }

        override fun onFailure(t: Throwable) {
          controllerFuture = null
        }
      },
      ContextCompat.getMainExecutor(appContext),
    )
  }

  /**
   * Starts playing [songs] (a library list in display order) from
   * [startIndex]. When the controller is not connected yet, the request is
   * queued and runs as soon as the connection is ready.
   */
  fun playQueue(
    songs: List<Song>,
    startIndex: Int,
    artworkUriByAlbumId: Map<Long, String>,
  ) {
    if (songs.isEmpty()) return
    val pending = PendingQueue(songs, startIndex, artworkUriByAlbumId)
    val active = controller
    if (active == null) {
      pendingQueue = pending
      connect()
    } else {
      playQueueInternal(active, pending)
    }
  }

  fun togglePlayPause() {
    val player = controller ?: return
    if (player.isPlaying) {
      player.pause()
    } else {
      player.play()
    }
  }

  fun playNext() {
    controller?.seekToNextMediaItem()
  }

  fun playPrevious() {
    controller?.seekToPreviousMediaItem()
  }

  fun seekTo(positionMs: Long) {
    controller?.seekTo(positionMs.coerceAtLeast(0L))
  }

  /** Turns shuffle on or off (Phase E). */
  fun setShuffleEnabled(enabled: Boolean) {
    val player = controller ?: return
    player.shuffleModeEnabled = enabled
    refresh(player)
  }

  /** Cycles repeat: off -> all -> one -> off (Phase E). */
  fun cycleRepeatMode() {
    val player = controller ?: return
    player.repeatMode = when (player.repeatMode) {
      Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
      Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
      else -> Player.REPEAT_MODE_OFF
    }
    refresh(player)
  }

  /**
   * Moves the queue item at [fromIndex] to [toIndex] (Phase E). The
   * parallel MediaStore duration list moves with it, so the seek-bar
   * duration fallback from Phase D keeps matching the playing song.
   */
  fun moveQueueItem(fromIndex: Int, toIndex: Int) {
    val player = controller ?: return
    if (fromIndex !in 0 until player.mediaItemCount) return
    if (toIndex !in 0 until player.mediaItemCount) return
    if (fromIndex == toIndex) return
    player.moveMediaItem(fromIndex, toIndex)
    queueDurationsMs = PlaybackQueue.moved(queueDurationsMs, fromIndex, toIndex)
    refresh(player)
  }

  /** Removes the queue item at [index] (Phase E); durations stay in sync. */
  fun removeQueueItem(index: Int) {
    val player = controller ?: return
    if (index !in 0 until player.mediaItemCount) return
    player.removeMediaItem(index)
    queueDurationsMs = PlaybackQueue.removedAt(queueDurationsMs, index)
    refresh(player)
  }

  /** Starts playing the queue item at [index] (Phase E). */
  fun playQueueItem(index: Int) {
    val player = controller ?: return
    if (index !in 0 until player.mediaItemCount) return
    player.seekTo(index, 0L)
    player.play()
    refresh(player)
  }

  private fun playQueueInternal(player: Player, pending: PendingQueue) {
    val items = pending.songs.map { song ->
      song.toMediaItem(pending.artworkUriByAlbumId[song.albumId])
    }
    queueDurationsMs = pending.songs.map { it.durationMs }
    player.setMediaItems(items, pending.startIndex, 0L)
    player.prepare()
    player.play()
  }

  private fun startProgressUpdates(player: Player) {
    scope.launch {
      while (true) {
        if (player.isPlaying) {
          refresh(player)
        }
        delay(PROGRESS_UPDATE_INTERVAL_MS)
      }
    }
  }

  private fun refresh(player: Player) {
    val item = player.currentMediaItem
    val metadata = item?.mediaMetadata
    val extras = metadata?.extras
    _state.value = MusicPlayerState(
      isConnected = true,
      currentSongId = item?.mediaId?.toLongOrNull(),
      title = metadata?.title?.toString(),
      artist = metadata?.artist?.toString(),
      albumId = extras
        ?.takeIf { it.containsKey(EXTRA_ALBUM_ID) }
        ?.getLong(EXTRA_ALBUM_ID),
      currentIndex = player.currentMediaItemIndex,
      queueSize = player.mediaItemCount,
      isPlaying = player.isPlaying,
      isBuffering = player.playbackState == Player.STATE_BUFFERING,
      positionMs = player.currentPosition.coerceAtLeast(0L),
      // Some files (e.g. MP3s without a length header) do not reveal their
      // duration to the player. Fall back to the duration MediaStore knows,
      // so the seek bar still works for those songs.
      durationMs = player.duration
        .takeUnless { it == C.TIME_UNSET || it <= 0L }
        ?: queueDurationsMs.getOrNull(player.currentMediaItemIndex)?.takeIf { it > 0L }
        ?: 0L,
      hasNext = player.hasNextMediaItem(),
      hasPrevious = player.hasPreviousMediaItem(),
      shuffleEnabled = player.shuffleModeEnabled,
      repeatMode = player.repeatMode,
      queueItems = (0 until player.mediaItemCount).map { index ->
        val queueItem = player.getMediaItemAt(index)
        val queueMetadata = queueItem.mediaMetadata
        QueueItem(
          songId = queueItem.mediaId.toLongOrNull() ?: -1L,
          title = queueMetadata.title?.toString() ?: "Unknown title",
          artist = queueMetadata.artist?.toString() ?: "Unknown artist",
          albumId = queueMetadata.extras
            ?.takeIf { it.containsKey(EXTRA_ALBUM_ID) }
            ?.getLong(EXTRA_ALBUM_ID),
        )
      },
    )
  }

  companion object {
    private const val PROGRESS_UPDATE_INTERVAL_MS = 500L

    @Volatile
    private var instance: MusicPlayerController? = null

    fun getInstance(context: Context): MusicPlayerController =
      instance ?: synchronized(this) {
        instance ?: MusicPlayerController(context).also { instance = it }
      }
  }
}

private fun Song.toMediaItem(artworkUri: String?): MediaItem {
  val metadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artist)
    .setAlbumTitle(album)
    .setExtras(Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) })
    .apply {
      if (artworkUri != null) {
        setArtworkUri(Uri.parse(artworkUri))
      }
    }
    .build()
  return MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(metadata)
    .build()
}
