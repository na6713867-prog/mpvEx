package app.marlboroadvance.mpvex.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.marlboroadvance.mpvex.data.music.MusicLibraryRepository
import app.marlboroadvance.mpvex.data.music.MusicPlaylistEntity
import app.marlboroadvance.mpvex.data.music.MusicPlaylistItemEntity
import app.marlboroadvance.mpvex.data.music.MusicPlaylistRepository
import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.PlaybackQueue
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.playback.MusicPlayerController
import app.marlboroadvance.mpvex.playback.MusicPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state of the music library section. */
sealed interface MusicUiState {
  data object Loading : MusicUiState

  data object PermissionDenied : MusicUiState

  data class Loaded(val library: MusicLibrary) : MusicUiState
}

class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = MusicLibraryRepository(application)
  private val playlistRepository = MusicPlaylistRepository(application)
  private val playerController = MusicPlayerController.getInstance(application)

  private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Loading)
  val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

  /** Live state of the music player, mirrored from the playback service. */
  val playerState: StateFlow<MusicPlayerState> = playerController.state

  /** Music playlists (Phase E), from the separate MusicDatabase. */
  val musicPlaylists: StateFlow<List<MusicPlaylistEntity>> =
    playlistRepository.playlists.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(STATE_SHARING_TIMEOUT_MS),
      emptyList(),
    )

  /** Playlist id -> song count (Phase E). */
  val musicPlaylistCounts: StateFlow<Map<Long, Int>> =
    playlistRepository.itemCounts
      .map { counts -> counts.associate { it.playlistId to it.itemCount } }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATE_SHARING_TIMEOUT_MS),
        emptyMap(),
      )

  /** Ids of the user's favorite songs (Phase E). */
  val favoriteSongIds: StateFlow<Set<Long>> =
    playlistRepository.favorites
      .map { favorites -> favorites.map { it.songId }.toSet() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATE_SHARING_TIMEOUT_MS),
        emptySet(),
      )

  init {
    playerController.connect()
  }

  /**
   * (Re)loads the library from MediaStore. When content is already shown the
   * reload happens silently so the visible list does not flicker.
   */
  fun load() {
    val silent = _uiState.value is MusicUiState.Loaded
    if (!silent) {
      _uiState.value = MusicUiState.Loading
    }
    viewModelScope.launch {
      if (!repository.hasAudioPermission()) {
        _uiState.value = MusicUiState.PermissionDenied
        return@launch
      }
      val library = withContext(Dispatchers.IO) { repository.loadLibrary() }
      _uiState.value = MusicUiState.Loaded(library)
      // Drop playlist/favorite entries for songs that left the device.
      val validIds = library.songs.map { it.id }.toSet()
      withContext(Dispatchers.IO) { playlistRepository.cleanup(validIds) }
    }
  }

  /**
   * Starts playback of [song]. The queue continues through [contextSongs] —
   * the visible list the song was tapped in (Songs tab, an album, an artist,
   * a folder, or search results), in display order.
   */
  fun playSong(song: Song, contextSongs: List<Song>) {
    val plan = PlaybackQueue.plan(contextSongs, song) ?: return
    val artworkUriByAlbumId = (_uiState.value as? MusicUiState.Loaded)
      ?.library
      ?.albums
      ?.associate { album -> album.id to album.artworkUri }
      ?: emptyMap()
    playerController.playQueue(plan.songs, plan.startIndex, artworkUriByAlbumId)
  }

  /** The stored items of one music playlist, in playlist order (Phase E). */
  fun playlistItems(playlistId: Long): Flow<List<MusicPlaylistItemEntity>> =
    playlistRepository.items(playlistId)

  fun createPlaylist(name: String) {
    if (name.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) { playlistRepository.createPlaylist(name) }
  }

  fun renamePlaylist(id: Long, name: String) {
    if (name.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) { playlistRepository.renamePlaylist(id, name) }
  }

  fun deletePlaylist(id: Long) {
    viewModelScope.launch(Dispatchers.IO) { playlistRepository.deletePlaylist(id) }
  }

  fun addSongToPlaylist(playlistId: Long, song: Song) {
    viewModelScope.launch(Dispatchers.IO) { playlistRepository.addSong(playlistId, song) }
  }

  fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.removeSong(playlistId, songId)
    }
  }

  fun setFavorite(song: Song, favorite: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.setFavorite(song.id, favorite)
    }
  }

  companion object {
    private const val STATE_SHARING_TIMEOUT_MS = 5_000L

    fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        MusicLibraryViewModel(application)
      }
    }
  }
}
