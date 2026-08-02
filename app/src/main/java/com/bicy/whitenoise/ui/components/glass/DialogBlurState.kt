package com.bicy.whitenoise.ui.components.glass

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DialogBlurState {
    private var _dialogCount = 0
    private val _isDialogShowing = MutableStateFlow(false)
    val isDialogShowing: StateFlow<Boolean> = _isDialogShowing

    fun onDialogShown() {
        _dialogCount++
        _isDialogShowing.value = _dialogCount > 0
    }

    fun onDialogDismissed() {
        if (_dialogCount > 0) _dialogCount--
        _isDialogShowing.value = _dialogCount > 0
    }
}
