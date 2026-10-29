package app.marlboroadvance.mpvex.ui.browser.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

/**
 * Data for the Profile dashboard. Everything shown here comes from data the
 * app already records: the recently-played history, the user's playlists and
 * the player's saved resume positions. No usage statistics are shown yet,
 * because the app does not record watched time or play counts.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

  private val recentlyPlayedRepository: RecentlyPlayedRepository by inject(
    RecentlyPlayedRepository::class.java,
  )
  private val playlistRepository: PlaylistRepository by inject(PlaylistRepository::class.java)
  private val database: MpvExDatabase by inject(MpvExDatabase::class.java)

  data class PlaylistSummary(
    val id: Int,
    val name: String,
    val itemCount: Int,
  )

  data class UiState(
    val isLoading: Boolean = true,
    val recent: List<RecentlyPlayedEntity> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val continueWatching: List<PlaybackStateEntity> = emptyList(),
  )

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        recentlyPlayedRepository.observeRecentlyPlayed(limit = 50),
        playlistRepository.observeAllPlaylists(),
      ) { recent, playlists -> recent to playlists }
        .collect { (recent, playlists) ->
          val summaries = playlists.map { playlist ->
            PlaylistSummary(
              id = playlist.id,
              name = playlist.name,
              itemCount = playlistRepository.getPlaylistItemCount(playlist.id),
            )
          }
          val continueWatching = runCatching {
            database.videoDataDao().getAllPlaybackStates()
          }.getOrDefault(emptyList())
            .filter { it.lastPosition > 0 && it.timeRemaining > 0 }
            .sortedBy { it.mediaTitle.lowercase() }
            .take(10)
          _uiState.value = UiState(
            isLoading = false,
            recent = recent,
            playlists = summaries,
            continueWatching = continueWatching,
          )
        }
    }
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
      initializer { ProfileViewModel(application) }
    }
  }
}
