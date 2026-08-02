package com.bicy.whitenoise.ui.screens.ChatStatePart

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object ChatState {
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf()
}
