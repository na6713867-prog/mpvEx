package app.marlboroadvance.mpvex.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.data.music.MusicLibraryFilter
import app.marlboroadvance.mpvex.playback.MusicPlayerController
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState

/**
 * The Now Playing screen (Phase D): artwork, title, artist, seek bar and
 * transport controls for the song playing in MusicPlaybackService.
 *
 * This is intentionally the standard artwork-based layout; the visual
 * redesign the owner described from reference screenshots is a later phase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
  val context = LocalContext.current
  val controller = remember { MusicPlayerController.getInstance(context) }
  val state by controller.state.collectAsState()
  val bottomPadding = LocalNavigationBarHeight.current + 16.dp

  LaunchedEffect(Unit) {
    controller.connect()
  }

  BackHandler(onBack = onClose)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Now Playing") },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { paddingValues ->
    if (state.currentSongId == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        EmptyState(
          icon = Icons.Filled.MusicNote,
          title = "Nothing is playing",
          message = "Pick a song from your library to start listening.",
        )
      }
      return@Scaffold
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 24.dp)
        .padding(bottom = bottomPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(8.dp))

      AlbumArtwork(
        albumId = state.albumId ?: -1L,
        targetSizePx = 1024,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(16.dp)),
      )

      Spacer(Modifier.height(24.dp))

      Text(
        text = state.title ?: "Unknown title",
        style = MaterialTheme.typography.titleLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = state.artist ?: "Unknown artist",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(24.dp))

      var sliderPosition by remember(state.currentSongId) { mutableStateOf<Float?>(null) }
      val duration = state.durationMs.coerceAtLeast(0L)
      Slider(
        value = sliderPosition ?: state.positionMs.toFloat(),
        onValueChange = { sliderPosition = it },
        onValueChangeFinished = {
          sliderPosition?.let { controller.seekTo(it.toLong()) }
          sliderPosition = null
        },
        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
        enabled = duration > 0L,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = MusicLibraryFilter.formatDuration(
            (sliderPosition?.toLong() ?: state.positionMs).coerceAtLeast(0L),
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = MusicLibraryFilter.formatDuration(duration),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(Modifier.height(16.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        IconButton(
          onClick = { controller.playPrevious() },
          enabled = state.hasPrevious,
          modifier = Modifier.size(56.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "Previous",
            modifier = Modifier.size(36.dp),
          )
        }

        FilledIconButton(
          onClick = { controller.togglePlayPause() },
          modifier = Modifier.size(72.dp),
        ) {
          if (state.isBuffering) {
            CircularProgressIndicator(
              modifier = Modifier.size(32.dp),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              strokeWidth = 3.dp,
            )
          } else {
            Icon(
              imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (state.isPlaying) "Pause" else "Play",
              modifier = Modifier.size(40.dp),
            )
          }
        }

        IconButton(
          onClick = { controller.playNext() },
          enabled = state.hasNext,
          modifier = Modifier.size(56.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Next",
            modifier = Modifier.size(36.dp),
          )
        }
      }

      if (state.queueSize > 0 && state.currentIndex >= 0) {
        Spacer(Modifier.height(16.dp))
        Text(
          text = "${state.currentIndex + 1} / ${state.queueSize}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
