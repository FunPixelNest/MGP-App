package com.funpixelnest.mgp.games.yatzy

import kotlin.random.Random

enum class YatzyCategory(val label: String, val isUpper: Boolean) {
    ONES("Ones", true),
    TWOS("Twos", true),
    THREES("Threes", true),
    FOURS("Fours", true),
    FIVES("Fives", true),
    SIXES("Sixes", true),
    ONE_PAIR("One Pair", false),
    TWO_PAIRS("Two Pairs", false),
    THREE_KIND("3 of a Kind", false),
    FOUR_KIND("4 of a Kind", false),
    SMALL_STRAIGHT("Sm Straight", false),
    LARGE_STRAIGHT("Lg Straight", false),
    FULL_HOUSE("Full House", false),
    CHANCE("Chance", false),
    YATZY("YATZY", false),
}

const val UPPER_BONUS_THRESHOLD = 63
const val UPPER_BONUS = 50

/** Scandinavian Yatzy scoring. */
fun yatzyScore(category: YatzyCategory, dice: List<Int>): Int {
    val counts = IntArray(7)
    dice.forEach { counts[it]++ }
    val sum = dice.sum()
    fun ofAKind(n: Int) = (6 downTo 1).firstOrNull { counts[it] >= n }?.let { it * n } ?: 0
    return when (category) {
        YatzyCategory.ONES, YatzyCategory.TWOS, YatzyCategory.THREES,
        YatzyCategory.FOURS, YatzyCategory.FIVES, YatzyCategory.SIXES -> {
            val face = category.ordinal + 1
            counts[face] * face
        }
        YatzyCategory.ONE_PAIR -> ofAKind(2)
        YatzyCategory.TWO_PAIRS -> {
            val pairs = (6 downTo 1).filter { counts[it] >= 2 }
            if (pairs.size >= 2) (pairs[0] + pairs[1]) * 2 else 0
        }
        YatzyCategory.THREE_KIND -> ofAKind(3)
        YatzyCategory.FOUR_KIND -> ofAKind(4)
        YatzyCategory.SMALL_STRAIGHT -> if (dice.sorted() == listOf(1, 2, 3, 4, 5)) 15 else 0
        YatzyCategory.LARGE_STRAIGHT -> if (dice.sorted() == listOf(2, 3, 4, 5, 6)) 20 else 0
        YatzyCategory.FULL_HOUSE -> if (counts.any { it == 3 } && counts.any { it == 2 }) sum else 0
        YatzyCategory.CHANCE -> sum
        YatzyCategory.YATZY -> if (counts.any { it == 5 }) 50 else 0
    }
}

data class YatzyState(
    val dice: List<Int> = listOf(1, 2, 3, 4, 5),
    val held: List<Boolean> = List(5) { false },
    val rollsLeft: Int = 3,
    val hasRolled: Boolean = false,
    val scores: Map<YatzyCategory, Int> = emptyMap(),
) {
    val upperSum: Int get() = scores.filterKeys { it.isUpper }.values.sum()
    val bonus: Int get() = if (upperSum >= UPPER_BONUS_THRESHOLD) UPPER_BONUS else 0
    val total: Int get() = scores.values.sum() + bonus
    val isOver: Boolean get() = scores.size == YatzyCategory.entries.size
    val canRoll: Boolean get() = !isOver && rollsLeft > 0

    fun roll(random: Random = Random.Default): YatzyState? {
        if (!canRoll) return null
        val newDice = dice.mapIndexed { i, d -> if (hasRolled && held[i]) d else random.nextInt(1, 7) }
        return copy(
            dice = newDice,
            held = if (hasRolled) held else List(5) { false },
            rollsLeft = rollsLeft - 1,
            hasRolled = true,
        )
    }

    fun toggleHold(index: Int): YatzyState? {
        if (!hasRolled || rollsLeft == 0) return null
        return copy(held = held.toMutableList().also { it[index] = !it[index] })
    }

    fun potential(category: YatzyCategory): Int? =
        if (!hasRolled || category in scores) null else yatzyScore(category, dice)

    fun choose(category: YatzyCategory): YatzyState? {
        if (!hasRolled || category in scores) return null
        return copy(
            scores = scores + (category to yatzyScore(category, dice)),
            held = List(5) { false },
            rollsLeft = 3,
            hasRolled = false,
        )
    }
}
