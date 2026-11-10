package com.funpixelnest.mgp.ui.bonus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.CoinIcon
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.SunburstRays
import com.funpixelnest.mgp.ui.components.formatCoins
import com.funpixelnest.mgp.ui.components.formatDuration
import com.funpixelnest.mgp.ui.components.rememberNow
import com.funpixelnest.mgp.ui.components.rememberPulse
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors

private enum class DayState { CLAIMED, TODAY, LOCKED }

@Composable
fun DailyBonusScreen(store: PlayerStore, onBack: () -> Unit) {
    val now by rememberNow()
    var claimed by remember { mutableIntStateOf(0) }
    val canClaim = store.canClaimDaily(now)
    val done = store.claimedInCycle(now)
    val todayIndex = if (canClaim) store.dailyIndex(now) else -1

    fun stateOf(day: Int) = when {
        day < done -> DayState.CLAIMED
        day == todayIndex -> DayState.TODAY
        else -> DayState.LOCKED
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Daily Bonus", store.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                SunburstRays(Modifier.fillMaxSize())
                Text("🎁", fontSize = 76.sp, modifier = Modifier.scale(if (canClaim) rememberPulse(1.1f, 600) else 1f))
            }
            Text("Come back every day!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                "Rewards grow each day in a row. Miss a day and the streak resets.",
                color = MgpColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 3) {
                        val day = row * 3 + col
                        DayCard(day, PlayerStore.DAILY_REWARDS[day], stateOf(day), Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            DayCard(6, PlayerStore.DAILY_REWARDS[6], stateOf(6), Modifier.fillMaxWidth(), big = true)

            Spacer(Modifier.height(24.dp))
            if (canClaim) {
                GradientButton(
                    "CLAIM ${formatCoins(PlayerStore.DAILY_REWARDS[todayIndex])} COINS",
                    Modifier.fillMaxWidth().scale(rememberPulse(1.03f, 500)),
                    brush = MgpBrushes.Gold,
                    textColor = Color(0xFF5B2A00),
                ) { claimed = store.claimDaily(System.currentTimeMillis()) }
            } else {
                GradientButton(
                    "Next bonus in ${formatDuration(store.msUntilTomorrow(now))}",
                    Modifier.fillMaxWidth(),
                    brush = MgpBrushes.Glass,
                    enabled = false,
                ) {}
            }
            Text(
                "Streak: ${store.dailyStreak} day${if (store.dailyStreak == 1) "" else "s"} 🔥",
                color = MgpColors.Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }

    if (claimed > 0) {
        ResultDialog(
            emoji = "💰",
            title = "Bonus Collected!",
            message = "See you tomorrow for an even bigger reward.",
            reward = claimed,
            primaryText = "AWESOME",
            onPrimary = { claimed = 0 },
        )
    }
}

@Composable
private fun DayCard(day: Int, reward: Int, state: DayState, modifier: Modifier, big: Boolean = false) {
    val shape = RoundedCornerShape(20.dp)
    val bg = when (state) {
        DayState.TODAY -> Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFF3D77)))
        DayState.CLAIMED -> Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF047857)))
        DayState.LOCKED -> if (big) Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF312E81))) else MgpBrushes.Glass
    }
    val pulse = if (state == DayState.TODAY) rememberPulse(1.05f, 600) else 1f
    Box(
        modifier
            .scale(pulse)
            .clip(shape)
            .background(bg)
            .border(
                if (state == DayState.TODAY) 2.dp else 1.dp,
                if (state == DayState.TODAY) Color.White else MgpColors.GlassStroke,
                shape,
            )
            .padding(vertical = if (big) 16.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (big) "DAY 7 · MEGA BONUS" else "DAY ${day + 1}",
                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    state == DayState.CLAIMED -> "✅"
                    big -> "💎"
                    else -> "🪙"
                },
                fontSize = if (big) 40.sp else 28.sp,
                modifier = Modifier.alpha(if (state == DayState.LOCKED && !big) 0.6f else 1f),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(16.dp)
                Spacer(Modifier.size(4.dp))
                Text(formatCoins(reward), color = MgpColors.Gold, fontWeight = FontWeight.Black, fontSize = if (big) 22.sp else 16.sp)
            }
        }
    }
}
