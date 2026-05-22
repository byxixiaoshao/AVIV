package com.bicy.whitenoise.yODW.G2qv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackHandlerManager : ViewModel() {
    
    private val _handlers = MutableStateFlow<List<BackHandlerInfo>>(emptyList())
    val handlers: StateFlow<List<BackHandlerInfo>> = _handlers.asStateFlow()
    
    fun registerHandler(priority: Int, onBack: () -> Boolean): () -> Unit {
        val handler = BackHandlerInfo(
            id = System.nanoTime(),
            priority = priority,
            onBack = onBack
        )
        
        _handlers.value = (_handlers.value + handler).sortedByDescending { it.priority }
        
        return {
            _handlers.value = _handlers.value.filter { it.id != handler.id }
        }
    }
    
    fun handleBack(): Boolean {
        val currentHandlers = _handlers.value
        for (handler in currentHandlers) {
            if (handler.onBack()) {
                return true
            }
        }
        return false
    }
    
    data class BackHandlerInfo(
        val id: Long,
        val priority: Int,
        val onBack: () -> Boolean
    )
}

object BackHandlerPriorities {
    const val SUB_PAGE = 100
    const val DIALOG = 90
    const val EXPANDABLE_PANEL = 80
    const val PAGE_NAVIGATION = 70
    const val MAIN_SCREEN = 60
}

@Composable
fun rememberBackHandler(
    priority: Int,
    enabled: Boolean = true,
    onBack: () -> Unit
): () -> Unit {
    val manager = remember { BackHandlerManager() }
    
    DisposableEffect(priority, enabled) {
        val unregister = if (enabled) {
            manager.registerHandler(priority) {
                onBack()
                true
            }
        } else {
            {}
        }
        
        onDispose {
            unregister()
        }
    }
    
    return { manager.handleBack() }
}
