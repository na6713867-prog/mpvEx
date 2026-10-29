package app.marlboroadvance.mpvex.ui.browser.profile

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * The Profile tab. Its dashboard shows only data the app already records
 * (viewing history, playlists, resume positions). The Recents and Playlists
 * tiles open the app's existing screens in place, so their behavior is
 * exactly the same as before they moved off the bottom bar.
 */
@Serializable
object ProfileScreen : Screen {

  private enum class Section { Recents, Playlists }

  @Composable
  override fun Content() {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
      factory = ProfileViewModel.factory(context.applicationContext as Application),
    )
    val state by viewModel.uiState.collectAsState()

    var section by remember { mutableStateOf<Section?>(null) }
    BackHandler(enabled = section != null) { section = null }

    when (section) {
      Section.Recents -> RecentlyPlayedScreen.Content()
      Section.Playlists -> PlaylistScreen.Content()
      null -> Dashboard(
        state = state,
        onOpenRecents = { section = Section.Recents },
        onOpenPlaylists = { section = Section.Playlists },
      )
    }
  }

  @Composable
  private fun Dashboard(
    state: ProfileViewModel.UiState,
    onOpenRecents: () -> Unit,
    onOpenPlaylists: () -> Unit,
  ) {
    if (state.isLoading) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        CircularProgressIndicator()
      }
      return
    }

    val hubCount = state.recent.size + state.playlists.size

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
        bottom = LocalNavigationBarHeight.current + 16.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      item {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AccountCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(8.dp),
            )
          }
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
              text = "Profile",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "$hubCount items in hub",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          HubTile(
            icon = Icons.Filled.History,
            count = state.recent.size,
            label = "Recents",
            onClick = onOpenRecents,
            modifier = Modifier.weight(1f),
          )
          HubTile(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            count = state.playlists.size,
            label = "Playlists",
            onClick = onOpenPlaylists,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SectionHeader(
            title = "Recently Played",
            count = state.recent.size,
            onViewAll = onOpenRecents,
          )
          if (state.recent.isEmpty()) {
            EmptyState(
              icon = Icons.Filled.History,
              title = "No viewing history yet",
              message = "Videos you watch will appear here.",
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
      items(state.recent.take(8), key = { it.id }) { entity ->
        RecentRow(entity)
      }

      if (state.continueWatching.isNotEmpty()) {
        item {
          Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        items(state.continueWatching, key = { it.mediaTitle }) { playback ->
          ContinueWatchingRow(playback)
        }
      }

      if (state.playlists.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
              title = "Playlists",
              count = state.playlists.size,
              onViewAll = onOpenPlaylists,
            )
          }
        }
        items(state.playlists, key = { it.id }) { playlist ->
          PlaylistRow(
            name = playlist.name,
            itemCount = playlist.itemCount,
            onClick = onOpenPlaylists,
          )
        }
      }
    }
  }

  @Composable
  private fun HubTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Card(
      onClick = onClick,
      modifier = modifier,
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      ),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(9.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = Icons.Filled.ChevronRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  @Composable
  private fun SectionHeader(
    title: String,
    count: Int,
    onViewAll: () -> Unit,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (count > 0) {
        Spacer(Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onViewAll) {
        Text("View all")
        Icon(
          imageVector = Icons.Filled.ChevronRight,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }

  @Composable
  private fun RecentRow(entity: RecentlyPlayedEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = entity.videoTitle?.takeIf { it.isNotBlank() } ?: entity.fileName,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      val date = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
        .format(Date(entity.timestamp))
      val subtitle = if (entity.duration > 0) {
        "$date • ${formatDuration(entity.duration)}"
      } else {
        date
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }

  @Composable
  private fun ContinueWatchingRow(playback: PlaybackStateEntity) {
    val total = playback.lastPosition + playback.timeRemaining
    val progress = if (total > 0) {
      playback.lastPosition.toFloat() / total.toFloat()
    } else {
      0f
    }
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = playback.mediaTitle,
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "${formatRemaining(playback.timeRemaining)} left",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(50)),
      )
    }
  }

  @Composable
  private fun PlaylistRow(
    name: String,
    itemCount: Int,
    onClick: () -> Unit,
  ) {
    Card(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      ),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
          text = name,
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = if (itemCount == 1) "1 video" else "$itemCount videos",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
  }

  private fun formatRemaining(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
      hours > 0 -> "${hours}h ${minutes}m"
      minutes > 0 -> "${minutes}m"
      else -> "${safe}s"
    }
  }
}
