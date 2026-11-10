package com.funpixelnest.mgp.games.blackjack

import com.funpixelnest.mgp.games.cards.Card
import com.funpixelnest.mgp.games.cards.shuffledDecks
import kotlin.random.Random

fun handValue(cards: List<Card>): Int {
    var sum = cards.sumOf { minOf(it.rank, 10) }
    if (cards.any { it.rank == 1 } && sum + 10 <= 21) sum += 10
    return sum
}

fun isBlackjack(cards: List<Card>): Boolean = cards.size == 2 && handValue(cards) == 21

enum class BlackjackPhase { BETTING, PLAYER_TURN, DEALER_TURN, RESULT }

enum class Outcome(val title: String) {
    BLACKJACK("Blackjack!"),
    WIN("You win!"),
    DEALER_BUST("Dealer busts!"),
    PUSH("Push"),
    LOSE("Dealer wins"),
    BUST("Bust!"),
}

/** Coins returned to the player for a settled hand (includes the original bet). */
fun payout(outcome: Outcome, bet: Int): Int = when (outcome) {
    Outcome.BLACKJACK -> bet + bet * 3 / 2
    Outcome.WIN, Outcome.DEALER_BUST -> bet * 2
    Outcome.PUSH -> bet
    Outcome.LOSE, Outcome.BUST -> 0
}

const val SHOE_DECKS = 6
private const val RESHUFFLE_BELOW = 52

data class BlackjackState(
    val shoe: List<Card>,
    val player: List<Card> = emptyList(),
    val dealer: List<Card> = emptyList(),
    val bet: Int = 0,
    val phase: BlackjackPhase = BlackjackPhase.BETTING,
    val dealerHidden: Boolean = true,
    val outcome: Outcome? = null,
    val handId: Int = 0,
) {
    val playerValue: Int get() = handValue(player)
    val dealerValue: Int get() = handValue(dealer)
    val dealerVisibleValue: Int get() = if (dealerHidden) handValue(dealer.take(1)) else dealerValue
    val canDouble: Boolean get() = phase == BlackjackPhase.PLAYER_TURN && player.size == 2

    fun deal(bet: Int, random: Random = Random.Default): BlackjackState {
        val fresh = if (shoe.size < RESHUFFLE_BELOW) shuffledDecks(SHOE_DECKS, random) else shoe
        val p = listOf(fresh[0], fresh[2])
        val d = listOf(fresh[1], fresh[3])
        val dealt = copy(
            shoe = fresh.drop(4), player = p, dealer = d, bet = bet,
            phase = BlackjackPhase.PLAYER_TURN, dealerHidden = true, outcome = null, handId = handId + 1,
        )
        return when {
            isBlackjack(p) || isBlackjack(d) -> dealt.settle()
            else -> dealt
        }
    }

    fun hit(): BlackjackState {
        if (phase != BlackjackPhase.PLAYER_TURN) return this
        val next = copy(shoe = shoe.drop(1), player = player + shoe.first())
        return when {
            next.playerValue > 21 -> next.copy(phase = BlackjackPhase.RESULT, dealerHidden = false, outcome = Outcome.BUST)
            next.playerValue == 21 -> next.stand()
            else -> next
        }
    }

    fun stand(): BlackjackState {
        if (phase != BlackjackPhase.PLAYER_TURN) return this
        return copy(phase = BlackjackPhase.DEALER_TURN, dealerHidden = false)
    }

    /** Caller must have already taken the extra bet from the player's coins. */
    fun doubleDown(): BlackjackState {
        if (!canDouble) return this
        val next = copy(bet = bet * 2, shoe = shoe.drop(1), player = player + shoe.first())
        return if (next.playerValue > 21) {
            next.copy(phase = BlackjackPhase.RESULT, dealerHidden = false, outcome = Outcome.BUST)
        } else {
            next.copy(phase = BlackjackPhase.DEALER_TURN, dealerHidden = false)
        }
    }

    /** Dealer draws to 17 (stands on all 17s), one card per call, then settles. */
    fun dealerStep(): BlackjackState {
        if (phase != BlackjackPhase.DEALER_TURN) return this
        return if (dealerValue < 17) copy(shoe = shoe.drop(1), dealer = dealer + shoe.first()) else settle()
    }

    fun nextHand(): BlackjackState =
        copy(phase = BlackjackPhase.BETTING, player = emptyList(), dealer = emptyList(), outcome = null, dealerHidden = true)

    private fun settle(): BlackjackState {
        val pv = playerValue
        val dv = dealerValue
        val result = when {
            isBlackjack(player) && isBlackjack(dealer) -> Outcome.PUSH
            isBlackjack(player) -> Outcome.BLACKJACK
            isBlackjack(dealer) -> Outcome.LOSE
            pv > 21 -> Outcome.BUST
            dv > 21 -> Outcome.DEALER_BUST
            pv > dv -> Outcome.WIN
            pv == dv -> Outcome.PUSH
            else -> Outcome.LOSE
        }
        return copy(phase = BlackjackPhase.RESULT, dealerHidden = false, outcome = result)
    }

    companion object {
        fun create(random: Random = Random.Default) = BlackjackState(shoe = shuffledDecks(SHOE_DECKS, random))
    }
}
