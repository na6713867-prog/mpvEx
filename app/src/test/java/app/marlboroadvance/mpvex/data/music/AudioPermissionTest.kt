package app.marlboroadvance.mpvex.data.music

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The permission that guards the music library changed in Android 13:
 * older versions use READ_EXTERNAL_STORAGE, Android 13+ uses
 * READ_MEDIA_AUDIO. The app must ask for the right one per version.
 */
class AudioPermissionTest {

  @Test
  fun `minimum supported version uses external storage permission`() {
    assertEquals(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      AudioPermission.requiredPermission(26),
    )
  }

  @Test
  fun `android 9 uses external storage permission`() {
    assertEquals(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      AudioPermission.requiredPermission(28),
    )
  }

  @Test
  fun `android 10 to 12 use external storage permission`() {
    for (sdk in 29..32) {
      assertEquals(
        "sdk $sdk",
        Manifest.permission.READ_EXTERNAL_STORAGE,
        AudioPermission.requiredPermission(sdk),
      )
    }
  }

  @Test
  fun `android 13 uses media audio permission`() {
    assertEquals(
      Manifest.permission.READ_MEDIA_AUDIO,
      AudioPermission.requiredPermission(33),
    )
  }

  @Test
  fun `android 14 and newer use media audio permission`() {
    for (sdk in 34..36) {
      assertEquals(
        "sdk $sdk",
        Manifest.permission.READ_MEDIA_AUDIO,
        AudioPermission.requiredPermission(sdk),
      )
    }
  }
}
