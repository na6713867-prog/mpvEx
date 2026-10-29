package app.marlboroadvance.mpvex.ui.music

import android.app.Application
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.data.music.AudioPermission
import app.marlboroadvance.mpvex.data.music.MusicLibraryFilter
import app.marlboroadvance.mpvex.data.music.SongSort
import app.marlboroadvance.mpvex.domain.music.Album
import app.marlboroadvance.mpvex.domain.music.Artist
import app.marlboroadvance.mpvex.domain.music.MusicFolder
import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.playback.MusicPlayerState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import kotlinx.serialization.Serializable

/** The Music tab: local music library (songs, albums, artists, folders). */
@Serializable
object MusicLibraryScreen : Screen {

  @Composable
  override fun Content() {
    Content(onOpenNowPlaying = {})
  }

  /**
   * [onOpenNowPlaying] is supplied by MainScreen (Phase E): Now Playing is
   * hosted there so the mini-player on every tab can open it. The no-arg
   * override keeps the Screen interface contract.
   */
  @Composable
  fun Content(onOpenNowPlaying: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MusicLibraryViewModel = viewModel(
      factory = MusicLibraryViewModel.factory(context.applicationContext as Application),
    )
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      if (granted) {
        viewModel.load()
      }
    }

    LaunchedEffect(Unit) {
      viewModel.load()
    }

    // Refresh when returning to the app, e.g. after granting the permission
    // from the system Settings app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          viewModel.load()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val state = uiState) {
      MusicUiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }

      MusicUiState.PermissionDenied -> {
        PermissionRequiredState(
          onAllowAccess = {
            permissionLauncher.launch(
              AudioPermission.requiredPermission(Build.VERSION.SDK_INT),
            )
          },
        )
      }

      is MusicUiState.Loaded -> {
        MusicLibraryContent(
          library = state.library,
          playerState = playerState,
          onSongPlay = viewModel::playSong,
          onOpenNowPlaying = onOpenNowPlaying,
        )
      }
    }
  }
}

private sealed interface MusicDetail {
  data class AlbumDetail(val album: Album) : MusicDetail

  data class ArtistDetail(val artist: Artist) : MusicDetail

  data class FolderDetail(val folder: MusicFolder) : MusicDetail

  val title: String
    get() = when (this) {
      is AlbumDetail -> album.title
      is ArtistDetail -> artist.name
      is FolderDetail -> folder.name
    }
}

private val TAB_TITLES = listOf("Songs", "Albums", "Artists", "Folders")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicLibraryContent(
  library: MusicLibrary,
  playerState: MusicPlayerState,
  onSongPlay: (Song, List<Song>) -> Unit,
  onOpenNowPlaying: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var query by rememberSaveable { mutableStateOf("") }
  var searchActive by rememberSaveable { mutableStateOf(false) }
  var songSort by rememberSaveable { mutableStateOf(SongSort.TITLE) }
  var ascending by rememberSaveable { mutableStateOf(true) }
  var sortMenuOpen by remember { mutableStateOf(false) }
  var detail by remember { mutableStateOf<MusicDetail?>(null) }

  val bottomPadding = LocalNavigationBarHeight.current + 16.dp

  // Tapping a song plays it and opens Now Playing, as in Phase D; since
  // Phase E the screen itself is hosted by MainScreen (see Content above).
  val onSongClick: (Song, List<Song>) -> Unit = { song, contextSongs ->
    onSongPlay(song, contextSongs)
    onOpenNowPlaying()
  }

  BackHandler(enabled = detail != null) {
    detail = null
  }

  Scaffold(
    topBar = {
      Column {
        TopAppBar(
          title = { Text(detail?.title ?: "Music") },
          navigationIcon = {
            if (detail != null) {
              IconButton(onClick = { detail = null }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            }
          },
          actions = {
            if (detail == null && playerState.currentSongId != null) {
              IconButton(onClick = onOpenNowPlaying) {
                Icon(
                  imageVector = Icons.Filled.MusicNote,
                  contentDescription = "Now playing",
                )
              }
            }
            if (detail == null && library.songs.isNotEmpty()) {
              IconButton(
                onClick = {
                  searchActive = !searchActive
                  if (!searchActive) {
                    query = ""
                  }
                },
              ) {
                Icon(
                  imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                  contentDescription = "Search",
                )
              }
              Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                  Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                SortMenu(
                  expanded = sortMenuOpen,
                  onDismiss = { sortMenuOpen = false },
                  selectedTab = selectedTab,
                  songSort = songSort,
                  onSongSortChange = { songSort = it },
                  ascending = ascending,
                  onAscendingChange = { ascending = it },
                )
              }
            }
          },
        )
        if (detail == null && library.songs.isNotEmpty()) {
          if (searchActive) {
            OutlinedTextField(
              value = query,
              onValueChange = { query = it },
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              placeholder = { Text("Search music") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
              trailingIcon = {
                if (query.isNotEmpty()) {
                  IconButton(onClick = { query = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true,
            )
          }
          TabRow(selectedTabIndex = selectedTab) {
            TAB_TITLES.forEachIndexed { index, title ->
              Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = { Text(title) },
              )
            }
          }
        }
      }
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    ) {
      val currentDetail = detail
      when {
        currentDetail != null -> {
          DetailContent(
            detail = currentDetail,
            library = library,
            onSongClick = onSongClick,
            bottomPadding = bottomPadding,
          )
        }

        library.songs.isEmpty() -> {
          EmptyState(
            icon = Icons.Filled.MusicNote,
            title = "No music found",
            message = "No audio files were found on this device.",
          )
        }

        else -> {
          when (selectedTab) {
            0 -> SongsTab(library, query, songSort, ascending, onSongClick, bottomPadding)
            1 -> AlbumsTab(library, query, ascending, { detail = it }, bottomPadding)
            2 -> ArtistsTab(library, query, ascending, { detail = it }, bottomPadding)
            else -> FoldersTab(library, query, ascending, { detail = it }, bottomPadding)
          }
        }
      }
    }
  }
}

@Composable
private fun SortMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  selectedTab: Int,
  songSort: SongSort,
  onSongSortChange: (SongSort) -> Unit,
  ascending: Boolean,
  onAscendingChange: (Boolean) -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    if (selectedTab == 0) {
      SongSort.entries.forEach { sort ->
        DropdownMenuItem(
          text = { Text(sort.label()) },
          trailingIcon = {
            if (songSort == sort) {
              Icon(Icons.Filled.Check, contentDescription = null)
            }
          },
          onClick = {
            onSongSortChange(sort)
            onDismiss()
          },
        )
      }
    } else {
      DropdownMenuItem(
        text = { Text(if (selectedTab == 1) "Title" else "Name") },
        trailingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
        onClick = onDismiss,
      )
    }
    HorizontalDivider()
    DropdownMenuItem(
      text = { Text("Ascending") },
      trailingIcon = {
        if (ascending) {
          Icon(Icons.Filled.Check, contentDescription = null)
        }
      },
      onClick = {
        onAscendingChange(true)
        onDismiss()
      },
    )
    DropdownMenuItem(
      text = { Text("Descending") },
      trailingIcon = {
        if (!ascending) {
          Icon(Icons.Filled.Check, contentDescription = null)
        }
      },
      onClick = {
        onAscendingChange(false)
        onDismiss()
      },
    )
  }
}

private fun SongSort.label(): String = when (this) {
  SongSort.TITLE -> "Title"
  SongSort.ARTIST -> "Artist"
  SongSort.ALBUM -> "Album"
  SongSort.DURATION -> "Duration"
  SongSort.DATE_ADDED -> "Date added"
}

@Composable
private fun SongsTab(
  library: MusicLibrary,
  query: String,
  songSort: SongSort,
  ascending: Boolean,
  onSongClick: (Song, List<Song>) -> Unit,
  bottomPadding: Dp,
) {
  val songs = MusicLibraryFilter.sortSongs(
    songs = MusicLibraryFilter.filterSongs(library.songs, query),
    sort = songSort,
    ascending = ascending,
  )
  if (songs.isEmpty()) {
    NoResultsState()
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    items(songs, key = { it.id }) { song ->
      SongRow(song = song, onClick = { onSongClick(song, songs) })
    }
  }
}

@Composable
private fun AlbumsTab(
  library: MusicLibrary,
  query: String,
  ascending: Boolean,
  onAlbumClick: (MusicDetail.AlbumDetail) -> Unit,
  bottomPadding: Dp,
) {
  val albums = MusicLibraryFilter.sortAlbums(
    albums = MusicLibraryFilter.filterAlbums(library.albums, query),
    ascending = ascending,
  )
  if (albums.isEmpty()) {
    NoResultsState()
    return
  }
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 8.dp,
      end = 8.dp,
      top = 8.dp,
      bottom = bottomPadding,
    ),
  ) {
    items(albums, key = { it.id }) { album ->
      AlbumCard(album = album, onClick = { onAlbumClick(MusicDetail.AlbumDetail(album)) })
    }
  }
}

@Composable
private fun ArtistsTab(
  library: MusicLibrary,
  query: String,
  ascending: Boolean,
  onArtistClick: (MusicDetail.ArtistDetail) -> Unit,
  bottomPadding: Dp,
) {
  val artists = MusicLibraryFilter.sortArtists(
    artists = MusicLibraryFilter.filterArtists(library.artists, query),
    ascending = ascending,
  )
  if (artists.isEmpty()) {
    NoResultsState()
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    items(artists, key = { it.id }) { artist ->
      ArtistRow(artist = artist, onClick = { onArtistClick(MusicDetail.ArtistDetail(artist)) })
    }
  }
}

@Composable
private fun FoldersTab(
  library: MusicLibrary,
  query: String,
  ascending: Boolean,
  onFolderClick: (MusicDetail.FolderDetail) -> Unit,
  bottomPadding: Dp,
) {
  val folders = MusicLibraryFilter.sortFolders(
    folders = MusicLibraryFilter.filterFolders(library.folders, query),
    ascending = ascending,
  )
  if (folders.isEmpty()) {
    NoResultsState()
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    items(folders, key = { it.path }) { folder ->
      FolderRow(folder = folder, onClick = { onFolderClick(MusicDetail.FolderDetail(folder)) })
    }
  }
}

@Composable
private fun DetailContent(
  detail: MusicDetail,
  library: MusicLibrary,
  onSongClick: (Song, List<Song>) -> Unit,
  bottomPadding: Dp,
) {
  val songs = when (detail) {
    is MusicDetail.AlbumDetail -> MusicLibraryFilter.songsInAlbum(library.songs, detail.album.id)
    is MusicDetail.ArtistDetail -> MusicLibraryFilter.songsByArtist(library.songs, detail.artist.id)
    is MusicDetail.FolderDetail -> MusicLibraryFilter.songsInFolder(library.songs, detail.folder.path)
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = bottomPadding),
  ) {
    item {
      DetailHeader(detail = detail)
    }
    items(songs, key = { it.id }) { song ->
      SongRow(song = song, onClick = { onSongClick(song, songs) })
    }
  }
}

@Composable
private fun DetailHeader(detail: MusicDetail) {
  when (detail) {
    is MusicDetail.AlbumDetail -> {
      val album = detail.album
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AlbumArtwork(
          albumId = album.id,
          targetSizePx = 512,
          modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Column {
          Text(
            text = album.title,
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          val meta = listOfNotNull(
            album.year?.toString(),
            songCountText(album.songCount),
          ).joinToString(" • ")
          Text(
            text = meta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    is MusicDetail.ArtistDetail -> {
      val artist = detail.artist
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = artist.name,
          style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "${artist.albumCount} albums • ${songCountText(artist.songCount)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    is MusicDetail.FolderDetail -> {
      val folder = detail.folder
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = folder.path,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = songCountText(folder.songCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun PermissionRequiredState(onAllowAccess: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        modifier = Modifier.size(96.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
      ) {
        Icon(
          imageVector = Icons.Filled.LibraryMusic,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp),
        )
      }
      Spacer(Modifier.height(24.dp))
      Text(
        text = "Permission needed",
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = "To show the music stored on this device, MPV EX needs access to your audio files.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
      Button(onClick = onAllowAccess) {
        Text("Allow access")
      }
    }
  }
}

@Composable
private fun NoResultsState() {
  EmptyState(
    icon = Icons.Filled.SearchOff,
    title = "No results",
    message = "Nothing matches your search.",
  )
}
