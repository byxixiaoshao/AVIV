package com.bicy.whitenoise.data.agent.AgentToolPart

sealed class ToolResult {
    data class Success(
        val message: String,
        val data: Map<String, Any?> = emptyMap(),
        val operationType: String? = null,
        val targetType: String? = null,
        val targetId: String? = null,
        val targetName: String? = null
    ) : ToolResult()

    data class Error(val message: String) : ToolResult()

    fun hasOperation(): Boolean = this is Success && operationType != null && targetType != null && targetId != null
}

fun AgentTool.toOpenAITool(): Map<String, Any> = mapOf(
    "type" to "function",
    "function" to mapOf(
        "name" to name,
        "description" to description,
        "parameters" to mapOf(
            "type" to parameters.type,
            "properties" to parameters.properties.mapValues { (_, prop) ->
                val map = mutableMapOf<String, Any>("type" to prop.type, "description" to prop.description)
                prop.enum?.let { map["enum"] = it }
                map.toMap()
            },
            "required" to parameters.required
        )
    )
)
