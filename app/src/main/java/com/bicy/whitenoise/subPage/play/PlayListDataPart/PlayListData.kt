package com.bicy.whitenoise.subPage.play.PlayListDataPart

data class PlayListData(
    val isPaused: Boolean = false,
    val sounds: List<SoundPlayData> = emptyList()
)
