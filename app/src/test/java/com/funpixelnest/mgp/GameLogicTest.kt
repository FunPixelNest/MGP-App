package com.funpixelnest.mgp

import com.funpixelnest.mgp.games.bingo.BINGO_LINES
import com.funpixelnest.mgp.games.bingo.BingoPhase
import com.funpixelnest.mgp.games.bingo.BingoState
import com.funpixelnest.mgp.games.bingo.FREE
import com.funpixelnest.mgp.games.bingo.generateBingoCard
import com.funpixelnest.mgp.games.blackjack.BlackjackPhase
import com.funpixelnest.mgp.games.blackjack.BlackjackState
import com.funpixelnest.mgp.games.blackjack.Outcome
import com.funpixelnest.mgp.games.blackjack.handValue
import com.funpixelnest.mgp.games.blackjack.payout
import com.funpixelnest.mgp.games.blockpuzzle.BOARD_SIZE
import com.funpixelnest.mgp.games.blockpuzzle.BlockPuzzleState
import com.funpixelnest.mgp.games.blockpuzzle.Piece
import com.funpixelnest.mgp.games.cards.Card
import com.funpixelnest.mgp.games.cards.Suit
import com.funpixelnest.mgp.games.solitaire.SolitaireState
import com.funpixelnest.mgp.games.wordsearch.Cell
import com.funpixelnest.mgp.games.wordsearch.WordSearchState
import com.funpixelnest.mgp.games.wordsearch.lineCells
import com.funpixelnest.mgp.games.yatzy.YatzyCategory
import com.funpixelnest.mgp.games.yatzy.YatzyState
import com.funpixelnest.mgp.games.yatzy.yatzyScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BlockPuzzleTest {
    private val line8 = Piece((0 until 8).map { 0 to it }, color = 1, id = 1)
    private val dot = Piece(listOf(0 to 0), color = 2, id = 2)

    @Test
    fun placingFullRowClearsIt() {
        val s = BlockPuzzleState(board = List(64) { 0 }, tray = listOf(line8, dot, dot))
        assertEquals(8, s.previewClear(line8, 3, 0).size)
        val next = s.place(0, 3, 0)!!
        assertEquals(1, next.lastLines)
        assertTrue(next.board.all { it == 0 })
        assertEquals(8 + 10 + 300, next.score) // cells + 1 line + board clear bonus
        assertNull(next.tray[0])
    }

    @Test
    fun cannotPlaceOutOfBoundsOrOnTop() {
        val s = BlockPuzzleState(board = List(64) { if (it == 0) 3 else 0 }, tray = listOf(dot, dot, dot))
        assertFalse(s.canPlace(dot, 0, 0))
        assertFalse(s.canPlace(line8, 0, 1))
        assertNull(s.place(0, 0, 0))
        assertNotNull(s.place(0, 7, 7))
    }

    @Test
    fun trayRefillsWhenEmptyAndGameOverDetected() {
        val s = BlockPuzzleState(board = List(64) { 0 }, tray = listOf(dot, null, null))
        val next = s.place(0, 0, 0, Random(1))!!
        assertTrue(next.tray.all { it != null })
        val full = BlockPuzzleState(board = List(BOARD_SIZE * BOARD_SIZE) { if (it % 9 == 0) 0 else 1 }, tray = listOf(line8, null, null))
        assertTrue(full.isOver)
    }
}

class YatzyTest {
    @Test
    fun scoring() {
        assertEquals(3, yatzyScore(YatzyCategory.ONES, listOf(1, 1, 1, 2, 3)))
        assertEquals(12, yatzyScore(YatzyCategory.ONE_PAIR, listOf(6, 6, 1, 1, 3)))
        assertEquals(14, yatzyScore(YatzyCategory.TWO_PAIRS, listOf(6, 6, 1, 1, 3)))
        assertEquals(0, yatzyScore(YatzyCategory.TWO_PAIRS, listOf(6, 6, 6, 6, 3)))
        assertEquals(15, yatzyScore(YatzyCategory.SMALL_STRAIGHT, listOf(5, 3, 1, 2, 4)))
        assertEquals(20, yatzyScore(YatzyCategory.LARGE_STRAIGHT, listOf(6, 3, 5, 2, 4)))
        assertEquals(17, yatzyScore(YatzyCategory.FULL_HOUSE, listOf(5, 5, 5, 1, 1)))
        assertEquals(0, yatzyScore(YatzyCategory.FULL_HOUSE, listOf(5, 5, 5, 5, 5)))
        assertEquals(50, yatzyScore(YatzyCategory.YATZY, listOf(2, 2, 2, 2, 2)))
        assertEquals(16, yatzyScore(YatzyCategory.FOUR_KIND, listOf(4, 4, 4, 4, 4)))
    }

    @Test
    fun turnFlowAndHolds() {
        var s = YatzyState()
        assertNull(s.choose(YatzyCategory.CHANCE)) // must roll first
        s = s.roll(Random(3))!!
        val kept = s.dice[0]
        s = s.toggleHold(0)!!
        s = s.roll(Random(4))!!
        assertEquals(kept, s.dice[0])
        s = s.roll(Random(5))!!
        assertNull(s.roll())
        s = s.choose(YatzyCategory.CHANCE)!!
        assertEquals(3, s.rollsLeft)
        assertNull(s.roll()!!.choose(YatzyCategory.CHANCE))
    }

    @Test
    fun fullGameEndsWithBonus() {
        var s = YatzyState()
        YatzyCategory.entries.forEach { s = s.roll(Random(it.ordinal))!!.choose(it)!! }
        assertTrue(s.isOver)
        assertEquals(s.scores.values.sum() + s.bonus, s.total)
    }
}

class SolitaireTest {
    @Test
    fun dealIsValid() {
        val s = SolitaireState.deal(Random(7))
        assertEquals(24, s.stock.size)
        s.tableau.forEachIndexed { i, col ->
            assertEquals(i + 1, col.size)
            assertTrue(col.last().faceUp)
            assertTrue(col.dropLast(1).none { it.faceUp })
        }
        val all = s.stock + s.tableau.flatten().map { it.card }
        assertEquals(52, all.toSet().size)
    }

    @Test
    fun stockCyclesThroughWaste() {
        var s = SolitaireState.deal(Random(7))
        repeat(24) { s = s.drawFromStock()!! }
        assertEquals(0, s.stock.size)
        assertEquals(24, s.waste.size)
        s = s.drawFromStock()!!
        assertEquals(24, s.stock.size)
    }

    @Test
    fun movesFollowRules() {
        val ace = Card(1, Suit.HEARTS)
        val two = Card(2, Suit.HEARTS)
        val blackThree = Card(3, Suit.SPADES)
        val s = SolitaireState(
            stock = emptyList(), waste = listOf(two, ace),
            foundations = List(4) { emptyList() },
            tableau = listOf(listOf(com.funpixelnest.mgp.games.solitaire.TableauCard(blackThree, true))) + List(6) { emptyList() },
        )
        val afterAce = s.tapWaste()!!
        assertEquals(listOf(ace), afterAce.foundations[Suit.HEARTS.ordinal])
        val afterTwo = afterAce.tapWaste()!!
        assertEquals(2, afterTwo.foundations[Suit.HEARTS.ordinal].size)
        // Two back onto black three
        val back = afterTwo.tapFoundation(Suit.HEARTS.ordinal)!!
        assertEquals(two, back.tableau[0].last().card)
    }

    @Test
    fun autoFinishWins() {
        val cols = Suit.entries.map { suit -> (13 downTo 1).map { com.funpixelnest.mgp.games.solitaire.TableauCard(Card(it, suit), true) } }
        var s = SolitaireState(emptyList(), emptyList(), List(4) { emptyList() }, cols + List(3) { emptyList() })
        assertTrue(s.canAutoFinish)
        while (true) s = s.autoStep() ?: break
        assertTrue(s.isWon)
    }
}

class BingoTest {
    @Test
    fun cardHasCorrectColumns() {
        val card = generateBingoCard(Random(1))
        assertEquals(FREE, card[12])
        card.forEachIndexed { i, n -> if (i != 12) assertTrue(n in (i % 5) * 15 + 1..(i % 5) * 15 + 15) }
        assertEquals(24, card.filter { it != FREE }.toSet().size)
        assertEquals(12, BINGO_LINES.size)
    }

    @Test
    fun daubAndClaim() {
        var s = BingoState.newGame(Random(2)).start()
        assertNull(s.daub(0).takeIf { s.card[0] !in s.called })
        val falseClaim = s.claimBingo()
        assertEquals(1, falseClaim.falseCalls)
        // Call balls until game ends, daubing everything we can.
        while (s.phase == BingoPhase.PLAYING) {
            s = s.callNext()
            s.card.indices.forEach { i -> s.daub(i)?.let { s = it } }
            if (com.funpixelnest.mgp.games.bingo.hasBingo(s.daubed)) s = s.claimBingo()
        }
        assertTrue(s.phase == BingoPhase.WON || s.phase == BingoPhase.LOST)
    }
}

class BlackjackTest {
    @Test
    fun handValues() {
        assertEquals(21, handValue(listOf(Card(1, Suit.SPADES), Card(13, Suit.HEARTS))))
        assertEquals(12, handValue(listOf(Card(1, Suit.SPADES), Card(1, Suit.HEARTS))))
        assertEquals(21, handValue(listOf(Card(1, Suit.SPADES), Card(1, Suit.HEARTS), Card(9, Suit.CLUBS))))
        assertEquals(22, handValue(listOf(Card(10, Suit.SPADES), Card(5, Suit.HEARTS), Card(7, Suit.CLUBS))))
    }

    @Test
    fun payouts() {
        assertEquals(250, payout(Outcome.BLACKJACK, 100))
        assertEquals(200, payout(Outcome.WIN, 100))
        assertEquals(100, payout(Outcome.PUSH, 100))
        assertEquals(0, payout(Outcome.BUST, 100))
    }

    @Test
    fun handsAlwaysSettle() {
        val random = Random(9)
        var s = BlackjackState.create(random)
        repeat(300) {
            s = s.deal(10, random)
            while (s.phase == BlackjackPhase.PLAYER_TURN) s = if (s.playerValue < 15) s.hit() else s.stand()
            while (s.phase == BlackjackPhase.DEALER_TURN) s = s.dealerStep()
            assertEquals(BlackjackPhase.RESULT, s.phase)
            assertNotNull(s.outcome)
            if (s.outcome != Outcome.BUST && !s.player.let { it.size == 2 && handValue(it) == 21 } && s.dealer.size > 2) {
                assertTrue(s.dealerValue >= 17)
            }
            s = s.nextHand()
        }
    }
}

class WordSearchTest {
    @Test
    fun allWordsPlacedAndFindable() {
        repeat(50) { seed ->
            var s = WordSearchState.newGame(Random(seed))
            assertEquals(8, s.words.size)
            s.words.forEach { w ->
                val text = lineCells(w.start, w.end)!!.joinToString("") { s.letter(it).toString() }
                assertEquals(w.word, text)
            }
            s.words.forEach { w -> s = s.select(w.end, w.start) ?: s }
            assertTrue(s.isComplete)
        }
    }

    @Test
    fun lineCellsRejectsCrookedLines() {
        assertNull(lineCells(Cell(0, 0), Cell(1, 2)))
        assertEquals(4, lineCells(Cell(3, 3), Cell(0, 0))!!.size)
    }
}
