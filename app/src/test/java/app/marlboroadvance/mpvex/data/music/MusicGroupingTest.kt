package app.marlboroadvance.mpvex.data.music

import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicGroupingTest {

  private fun song(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist",
    artistId: Long = 1L,
    album: String = "Album",
    albumId: Long = 10L,
    dataPath: String? = "/storage/emulated/0/Music/song$id.mp3",
    relativePath: String? = "Music/",
    year: Int = 2020,
  ) = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    durationMs = 180_000L,
    uri = MusicGrouping.songUri(id),
    dataPath = dataPath,
    relativePath = relativePath,
    trackNumber = 1,
    year = year,
    sizeBytes = 1_000L,
    mimeType = "audio/mpeg",
    dateAddedSeconds = 1_700_000_000L,
  )

  @Test
  fun `empty library stays empty`() {
    assertTrue(MusicGrouping.buildAlbums(emptyList()).isEmpty())
    assertTrue(MusicGrouping.buildArtists(emptyList()).isEmpty())
    assertTrue(MusicGrouping.buildFolders(emptyList()).isEmpty())
    assertTrue(MusicLibrary.EMPTY.songs.isEmpty())
    assertTrue(MusicLibrary.EMPTY.albums.isEmpty())
    assertTrue(MusicLibrary.EMPTY.artists.isEmpty())
    assertTrue(MusicLibrary.EMPTY.folders.isEmpty())
  }

  @Test
  fun `uri helpers build the standard mediastore uris`() {
    assertEquals("content://media/external/audio/media/42", MusicGrouping.songUri(42L))
    assertEquals("content://media/external/audio/albumart/7", MusicGrouping.albumArtUri(7L))
  }

  @Test
  fun `albums are grouped by album id with counts and earliest year`() {
    val songs = listOf(
      song(id = 1, album = "Beta", albumId = 20, year = 2019),
      song(id = 2, album = "Beta", albumId = 20, year = 2015),
      song(id = 3, album = "Beta", albumId = 20, year = 0),
      song(id = 4, album = "Alpha", albumId = 10, year = 2021),
    )

    val albums = MusicGrouping.buildAlbums(songs)

    assertEquals(2, albums.size)
    // Sorted by title: Alpha first, Beta second.
    assertEquals(10L, albums[0].id)
    assertEquals(1, albums[0].songCount)
    assertEquals(20L, albums[1].id)
    assertEquals(3, albums[1].songCount)
    // Earliest positive year wins; year 0 (unknown) is ignored.
    assertEquals(2015, albums[1].year)
    assertEquals("content://media/external/audio/albumart/20", albums[1].artworkUri)
  }

  @Test
  fun `album year is null when every song year is unknown`() {
    val albums = MusicGrouping.buildAlbums(listOf(song(id = 1, year = 0)))
    assertNull(albums.single().year)
  }

  @Test
  fun `artists are grouped by artist id with album and song counts`() {
    val songs = listOf(
      song(id = 1, artist = "Zed", artistId = 5, albumId = 10),
      song(id = 2, artist = "Zed", artistId = 5, albumId = 10),
      song(id = 3, artist = "Zed", artistId = 5, albumId = 11),
      song(id = 4, artist = "Amy", artistId = 6, albumId = 12),
    )

    val artists = MusicGrouping.buildArtists(songs)

    assertEquals(2, artists.size)
    // Sorted by name: Amy first, Zed second.
    assertEquals("Amy", artists[0].name)
    assertEquals(1, artists[0].songCount)
    assertEquals("Zed", artists[1].name)
    assertEquals(3, artists[1].songCount)
    assertEquals(2, artists[1].albumCount)
  }

  @Test
  fun `folders come from the parent of the file path`() {
    val songs = listOf(
      song(id = 1, dataPath = "/storage/emulated/0/Music/Rock/a.mp3"),
      song(id = 2, dataPath = "/storage/emulated/0/Music/Rock/b.mp3"),
      song(id = 3, dataPath = "/storage/emulated/0/Download/c.mp3"),
    )

    val folders = MusicGrouping.buildFolders(songs)

    assertEquals(2, folders.size)
    val byPath = folders.associateBy { it.path }
    assertEquals(2, byPath.getValue("/storage/emulated/0/Music/Rock").songCount)
    assertEquals("Rock", byPath.getValue("/storage/emulated/0/Music/Rock").name)
    assertEquals(1, byPath.getValue("/storage/emulated/0/Download").songCount)
    assertEquals("Download", byPath.getValue("/storage/emulated/0/Download").name)
  }

  @Test
  fun `folder falls back to relative path and skips songs without any path`() {
    val songs = listOf(
      song(id = 1, dataPath = null, relativePath = "Music/Podcasts/"),
      song(id = 2, dataPath = null, relativePath = null),
      song(id = 3, dataPath = " ", relativePath = null),
    )

    val folders = MusicGrouping.buildFolders(songs)

    assertEquals(1, folders.size)
    assertEquals("Music/Podcasts", folders.single().path)
    assertEquals("Podcasts", folders.single().name)
    assertEquals(1, folders.single().songCount)
  }
}
