package com.funpixelnest.mgp.games.blockpuzzle

import kotlin.random.Random

const val BOARD_SIZE = 8
const val BLOCK_COLOR_COUNT = 7

/** A piece made of (row, col) cells, normalised so the top-left is (0, 0). */
data class Piece(val cells: List<Pair<Int, Int>>, val color: Int, val id: Long) {
    val rows: Int get() = cells.maxOf { it.first } + 1
    val cols: Int get() = cells.maxOf { it.second } + 1
}

private fun shape(vararg lines: String): List<Pair<Int, Int>> =
    lines.flatMapIndexed { r, line -> line.mapIndexedNotNull { c, ch -> if (ch == '#') r to c else null } }

val SHAPES: List<List<Pair<Int, Int>>> = listOf(
    shape("#"),
    shape("##"), shape("#", "#"),
    shape("###"), shape("#", "#", "#"),
    shape("####"), shape("#", "#", "#", "#"),
    shape("#####"), shape("#", "#", "#", "#", "#"),
    shape("##", "##"),
    shape("###", "###", "###"),
    shape("#.", "##"), shape(".#", "##"), shape("##", "#."), shape("##", ".#"),
    shape("#..", "###"), shape("..#", "###"), shape("###", "#.."), shape("###", "..#"),
    shape("#.", "#.", "##"), shape(".#", ".#", "##"), shape("##", "#.", "#."), shape("##", ".#", ".#"),
    shape("###", ".#."), shape(".#.", "###"), shape("#.", "##", "#."), shape(".#", "##", ".#"),
    shape("##.", ".##"), shape(".##", "##."), shape("#.", "##", ".#"), shape(".#", "##", "#."),
)

/**
 * Immutable Block Puzzle state. [board] holds 0 for an empty cell, otherwise a colour index 1..7.
 */
data class BlockPuzzleState(
    val board: List<Int>,
    val tray: List<Piece?>,
    val score: Int = 0,
    val combo: Int = 0,
    val lastPoints: Int = 0,
    val lastLines: Int = 0,
    val turn: Int = 0,
) {
    val isOver: Boolean get() = tray.filterNotNull().none { fitsAnywhere(it) }

    fun canPlace(piece: Piece, row: Int, col: Int): Boolean = piece.cells.all { (r, c) ->
        val rr = row + r
        val cc = col + c
        rr in 0 until BOARD_SIZE && cc in 0 until BOARD_SIZE && board[rr * BOARD_SIZE + cc] == 0
    }

    fun fitsAnywhere(piece: Piece): Boolean =
        (0 until BOARD_SIZE).any { r -> (0 until BOARD_SIZE).any { c -> canPlace(piece, r, c) } }

    /** Cells that would be cleared if [piece] were dropped at (row, col). */
    fun previewClear(piece: Piece, row: Int, col: Int): Set<Int> {
        if (!canPlace(piece, row, col)) return emptySet()
        val placed = piece.cells.map { (r, c) -> (row + r) * BOARD_SIZE + col + c }.toSet()
        return fullLines { board[it] != 0 || it in placed }.second
    }

    fun place(trayIndex: Int, row: Int, col: Int, random: Random = Random.Default): BlockPuzzleState? {
        val piece = tray.getOrNull(trayIndex) ?: return null
        if (!canPlace(piece, row, col)) return null
        val next = board.toMutableList()
        piece.cells.forEach { (r, c) -> next[(row + r) * BOARD_SIZE + col + c] = piece.color }
        val (lines, cleared) = fullLines { next[it] != 0 }
        cleared.forEach { next[it] = 0 }

        val newCombo = if (lines > 0) combo + 1 else 0
        var points = piece.cells.size + lines * lines * 10
        if (newCombo > 1) points += (newCombo - 1) * 10
        if (lines > 0 && next.all { it == 0 }) points += 300

        var newTray = tray.toMutableList<Piece?>().also { it[trayIndex] = null }.toList()
        if (newTray.all { it == null }) newTray = randomTray(random)

        return copy(
            board = next,
            tray = newTray,
            score = score + points,
            combo = newCombo,
            lastPoints = points,
            lastLines = lines,
            turn = turn + 1,
        )
    }

    companion object {
        fun newGame(random: Random = Random.Default) =
            BlockPuzzleState(board = List(BOARD_SIZE * BOARD_SIZE) { 0 }, tray = randomTray(random))

        fun randomTray(random: Random): List<Piece?> = List(3) { randomPiece(random) }

        fun randomPiece(random: Random) = Piece(
            cells = SHAPES[random.nextInt(SHAPES.size)],
            color = 1 + random.nextInt(BLOCK_COLOR_COUNT),
            id = random.nextLong(),
        )

        /** Returns (number of full lines, set of cells in those lines). */
        internal fun fullLines(filled: (Int) -> Boolean): Pair<Int, Set<Int>> {
            val cells = mutableSetOf<Int>()
            var lines = 0
            for (r in 0 until BOARD_SIZE) {
                if ((0 until BOARD_SIZE).all { filled(r * BOARD_SIZE + it) }) {
                    lines++
                    (0 until BOARD_SIZE).forEach { cells += r * BOARD_SIZE + it }
                }
            }
            for (c in 0 until BOARD_SIZE) {
                if ((0 until BOARD_SIZE).all { filled(it * BOARD_SIZE + c) }) {
                    lines++
                    (0 until BOARD_SIZE).forEach { cells += it * BOARD_SIZE + c }
                }
            }
            return lines to cells
        }
    }
}
