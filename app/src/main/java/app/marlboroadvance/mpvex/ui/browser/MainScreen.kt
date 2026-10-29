package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.playback.MusicPlayerController
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.profile.ProfileScreen
import app.marlboroadvance.mpvex.ui.music.MiniPlayerBar
import app.marlboroadvance.mpvex.ui.music.MiniPlayerBarExtraHeight
import app.marlboroadvance.mpvex.ui.music.MusicLibraryScreen
import app.marlboroadvance.mpvex.ui.music.NowPlayingScreen
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
  private var persistentSelectedTab: Int = 0

  // Reactive shared state that can be updated by FileSystemBrowserScreen / selection handlers
  private val _isInSelectionMode = MutableStateFlow(false)
  val isInSelectionMode: StateFlow<Boolean> = _isInSelectionMode.asStateFlow()

  private val _shouldHideNavigationBar = MutableStateFlow(false)
  val shouldHideNavigationBar: StateFlow<Boolean> = _shouldHideNavigationBar.asStateFlow()

  private val _sharedVideoSelectionManager = MutableStateFlow<Any?>(null)
  val sharedVideoSelectionManager: StateFlow<Any?> = _sharedVideoSelectionManager.asStateFlow()

  private val _isPermissionDenied = MutableStateFlow(false)

  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    _isInSelectionMode.value = isInSelectionMode
    _sharedVideoSelectionManager.value = selectionManager
    // Only hide navigation bar when videos are selected AND in selection mode
    _shouldHideNavigationBar.value = isInSelectionMode && isOnlyVideosSelected
  }

  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    _isPermissionDenied.value = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = _isPermissionDenied.value

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    _shouldHideNavigationBar.value = !shouldShow
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by remember {
      mutableIntStateOf(persistentSelectedTab.coerceIn(0, 3))
    }

    val context = LocalContext.current
    val density = LocalDensity.current

    val hideNavigationBar by _shouldHideNavigationBar.collectAsState()

    // Music mini-player (Phase E): the controller is process-wide, so the
    // bar can live here and stay visible on every tab while a song is
    // loaded. Now Playing is hosted at this level for the same reason.
    val musicController = remember { MusicPlayerController.getInstance(context) }
    val playerState by musicController.state.collectAsState()
    var nowPlayingOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
      musicController.connect()
    }
    val miniPlayerVisible = playerState.currentSongId != null && !nowPlayingOpen

    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      persistentSelectedTab = selectedTab
    }

    if (nowPlayingOpen) {
      NowPlayingScreen(onClose = { nowPlayingOpen = false })
      return
    }

    // Scaffold with bottom navigation bar
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        Column {
          // Mini-player above the pill bar; it hides together with the bar
          // (e.g. during video selection mode).
          AnimatedVisibility(
            visible = miniPlayerVisible && !hideNavigationBar,
            enter = slideInVertically(
              animationSpec = tween(durationMillis = 300),
              initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = slideOutVertically(
              animationSpec = tween(durationMillis = 300),
              targetOffsetY = { fullHeight -> fullHeight }
            )
          ) {
            MiniPlayerBar(
              state = playerState,
              onPlayPause = { musicController.togglePlayPause() },
              onClick = { nowPlayingOpen = true }
            )
          }
          // Animated bottom navigation bar with slide animations
          AnimatedVisibility(
            visible = !hideNavigationBar,
            enter = slideInVertically(
              animationSpec = tween(durationMillis = 300),
              initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = slideOutVertically(
              animationSpec = tween(durationMillis = 300),
              targetOffsetY = { fullHeight -> fullHeight }
            )
          ) {
            PillNavigationBar(
              selectedTab = selectedTab,
              onSelect = { selectedTab = it }
            )
          }
        }
      }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        // Bottom padding for tab content: 96dp for the pill bar, plus the
        // mini-player's height while it is visible.
        val fabBottomPadding = if (miniPlayerVisible && !hideNavigationBar) {
          96.dp + MiniPlayerBarExtraHeight
        } else {
          96.dp
        }

        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            // Material 3 Expressive slide-in-fade animation (like Google Phone app)
            val slideDistance = with(density) { 48.dp.roundToPx() }
            val animationDuration = 250
            
            if (targetState > initialState) {
              // Moving forward: slide in from right with fade
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { -slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            } else {
              // Moving backward: slide in from left with fade
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { -slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            }
          },
          label = "tab_animation"
        ) { targetTab ->
          CompositionLocalProvider(
            LocalNavigationBarHeight provides fabBottomPadding
          ) {
            when (targetTab) {
              0 -> FolderListScreen.Content()
              1 -> MusicLibraryScreen.Content(
                onOpenNowPlaying = { nowPlayingOpen = true }
              )
              2 -> NetworkStreamingScreen.Content()
              3 -> ProfileScreen.Content()
            }
          }
        }
      }
    }
  }
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

/**
 * Floating pill-shaped bottom navigation bar with four tabs
 * (Home, Music, Network, Profile), following the owner's reference design:
 * a rounded bar floating above the content, with the selected tab shown as
 * a filled pill highlight behind its icon and label.
 */
@Composable
private fun PillNavigationBar(
  selectedTab: Int,
  onSelect: (Int) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(percent = 50),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      shadowElevation = 6.dp
    ) {
      Row(
        modifier = Modifier.padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        PillTab(
          icon = Icons.Filled.Home,
          label = "Home",
          selected = selectedTab == 0,
          onClick = { onSelect(0) }
        )
        PillTab(
          icon = Icons.Filled.MusicNote,
          label = "Music",
          selected = selectedTab == 1,
          onClick = { onSelect(1) }
        )
        PillTab(
          icon = Icons.Filled.Language,
          label = "Network",
          selected = selectedTab == 2,
          onClick = { onSelect(2) }
        )
        PillTab(
          icon = Icons.Filled.AccountCircle,
          label = "Profile",
          selected = selectedTab == 3,
          onClick = { onSelect(3) }
        )
      }
    }
  }
}

@Composable
private fun RowScope.PillTab(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  val contentColor = if (selected) {
    MaterialTheme.colorScheme.onSecondaryContainer
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }
  Column(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(percent = 50))
      .background(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
      )
      .selectable(
        selected = selected,
        role = Role.Tab,
        onClick = onClick
      )
      .padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = contentColor
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = contentColor
    )
  }
}
