package app.marlboroadvance.mpvex.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
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
import app.marlboroadvance.mpvex.data.music.MusicLibraryFilter
import app.marlboroadvance.mpvex.domain.music.Album
import app.marlboroadvance.mpvex.domain.music.Artist
import app.marlboroadvance.mpvex.domain.music.MusicFolder
import app.marlboroadvance.mpvex.domain.music.Song

internal fun songCountText(count: Int): String = if (count == 1) "1 song" else "$count songs"

@Composable
fun SongRow(
  song: Song,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isFavorite: Boolean = false,
  onToggleFavorite: (() -> Unit)? = null,
  onAddToPlaylist: (() -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AlbumArtwork(
      albumId = song.albumId,
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(8.dp)),
    )
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "${song.artist} • ${song.album}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.width(8.dp))
    Text(
      text = MusicLibraryFilter.formatDuration(song.durationMs),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Phase E: optional playlist / favorite actions. Callers that pass no
    // handlers keep the exact Phase C/D row.
    if (onAddToPlaylist != null) {
      IconButton(onClick = onAddToPlaylist) {
        Icon(
          imageVector = Icons.Filled.PlaylistAdd,
          contentDescription = "Add to playlist",
        )
      }
    }
    if (onToggleFavorite != null) {
      IconButton(onClick = onToggleFavorite) {
        Icon(
          imageVector = if (isFavorite) {
            Icons.Filled.Favorite
          } else {
            Icons.Filled.FavoriteBorder
          },
          contentDescription = if (isFavorite) {
            "Remove from favorites"
          } else {
            "Add to favorites"
          },
          tint = if (isFavorite) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }
    }
  }
}

@Composable
fun AlbumCard(
  album: Album,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(8.dp),
  ) {
    AlbumArtwork(
      albumId = album.id,
      targetSizePx = 512,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp)),
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = album.title,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = album.artist,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = songCountText(album.songCount),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun ArtistRow(
  artist: Artist,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      modifier = Modifier.size(48.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp),
      )
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = artist.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "${artist.albumCount} albums • ${songCountText(artist.songCount)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun FolderRow(
  folder: MusicFolder,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      modifier = Modifier.size(48.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Folder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp),
      )
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folder.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = songCountText(folder.songCount),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
