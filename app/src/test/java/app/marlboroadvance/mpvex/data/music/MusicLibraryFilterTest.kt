package app.marlboroadvance.mpvex.data.music

import app.marlboroadvance.mpvex.domain.music.Album
import app.marlboroadvance.mpvex.domain.music.Artist
import app.marlboroadvance.mpvex.domain.music.MusicFolder
import app.marlboroadvance.mpvex.domain.music.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicLibraryFilterTest {

  private fun song(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist",
    artistId: Long = 1L,
    album: String = "Album",
    albumId: Long = 10L,
    durationMs: Long = 180_000L,
    trackNumber: Int = 1,
    dataPath: String? = "/storage/emulated/0/Music/song$id.mp3",
    relativePath: String? = "Music/",
    dateAddedSeconds: Long = 1_700_000_000L,
  ) = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = "content://media/external/audio/media/$id",
    dataPath = dataPath,
    relativePath = relativePath,
    trackNumber = trackNumber,
    year = 2020,
    sizeBytes = 1_000L,
    mimeType = "audio/mpeg",
    dateAddedSeconds = dateAddedSeconds,
  )

  // ---------- search ----------

  @Test
  fun `song search matches title artist and album ignoring case`() {
    val songs = listOf(
      song(id = 1, title = "Blue Sky", artist = "Asha", album = "Horizons"),
      song(id = 2, title = "Rain", artist = "Rahim", album = "Monsoon"),
    )
    assertEquals(listOf(1L), MusicLibraryFilter.filterSongs(songs, "blue").map { it.id })
    assertEquals(listOf(1L), MusicLibraryFilter.filterSongs(songs, "ASHA").map { it.id })
    assertEquals(listOf(2L), MusicLibraryFilter.filterSongs(songs, "monsoon").map { it.id })
    assertEquals(2, MusicLibraryFilter.filterSongs(songs, "  ").size)
    assertTrue(MusicLibraryFilter.filterSongs(songs, "missing").isEmpty())
  }

  @Test
  fun `album artist and folder search filter by their fields`() {
    val albums = listOf(
      Album(1L, "Horizons", "Asha", 3, 2020, "art://1"),
      Album(2L, "Monsoon", "Rahim", 5, 2021, "art://2"),
    )
    assertEquals(listOf(2L), MusicLibraryFilter.filterAlbums(albums, "rahim").map { it.id })
    assertEquals(listOf(1L), MusicLibraryFilter.filterAlbums(albums, "hori").map { it.id })

    val artists = listOf(
      Artist(1L, "Asha", 1, 3),
      Artist(2L, "Rahim", 2, 9),
    )
    assertEquals(listOf(1L), MusicLibraryFilter.filterArtists(artists, "ash").map { it.id })

    val folders = listOf(
      MusicFolder("/storage/emulated/0/Music", "Music", 4),
      MusicFolder("/storage/emulated/0/Download", "Download", 2),
    )
    assertEquals(
      listOf("Download"),
      MusicLibraryFilter.filterFolders(folders, "down").map { it.name },
    )
    assertEquals(
      listOf("Music"),
      MusicLibraryFilter.filterFolders(folders, "emulated/0/music").map { it.name },
    )
  }

  // ---------- sorting ----------

  @Test
  fun `songs sort by title in both directions`() {
    val songs = listOf(
      song(id = 1, title = "Banana"),
      song(id = 2, title = "apple"),
      song(id = 3, title = "Cherry"),
    )
    assertEquals(
      listOf("apple", "Banana", "Cherry"),
      MusicLibraryFilter.sortSongs(songs, SongSort.TITLE, ascending = true).map { it.title },
    )
    assertEquals(
      listOf("Cherry", "Banana", "apple"),
      MusicLibraryFilter.sortSongs(songs, SongSort.TITLE, ascending = false).map { it.title },
    )
  }

  @Test
  fun `songs sort by duration and date added`() {
    val songs = listOf(
      song(id = 1, durationMs = 300_000L, dateAddedSeconds = 300L),
      song(id = 2, durationMs = 100_000L, dateAddedSeconds = 100L),
      song(id = 3, durationMs = 200_000L, dateAddedSeconds = 200L),
    )
    assertEquals(
      listOf(2L, 3L, 1L),
      MusicLibraryFilter.sortSongs(songs, SongSort.DURATION, ascending = true).map { it.id },
    )
    assertEquals(
      listOf(1L, 3L, 2L),
      MusicLibraryFilter.sortSongs(songs, SongSort.DATE_ADDED, ascending = false).map { it.id },
    )
  }

  @Test
  fun `albums artists and folders sort by name in both directions`() {
    val albums = listOf(
      Album(1L, "Beta", "A", 1, null, "art://1"),
      Album(2L, "alpha", "B", 1, null, "art://2"),
    )
    assertEquals(
      listOf("alpha", "Beta"),
      MusicLibraryFilter.sortAlbums(albums, ascending = true).map { it.title },
    )
    assertEquals(
      listOf("Beta", "alpha"),
      MusicLibraryFilter.sortAlbums(albums, ascending = false).map { it.title },
    )

    val artists = listOf(Artist(1L, "Zed", 1, 1), Artist(2L, "Amy", 1, 1))
    assertEquals(
      listOf("Amy", "Zed"),
      MusicLibraryFilter.sortArtists(artists, ascending = true).map { it.name },
    )

    val folders = listOf(
      MusicFolder("/a/Rock", "Rock", 1),
      MusicFolder("/a/Jazz", "Jazz", 1),
    )
    assertEquals(
      listOf("Jazz", "Rock"),
      MusicLibraryFilter.sortFolders(folders, ascending = true).map { it.name },
    )
  }

  // ---------- detail selection ----------

  @Test
  fun `album songs come back in track order`() {
    val songs = listOf(
      song(id = 1, albumId = 10L, trackNumber = 3, title = "Third"),
      song(id = 2, albumId = 10L, trackNumber = 1, title = "First"),
      song(id = 3, albumId = 11L, trackNumber = 1, title = "Other album"),
    )
    assertEquals(
      listOf("First", "Third"),
      MusicLibraryFilter.songsInAlbum(songs, albumId = 10L).map { it.title },
    )
  }

  @Test
  fun `artist songs only include that artist`() {
    val songs = listOf(
      song(id = 1, artistId = 7L, album = "B album", trackNumber = 2),
      song(id = 2, artistId = 7L, album = "A album", trackNumber = 1),
      song(id = 3, artistId = 8L, album = "A album", trackNumber = 1),
    )
    val result = MusicLibraryFilter.songsByArtist(songs, artistId = 7L)
    assertEquals(listOf(2L, 1L), result.map { it.id })
  }

  @Test
  fun `folder songs match the folder of their file path`() {
    val songs = listOf(
      song(id = 1, dataPath = "/storage/emulated/0/Music/a.mp3"),
      song(id = 2, dataPath = "/storage/emulated/0/Music/b.mp3"),
      song(id = 3, dataPath = "/storage/emulated/0/Other/c.mp3"),
    )
    assertEquals(
      listOf(1L, 2L),
      MusicLibraryFilter.songsInFolder(songs, "/storage/emulated/0/Music").map { it.id },
    )
  }

  // ---------- duration formatting ----------

  @Test
  fun `duration formats as m ss or h mm ss`() {
    assertEquals("0:00", MusicLibraryFilter.formatDuration(0L))
    assertEquals("1:01", MusicLibraryFilter.formatDuration(61_000L))
    assertEquals("59:59", MusicLibraryFilter.formatDuration(3_599_000L))
    assertEquals("1:00:00", MusicLibraryFilter.formatDuration(3_600_000L))
    assertEquals("2:03:05", MusicLibraryFilter.formatDuration(7_385_000L))
  }
}
