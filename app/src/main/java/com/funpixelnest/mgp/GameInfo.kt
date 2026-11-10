package com.funpixelnest.mgp

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class GameInfo(
    val id: String,
    val title: String,
    val tagline: String,
    val emoji: String,
    val startColor: Color,
    val endColor: Color,
) {
    BLOCK_PUZZLE("block_puzzle", "Block Puzzle", "Fill lines, chain combos", "🧩", Color(0xFF7C3AED), Color(0xFFDB2777)),
    YATZY("yatzy", "Yatzy", "Roll for the big 50", "🎲", Color(0xFFF59E0B), Color(0xFFEF4444)),
    SOLITAIRE("solitaire", "Solitaire", "Classic Klondike", "🃏", Color(0xFF0EA5E9), Color(0xFF6366F1)),
    BINGO("bingo", "Bingo", "Beat the bots to BINGO", "🎱", Color(0xFFEC4899), Color(0xFFF97316)),
    BLACKJACK("blackjack", "Blackjack", "Hit 21, win big", "♠️", Color(0xFF16A34A), Color(0xFF065F46)),
    WORD_SEARCH("word_search", "Word Search", "Find the hidden words", "🔤", Color(0xFF14B8A6), Color(0xFF2563EB)),
    ;

    val brush: Brush get() = Brush.linearGradient(listOf(startColor, endColor))

    companion object {
        fun byId(id: String): GameInfo? = entries.firstOrNull { it.id == id }
    }
}
