package app.marlboroadvance.mpvex.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.marlboroadvance.mpvex.data.music.MusicLibraryRepository
import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.PlaybackQueue
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.playback.MusicPlayerController
import app.marlboroadvance.mpvex.playback.MusicPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val playerController = MusicPlayerController.getInstance(application)

  private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Loading)
  val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

  /** Live state of the music player, mirrored from the playback service. */
  val playerState: StateFlow<MusicPlayerState> = playerController.state

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

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        MusicLibraryViewModel(application)
      }
    }
  }
}
