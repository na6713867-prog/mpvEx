package app.marlboroadvance.mpvex.domain.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueTest {

  private fun song(id: Long) = Song(
    id = id,
    title = "Song $id",
    artist = "Artist",
    artistId = 1L,
    album = "Album",
    albumId = 1L,
    durationMs = 180_000L,
    uri = "content://media/external/audio/media/$id",
    dataPath = null,
    relativePath = null,
    trackNumber = 0,
    year = 0,
    sizeBytes = 0L,
    mimeType = "audio/mpeg",
    dateAddedSeconds = 0L,
  )

  private val songs = listOf(song(10), song(20), song(30))

  @Test
  fun `empty context gives no plan`() {
    assertNull(PlaybackQueue.plan(emptyList(), song(10)))
  }

  @Test
  fun `tapped song in the middle starts at its index`() {
    val plan = PlaybackQueue.plan(songs, song(20))
    assertEquals(1, plan?.startIndex)
    assertEquals(songs, plan?.songs)
  }

  @Test
  fun `tapped first song starts at zero`() {
    val plan = PlaybackQueue.plan(songs, song(10))
    assertEquals(0, plan?.startIndex)
  }

  @Test
  fun `tapped last song starts at the last index`() {
    val plan = PlaybackQueue.plan(songs, song(30))
    assertEquals(2, plan?.startIndex)
  }

  @Test
  fun `tapped song missing from context falls back to the first song`() {
    val plan = PlaybackQueue.plan(songs, song(99))
    assertEquals(0, plan?.startIndex)
    assertEquals(songs, plan?.songs)
  }

  @Test
  fun `order of the context list is preserved`() {
    val reordered = listOf(song(30), song(10), song(20))
    val plan = PlaybackQueue.plan(reordered, song(10))
    assertEquals(listOf(30L, 10L, 20L), plan?.songs?.map { it.id })
    assertEquals(1, plan?.startIndex)
  }
}
