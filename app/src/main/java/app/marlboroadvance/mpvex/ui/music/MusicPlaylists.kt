package app.marlboroadvance.mpvex.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.data.music.MusicPlaylistEntity
import app.marlboroadvance.mpvex.data.music.MusicPlaylistItemEntity
import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import kotlinx.coroutines.flow.Flow

/**
 * The Playlists tab of the Music section (Phase E): a built-in Favorites
 * card on top, then the user's own music playlists — following the
 * structure of the owner's reference screenshots. These playlists live in
 * the separate MusicDatabase and have nothing to do with video playlists.
 */
@Composable
fun PlaylistsTab(
  playlists: List<MusicPlaylistEntity>,
  playlistCounts: Map<Long, Int>,
  favoritesCount: Int,
  onFavoritesClick: () -> Unit,
  onPlaylistClick: (MusicPlaylistEntity) -> Unit,
  onCreatePlaylist: (String) -> Unit,
  onRenamePlaylist: (Long, String) -> Unit,
  onDeletePlaylist: (Long) -> Unit,
  bottomPadding: Dp,
) {
  var createOpen by remember { mutableStateOf(false) }
  var renameTarget by remember { mutableStateOf<MusicPlaylistEntity?>(null) }
  var deleteTarget by remember { mutableStateOf<MusicPlaylistEntity?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    item {
      FavoritesCard(
        favoritesCount = favoritesCount,
        onClick = onFavoritesClick,
      )
    }
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${playlists.size} Playlists",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { createOpen = true }) {
          Icon(Icons.Filled.Add, contentDescription = "Create playlist")
        }
      }
    }
    if (playlists.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No playlists yet. Tap + to create one, then add songs " +
              "from the Songs tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      items(playlists, key = { it.id }) { playlist ->
        PlaylistRow(
          playlist = playlist,
          songCount = playlistCounts[playlist.id] ?: 0,
          onClick = { onPlaylistClick(playlist) },
          onRename = { renameTarget = playlist },
          onDelete = { deleteTarget = playlist },
        )
      }
    }
  }

  if (createOpen) {
    PlaylistNameDialog(
      title = "New playlist",
      initialName = "",
      confirmLabel = "Create",
      onDismiss = { createOpen = false },
      onConfirm = { name ->
        onCreatePlaylist(name)
        createOpen = false
      },
    )
  }

  renameTarget?.let { playlist ->
    PlaylistNameDialog(
      title = "Rename playlist",
      initialName = playlist.name,
      confirmLabel = "Rename",
      onDismiss = { renameTarget = null },
      onConfirm = { name ->
        onRenamePlaylist(playlist.id, name)
        renameTarget = null
      },
    )
  }

  deleteTarget?.let { playlist ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Delete playlist?") },
      text = {
        Text(
          "“${playlist.name}” will be deleted. The songs themselves " +
            "stay on your device.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeletePlaylist(playlist.id)
            deleteTarget = null
          },
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun FavoritesCard(favoritesCount: Int, onClick: () -> Unit) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(32.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Favorites",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text = songCountText(favoritesCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun PlaylistRow(
  playlist: MusicPlaylistEntity,
  songCount: Int,
  onClick: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.QueueMusic,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(28.dp),
    )
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = playlist.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = songCountText(songCount),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Box {
      IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Playlist options")
      }
      DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
          text = { Text("Rename") },
          onClick = {
            menuOpen = false
            onRename()
          },
        )
        DropdownMenuItem(
          text = { Text("Delete") },
          onClick = {
            menuOpen = false
            onDelete()
          },
        )
      }
    }
  }
}

@Composable
private fun PlaylistNameDialog(
  title: String,
  initialName: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(name) },
        enabled = name.isNotBlank(),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

/** Chooser shown when adding a song to a playlist (Phase E). */
@Composable
fun AddToPlaylistDialog(
  playlists: List<MusicPlaylistEntity>,
  onDismiss: () -> Unit,
  onPick: (MusicPlaylistEntity) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add to playlist") },
    text = {
      if (playlists.isEmpty()) {
        Text("No playlists yet. Create one from the Playlists tab first.")
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          items(playlists, key = { it.id }) { playlist ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(playlist) }
                .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.width(12.dp))
              Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

/**
 * One music playlist's songs (Phase E), in stored order. Tapping a song
 * plays the whole playlist as the queue context; the cross removes the
 * song from this playlist only.
 */
@Composable
fun PlaylistDetailContent(
  itemsFlow: Flow<List<MusicPlaylistItemEntity>>,
  library: MusicLibrary,
  onSongClick: (Song, List<Song>) -> Unit,
  onRemoveSong: (Long) -> Unit,
  bottomPadding: Dp,
) {
  val items by itemsFlow.collectAsState(initial = emptyList())
  val songsById = remember(library) { library.songs.associateBy { it.id } }
  val songs = items.mapNotNull { songsById[it.songId] }

  if (items.isEmpty()) {
    EmptyState(
      icon = Icons.AutoMirrored.Filled.QueueMusic,
      title = "No songs yet",
      message = "Add songs with the playlist button on any song in your library.",
    )
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    item {
      Text(
        text = songCountText(songs.size),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      )
    }
    items(songs, key = { it.id }) { song ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        SongRow(
          song = song,
          onClick = { onSongClick(song, songs) },
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onRemoveSong(song.id) }) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove from playlist",
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(Modifier.width(4.dp))
      }
    }
  }
}

/** The built-in Favorites list (Phase E): the user's favorite songs. */
@Composable
fun FavoritesDetailContent(
  library: MusicLibrary,
  favoriteIds: Set<Long>,
  onSongClick: (Song, List<Song>) -> Unit,
  onToggleFavorite: (Song) -> Unit,
  bottomPadding: Dp,
) {
  val songs = remember(library, favoriteIds) {
    library.songs
      .filter { it.id in favoriteIds }
      .sortedBy { it.title.lowercase() }
  }
  if (songs.isEmpty()) {
    EmptyState(
      icon = Icons.Filled.Favorite,
      title = "No favorites yet",
      message = "Tap the heart on any song to keep it here.",
    )
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    items(songs, key = { it.id }) { song ->
      SongRow(
        song = song,
        onClick = { onSongClick(song, songs) },
        isFavorite = true,
        onToggleFavorite = { onToggleFavorite(song) },
      )
    }
  }
}
