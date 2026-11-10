package com.funpixelnest.mgp.games.cards

import kotlin.random.Random

enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false),
    HEARTS("♥", true),
    DIAMONDS("♦", true),
    CLUBS("♣", false),
}

/** A playing card. [rank] is 1 (Ace) to 13 (King). */
data class Card(val rank: Int, val suit: Suit) {
    val rankLabel: String
        get() = when (rank) {
            1 -> "A"
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            else -> rank.toString()
        }
}

fun standardDeck(): List<Card> = Suit.entries.flatMap { suit -> (1..13).map { Card(it, suit) } }

fun shuffledDecks(count: Int, random: Random = Random.Default): List<Card> =
    List(count) { standardDeck() }.flatten().shuffled(random)
