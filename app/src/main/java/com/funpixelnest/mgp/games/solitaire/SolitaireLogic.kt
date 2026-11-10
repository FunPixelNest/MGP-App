package com.funpixelnest.mgp.games.solitaire

import com.funpixelnest.mgp.games.cards.Card
import com.funpixelnest.mgp.games.cards.Suit
import com.funpixelnest.mgp.games.cards.standardDeck
import kotlin.random.Random

data class TableauCard(val card: Card, val faceUp: Boolean)

/**
 * Klondike solitaire (draw one). Foundations are indexed by [Suit.ordinal].
 * Every move returns a new state, or null when the move is not allowed.
 */
data class SolitaireState(
    val stock: List<Card>,
    val waste: List<Card>,
    val foundations: List<List<Card>>,
    val tableau: List<List<TableauCard>>,
    val moves: Int = 0,
    val score: Int = 0,
) {
    val isWon: Boolean get() = foundations.all { it.size == 13 }

    val canAutoFinish: Boolean
        get() = !isWon && stock.isEmpty() && waste.isEmpty() && tableau.all { col -> col.all { it.faceUp } }

    fun canGoToFoundation(card: Card): Boolean {
        val pile = foundations[card.suit.ordinal]
        return if (pile.isEmpty()) card.rank == 1 else pile.last().rank + 1 == card.rank
    }

    fun canGoToColumn(card: Card, column: Int): Boolean {
        val col = tableau[column]
        if (col.isEmpty()) return card.rank == 13
        val top = col.last()
        return top.faceUp && top.card.suit.isRed != card.suit.isRed && top.card.rank == card.rank + 1
    }

    fun drawFromStock(): SolitaireState? = when {
        stock.isNotEmpty() -> copy(stock = stock.dropLast(1), waste = waste + stock.last(), moves = moves + 1)
        waste.isNotEmpty() -> copy(stock = waste.reversed(), waste = emptyList(), moves = moves + 1)
        else -> null
    }

    fun tapWaste(): SolitaireState? {
        val card = waste.lastOrNull() ?: return null
        val newWaste = waste.dropLast(1)
        if (canGoToFoundation(card)) {
            return copy(waste = newWaste, foundations = addToFoundation(card), moves = moves + 1, score = score + 10)
        }
        val target = findColumnFor(card, exclude = -1, allowEmpty = true) ?: return null
        val newTableau = tableau.toMutableList()
        newTableau[target] = tableau[target] + TableauCard(card, true)
        return copy(waste = newWaste, tableau = newTableau, moves = moves + 1, score = score + 5)
    }

    fun tapTableau(column: Int, index: Int): SolitaireState? {
        val col = tableau[column]
        val tc = col.getOrNull(index) ?: return null
        if (!tc.faceUp) return null
        if (index == col.lastIndex && canGoToFoundation(tc.card)) {
            val newTableau = tableau.toMutableList()
            newTableau[column] = col.dropLast(1)
            val (flipped, bonus) = flipTop(newTableau, column)
            return copy(
                tableau = flipped, foundations = addToFoundation(tc.card),
                moves = moves + 1, score = score + 10 + bonus,
            )
        }
        // Moving a king that is already at the bottom of a column to another empty column is pointless.
        val target = findColumnFor(tc.card, exclude = column, allowEmpty = index > 0) ?: return null
        val stack = col.subList(index, col.size)
        val newTableau = tableau.toMutableList()
        newTableau[column] = col.subList(0, index).toList()
        newTableau[target] = tableau[target] + stack
        val (flipped, bonus) = flipTop(newTableau, column)
        return copy(tableau = flipped, moves = moves + 1, score = score + bonus)
    }

    fun tapFoundation(suitIndex: Int): SolitaireState? {
        val pile = foundations[suitIndex]
        val card = pile.lastOrNull() ?: return null
        val target = findColumnFor(card, exclude = -1, allowEmpty = true) ?: return null
        val newTableau = tableau.toMutableList()
        newTableau[target] = tableau[target] + TableauCard(card, true)
        val newFoundations = foundations.toMutableList().also { it[suitIndex] = pile.dropLast(1) }
        return copy(
            tableau = newTableau, foundations = newFoundations,
            moves = moves + 1, score = maxOf(0, score - 15),
        )
    }

    /** Moves the lowest available card to its foundation; used for auto-finish. */
    fun autoStep(): SolitaireState? {
        val candidates = mutableListOf<Pair<Card, () -> SolitaireState?>>()
        waste.lastOrNull()?.let { if (canGoToFoundation(it)) candidates += it to { tapWaste() } }
        tableau.forEachIndexed { c, col ->
            val top = col.lastOrNull()
            if (top != null && top.faceUp && canGoToFoundation(top.card)) {
                candidates += top.card to { tapTableau(c, col.lastIndex) }
            }
        }
        return candidates.minByOrNull { it.first.rank }?.second?.invoke()
    }

    private fun addToFoundation(card: Card): List<List<Card>> =
        foundations.toMutableList().also { it[card.suit.ordinal] = it[card.suit.ordinal] + card }

    private fun findColumnFor(card: Card, exclude: Int, allowEmpty: Boolean): Int? {
        val indices = tableau.indices.filter { it != exclude }
        return indices.firstOrNull { tableau[it].isNotEmpty() && canGoToColumn(card, it) }
            ?: if (allowEmpty) indices.firstOrNull { tableau[it].isEmpty() && canGoToColumn(card, it) } else null
    }

    private fun flipTop(cols: MutableList<List<TableauCard>>, column: Int): Pair<List<List<TableauCard>>, Int> {
        val col = cols[column]
        val top = col.lastOrNull()
        if (top != null && !top.faceUp) {
            cols[column] = col.dropLast(1) + top.copy(faceUp = true)
            return cols to 5
        }
        return cols to 0
    }

    companion object {
        fun deal(random: Random = Random.Default): SolitaireState {
            val deck = standardDeck().shuffled(random).toMutableList()
            val tableau = (0 until 7).map { col ->
                (0..col).map { row -> TableauCard(deck.removeAt(deck.lastIndex), faceUp = row == col) }
            }
            return SolitaireState(
                stock = deck.toList(),
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = tableau,
            )
        }
    }
}
