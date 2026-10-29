package app.marlboroadvance.mpvex.domain.music

/**
 * Pure queue planning for music playback (Phase D).
 *
 * When the user taps a song, playback starts from that song and continues
 * through the list the song was tapped in (the Songs tab, an album, an
 * artist, a folder, or search results). This object turns a tap into a
 * concrete plan: the ordered songs plus the index to start from. It is kept
 * free of Android and Media3 types so it can be unit tested on a plain JVM;
 * the player itself stays the source of truth once playback has started.
 */
object PlaybackQueue {

  /** An ordered playback queue and the index playback should start at. */
  data class Plan(
    val songs: List<Song>,
    val startIndex: Int,
  )

  /**
   * Builds a plan for [tapped] within [context]. Returns null when [context]
   * is empty. If [tapped] is not part of [context] (should not normally
   * happen, because taps come from a visible list), playback starts at the
   * first song instead of failing.
   */
  fun plan(context: List<Song>, tapped: Song): Plan? {
    if (context.isEmpty()) return null
    val index = context.indexOfFirst { it.id == tapped.id }
    return Plan(songs = context, startIndex = if (index >= 0) index else 0)
  }
}
