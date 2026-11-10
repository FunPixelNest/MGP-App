package com.funpixelnest.mgp.games.bingo

import kotlin.random.Random

const val FREE = 0

/** All 12 winning lines on a 5x5 card: 5 rows, 5 columns, 2 diagonals. */
val BINGO_LINES: List<List<Int>> = buildList {
    for (r in 0 until 5) add((0 until 5).map { r * 5 + it })
    for (c in 0 until 5) add((0 until 5).map { it * 5 + c })
    add((0 until 5).map { it * 5 + it })
    add((0 until 5).map { it * 5 + (4 - it) })
}

fun bingoLetter(number: Int): Char = "BINGO"[(number - 1) / 15]

/** 25 numbers, row-major. Column c holds numbers from c*15+1..c*15+15; the centre is [FREE]. */
fun generateBingoCard(random: Random): List<Int> {
    val columns = (0 until 5).map { c -> (c * 15 + 1..c * 15 + 15).shuffled(random).take(5) }
    return List(25) { i -> if (i == 12) FREE else columns[i % 5][i / 5] }
}

fun hasBingo(marked: List<Boolean>): Boolean = BINGO_LINES.any { line -> line.all { marked[it] } }

fun cellsToGo(marked: List<Boolean>): Int = BINGO_LINES.minOf { line -> line.count { !marked[it] } }

fun initialMarks(): List<Boolean> = List(25) { it == 12 }

data class BingoBot(
    val name: String,
    val card: List<Int>,
    val marked: List<Boolean> = initialMarks(),
    /** Number of calls made when this bot first completed a line. Bots shout one ball later. */
    val completedAt: Int? = null,
)

enum class BingoPhase { READY, PLAYING, WON, LOST }

data class BingoState(
    val deck: List<Int>,
    val called: List<Int> = emptyList(),
    val card: List<Int>,
    val daubed: List<Boolean> = initialMarks(),
    val bots: List<BingoBot>,
    val phase: BingoPhase = BingoPhase.READY,
    val winner: String? = null,
    val falseCalls: Int = 0,
) {
    val lastBall: Int? get() = called.lastOrNull()
    val toGo: Int get() = cellsToGo(daubed)

    fun start(): BingoState = if (phase == BingoPhase.READY) copy(phase = BingoPhase.PLAYING) else this

    fun isCalled(number: Int): Boolean = number == FREE || number in called

    fun callNext(): BingoState {
        if (phase != BingoPhase.PLAYING) return this
        bots.firstOrNull { it.completedAt != null }?.let {
            return copy(phase = BingoPhase.LOST, winner = it.name)
        }
        if (deck.isEmpty()) return copy(phase = BingoPhase.LOST, winner = null)
        val ball = deck.first()
        val newCalled = called + ball
        val newBots = bots.map { bot ->
            val idx = bot.card.indexOf(ball)
            if (idx < 0) bot else {
                val marks = bot.marked.toMutableList().also { it[idx] = true }
                bot.copy(
                    marked = marks,
                    completedAt = bot.completedAt ?: if (hasBingo(marks)) newCalled.size else null,
                )
            }
        }
        return copy(deck = deck.drop(1), called = newCalled, bots = newBots)
    }

    /** Daubs a cell if its number has been called. Returns null if the daub is not allowed. */
    fun daub(index: Int): BingoState? {
        if (phase != BingoPhase.PLAYING || daubed[index]) return null
        if (!isCalled(card[index])) return null
        return copy(daubed = daubed.toMutableList().also { it[index] = true })
    }

    fun claimBingo(): BingoState {
        if (phase != BingoPhase.PLAYING) return this
        return if (hasBingo(daubed)) copy(phase = BingoPhase.WON, winner = "You")
        else copy(falseCalls = falseCalls + 1)
    }

    companion object {
        private val BOT_NAMES = listOf("Luna", "Max", "Kiki", "Rocco", "Zara", "Milo", "Nova", "Ollie")

        fun newGame(random: Random = Random.Default, botCount: Int = 2): BingoState = BingoState(
            deck = (1..75).shuffled(random),
            card = generateBingoCard(random),
            bots = BOT_NAMES.shuffled(random).take(botCount).map { BingoBot(it, generateBingoCard(random)) },
        )
    }
}
