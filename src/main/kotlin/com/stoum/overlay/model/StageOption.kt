package com.stoum.overlay.model

data class StageOption(
    val type: String,
    val displayName: String,
    val requiresDay: Boolean = false,
    val requiresPlayer: Boolean = false
)

class StageOptions {
    companion object {
        val AVAILABLE_STAGES = listOf(
            StageOption("dealing", "Раздача", requiresDay = false, requiresPlayer = false),
            StageOption("briefing", "Договорка", requiresDay = false, requiresPlayer = false),
            StageOption("comIntro", "Осмотр шерифа", requiresDay = false, requiresPlayer = false),
            StageOption("speech", "Речь игрока", requiresDay = true, requiresPlayer = true),
            StageOption("guess", "Лучший ход", requiresDay = false, requiresPlayer = false),
            StageOption("gameOver", "Конец игры", requiresDay = false, requiresPlayer = false)
        )

        @JvmStatic
        fun getDisplayName(type: String): String {
            return AVAILABLE_STAGES.find { it.type == type }?.displayName ?: type
        }
    }
}
