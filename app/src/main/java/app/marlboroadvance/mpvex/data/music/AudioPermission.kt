package app.marlboroadvance.mpvex.data.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Version-aware access to the permission that guards the local audio library.
 *
 * - API 26-32 (Android 8 to 12L): READ_EXTERNAL_STORAGE
 * - API 33+ (Android 13 and newer): READ_MEDIA_AUDIO
 *
 * Both permissions are already declared in the manifest: READ_EXTERNAL_STORAGE
 * by the original app (with maxSdkVersion 32) and READ_MEDIA_AUDIO since
 * Phase B1.
 */
object AudioPermission {

  /** Returns the runtime permission required on the given SDK level. */
  fun requiredPermission(sdkInt: Int): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }

  /** True when the app may read the device's audio library right now. */
  fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
      context,
      requiredPermission(Build.VERSION.SDK_INT),
    ) == PackageManager.PERMISSION_GRANTED
}
