package com.funpixelnest.mgp.games.wordsearch

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.random.Random

data class Cell(val row: Int, val col: Int)

data class PlacedWord(val word: String, val start: Cell, val dr: Int, val dc: Int) {
    val end: Cell get() = Cell(start.row + dr * (word.length - 1), start.col + dc * (word.length - 1))
}

data class FoundWord(val word: String, val start: Cell, val end: Cell, val colorIndex: Int)

val WORD_THEMES: Map<String, List<String>> = mapOf(
    "Animals" to listOf("TIGER", "ELEPHANT", "GIRAFFE", "ZEBRA", "MONKEY", "PANDA", "KOALA", "RABBIT", "DOLPHIN", "PENGUIN", "LION", "WOLF", "EAGLE", "SHARK"),
    "Fruits" to listOf("APPLE", "BANANA", "MANGO", "ORANGE", "GRAPE", "CHERRY", "LEMON", "PEACH", "PAPAYA", "KIWI", "MELON", "GUAVA", "PLUM", "COCONUT"),
    "Space" to listOf("PLANET", "COMET", "GALAXY", "ORBIT", "ROCKET", "NEBULA", "METEOR", "SATURN", "JUPITER", "MARS", "STAR", "MOON", "ASTEROID", "COSMOS"),
    "Sports" to listOf("SOCCER", "TENNIS", "CRICKET", "HOCKEY", "BOXING", "GOLF", "RUGBY", "SKIING", "SURFING", "KARATE", "CYCLING", "ROWING", "CHESS"),
    "Food" to listOf("PIZZA", "BURGER", "PASTA", "SUSHI", "TACO", "NOODLE", "SALAD", "BREAD", "CHEESE", "COOKIE", "DONUT", "WAFFLE", "PANCAKE", "CURRY"),
)

private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)

/** Cells on the straight (horizontal, vertical or diagonal) line from a to b, or null if not straight. */
fun lineCells(a: Cell, b: Cell): List<Cell>? {
    val dr = b.row - a.row
    val dc = b.col - a.col
    if (!(dr == 0 || dc == 0 || abs(dr) == abs(dc))) return null
    val steps = max(abs(dr), abs(dc))
    return (0..steps).map { Cell(a.row + dr.sign * it, a.col + dc.sign * it) }
}

data class WordSearchState(
    val theme: String,
    val size: Int,
    val grid: List<String>,
    val words: List<PlacedWord>,
    val found: List<FoundWord> = emptyList(),
) {
    val isComplete: Boolean get() = found.size == words.size

    fun letter(cell: Cell): Char = grid[cell.row][cell.col]

    fun isFound(word: String): Boolean = found.any { it.word == word }

    /** Checks a player's selection; returns the new state when it spells an unfound word. */
    fun select(start: Cell, end: Cell): WordSearchState? {
        val cells = lineCells(start, end) ?: return null
        if (cells.size < 2) return null
        val text = cells.joinToString("") { letter(it).toString() }
        val match = words.firstOrNull { !isFound(it.word) && (it.word == text || it.word == text.reversed()) }
            ?: return null
        return copy(found = found + FoundWord(match.word, start, end, found.size))
    }

    companion object {
        fun newGame(random: Random = Random.Default, size: Int = 10, wordCount: Int = 8): WordSearchState {
            val theme = WORD_THEMES.keys.random(random)
            val candidates = WORD_THEMES.getValue(theme).filter { it.length <= size }.shuffled(random).take(wordCount)
            return generate(theme, candidates, size, random)
        }

        fun generate(theme: String, words: List<String>, size: Int, random: Random): WordSearchState {
            val grid = Array(size) { CharArray(size) { '.' } }
            val placed = mutableListOf<PlacedWord>()
            for (word in words.sortedByDescending { it.length }) {
                for (attempt in 0 until 300) {
                    val (dr, dc) = DIRECTIONS[random.nextInt(DIRECTIONS.size)]
                    val r0 = random.nextInt(size)
                    val c0 = random.nextInt(size)
                    val fits = word.indices.all { i ->
                        val r = r0 + dr * i
                        val c = c0 + dc * i
                        r in 0 until size && c in 0 until size && (grid[r][c] == '.' || grid[r][c] == word[i])
                    }
                    if (fits) {
                        word.indices.forEach { i -> grid[r0 + dr * i][c0 + dc * i] = word[i] }
                        placed += PlacedWord(word, Cell(r0, c0), dr, dc)
                        break
                    }
                }
            }
            for (r in 0 until size) for (c in 0 until size) {
                if (grid[r][c] == '.') grid[r][c] = 'A' + random.nextInt(26)
            }
            return WordSearchState(
                theme = theme,
                size = size,
                grid = grid.map { String(it) },
                words = placed.sortedBy { it.word },
            )
        }
    }
}
