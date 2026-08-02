package com.bicy.whitenoise.data.agent.AgentToolPart

data class ToolContext(
    val mainViewModel: com.bicy.whitenoise.ui.viewmodel.MainViewModel? = null,
    val currentPageIndex: Int = 0
)
