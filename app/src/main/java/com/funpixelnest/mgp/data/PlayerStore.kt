package com.funpixelnest.mgp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.funpixelnest.mgp.GameInfo
import java.util.TimeZone

/**
 * Coins, bonuses and best scores, persisted in SharedPreferences and exposed as Compose state
 * so every screen updates automatically.
 */
class PlayerStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mgp_player", Context.MODE_PRIVATE)

    var coins by mutableIntStateOf(prefs.getInt(KEY_COINS, 0))
        private set
    var welcomeClaimed by mutableStateOf(prefs.getBoolean(KEY_WELCOME, false))
        private set
    var dailyStreak by mutableIntStateOf(prefs.getInt(KEY_STREAK, 0))
        private set
    var lastDailyDay by mutableLongStateOf(prefs.getLong(KEY_LAST_DAILY, NEVER))
        private set
    var lastFreeSpinAt by mutableLongStateOf(prefs.getLong(KEY_LAST_SPIN, 0L))
        private set

    private val bestScores = mutableStateMapOf<String, Int>()

    init {
        GameInfo.entries.forEach { bestScores[it.id] = prefs.getInt(bestKey(it.id), 0) }
    }

    // region Coins
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        coins += amount
        prefs.edit().putInt(KEY_COINS, coins).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0 || coins < amount) return false
        coins -= amount
        prefs.edit().putInt(KEY_COINS, coins).apply()
        return true
    }
    // endregion

    // region Best scores
    fun bestScore(gameId: String): Int = bestScores[gameId] ?: 0

    /** Returns true when [score] is a new personal best. */
    fun submitScore(gameId: String, score: Int): Boolean {
        if (score <= bestScore(gameId)) return false
        bestScores[gameId] = score
        prefs.edit().putInt(bestKey(gameId), score).apply()
        return true
    }
    // endregion

    // region Welcome bonus
    fun claimWelcomeBonus(): Int {
        if (welcomeClaimed) return 0
        welcomeClaimed = true
        prefs.edit().putBoolean(KEY_WELCOME, true).apply()
        addCoins(WELCOME_BONUS)
        return WELCOME_BONUS
    }
    // endregion

    // region Daily bonus
    fun canClaimDaily(now: Long): Boolean = lastDailyDay != dayOf(now)

    /** Which day (0..6) of the 7-day cycle will be claimed next. */
    fun dailyIndex(now: Long): Int = if (lastDailyDay == dayOf(now) - 1) dailyStreak % 7 else 0

    /** How many days of the current 7-day cycle are already collected. */
    fun claimedInCycle(now: Long): Int =
        if (canClaimDaily(now)) dailyIndex(now) else ((dailyStreak - 1) % 7) + 1

    fun claimDaily(now: Long): Int {
        if (!canClaimDaily(now)) return 0
        val index = dailyIndex(now)
        dailyStreak = if (lastDailyDay == dayOf(now) - 1) dailyStreak + 1 else 1
        lastDailyDay = dayOf(now)
        prefs.edit().putInt(KEY_STREAK, dailyStreak).putLong(KEY_LAST_DAILY, lastDailyDay).apply()
        val reward = DAILY_REWARDS[index]
        addCoins(reward)
        return reward
    }

    fun msUntilTomorrow(now: Long): Long = DAY_MS - (localMillis(now) % DAY_MS)
    // endregion

    // region Spin wheel
    fun freeSpinReadyAt(): Long = lastFreeSpinAt + SPIN_COOLDOWN_MS

    fun canFreeSpin(now: Long): Boolean = now >= freeSpinReadyAt()

    fun useFreeSpin(now: Long): Boolean {
        if (!canFreeSpin(now)) return false
        lastFreeSpinAt = now
        prefs.edit().putLong(KEY_LAST_SPIN, now).apply()
        return true
    }
    // endregion

    companion object {
        const val WELCOME_BONUS = 1000
        const val SPIN_COST = 100
        const val SPIN_COOLDOWN_MS = 4L * 60 * 60 * 1000
        val DAILY_REWARDS = listOf(100, 150, 200, 300, 400, 500, 1000)

        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val NEVER = Long.MIN_VALUE / 2

        private const val KEY_COINS = "coins"
        private const val KEY_WELCOME = "welcome_claimed"
        private const val KEY_STREAK = "daily_streak"
        private const val KEY_LAST_DAILY = "last_daily_day"
        private const val KEY_LAST_SPIN = "last_free_spin"
        private fun bestKey(id: String) = "best_$id"

        private fun localMillis(now: Long) = now + TimeZone.getDefault().getOffset(now)
        private fun dayOf(now: Long): Long = localMillis(now) / DAY_MS
    }
}
