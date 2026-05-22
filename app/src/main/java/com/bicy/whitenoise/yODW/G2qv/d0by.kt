package com.bicy.whitenoise.yODW.G2qv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class PlaylistNavigationState(
    val hasSubPage: Boolean = false,
    val onNavigateBack: (() -> Unit)? = null
)

val LocalPlaylistNavigation = compositionLocalOf { PlaylistNavigationState() }
val LocalPlaylistNavigationHolder = compositionLocalOf<PlaylistNavigationStateHolder?> { null }

@Composable
fun rememberPlaylistNavigationState(): PlaylistNavigationStateHolder {
    return remember { PlaylistNavigationStateHolder() }
}

class PlaylistNavigationStateHolder {
    private var _hasSubPage by mutableStateOf(false)
    private var _onNavigateBack by mutableStateOf<(() -> Unit)?>(null)
    
    val hasSubPage: Boolean get() = _hasSubPage
    val onNavigateBack: (() -> Unit)? get() = _onNavigateBack
    
    fun updateState(hasSubPage: Boolean, onNavigateBack: (() -> Unit)?) {
        _hasSubPage = hasSubPage
        _onNavigateBack = onNavigateBack
    }
    
    fun toState(): PlaylistNavigationState {
        return PlaylistNavigationState(_hasSubPage, _onNavigateBack)
    }
}
