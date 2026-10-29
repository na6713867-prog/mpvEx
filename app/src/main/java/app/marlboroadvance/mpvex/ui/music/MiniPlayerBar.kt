package app.marlboroadvance.mpvex.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.playback.MusicPlayerState

/**
 * Height the mini-player adds above the pill navigation bar while it is
 * visible (60.dp bar + 6.dp padding top and bottom). MainScreen adds this
 * to the content bottom padding so lists are never hidden behind it.
 */
val MiniPlayerBarExtraHeight = 72.dp

/**
 * The persistent mini-player (Phase E): shown above the bottom pill bar on
 * every tab while a song is loaded. Tapping the bar opens Now Playing; the
 * button toggles play/pause. It is a plain Material surface — no blur or
 * glass effects, per the owner's UI preferences.
 */
@Composable
fun MiniPlayerBar(
  state: MusicPlayerState,
  onPlayPause: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      shadowElevation = 6.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AlbumArtwork(
          albumId = state.albumId ?: -1L,
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.title ?: "Unknown title",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = state.artist ?: "Unknown artist",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = onPlayPause) {
          if (state.isBuffering) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (state.isPlaying) "Pause" else "Play",
              modifier = Modifier.size(28.dp),
            )
          }
        }
      }
    }
  }
}
