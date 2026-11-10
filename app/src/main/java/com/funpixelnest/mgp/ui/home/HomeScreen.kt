package com.funpixelnest.mgp.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.GameInfo
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.CoinPill
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.SectionTitle
import com.funpixelnest.mgp.ui.components.bouncyClick
import com.funpixelnest.mgp.ui.components.formatCoins
import com.funpixelnest.mgp.ui.components.formatDuration
import com.funpixelnest.mgp.ui.components.rememberNow
import com.funpixelnest.mgp.ui.components.rememberPulse
import com.funpixelnest.mgp.ui.theme.MgpColors

@Composable
fun HomeScreen(
    store: PlayerStore,
    onOpenGame: (GameInfo) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenSpin: () -> Unit,
) {
    val now by rememberNow()
    var showWelcome by remember { mutableStateOf(!store.welcomeClaimed) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { HomeHeader(store.coins) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val dailyReady = store.canClaimDaily(now)
                BonusCard(
                    emoji = "🎁",
                    title = "Daily Bonus",
                    subtitle = if (dailyReady) "Ready to claim!" else "Next in ${formatDuration(store.msUntilTomorrow(now))}",
                    ready = dailyReady,
                    brush = Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFF3D77))),
                    onClick = onOpenDaily,
                    modifier = Modifier.weight(1f),
                )
                val spinReady = store.canFreeSpin(now)
                BonusCard(
                    emoji = "🎡",
                    title = "Lucky Spin",
                    subtitle = if (spinReady) "Free spin ready!" else "Free in ${formatDuration(store.freeSpinReadyAt() - now)}",
                    ready = spinReady,
                    brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF22D3EE))),
                    onClick = onOpenSpin,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Games", Modifier.padding(top = 6.dp)) }
        items(GameInfo.entries, key = { it.id }) { game ->
            GameTile(game, best = store.bestScore(game.id), onClick = { onOpenGame(game) })
        }
    }

    if (showWelcome) {
        ResultDialog(
            emoji = "🎉",
            title = "Welcome!",
            message = "Thanks for joining MGP Games. Here's a welcome gift to get the party started!",
            reward = PlayerStore.WELCOME_BONUS,
            primaryText = "CLAIM ${formatCoins(PlayerStore.WELCOME_BONUS)} COINS",
            onPrimary = {
                store.claimWelcomeBonus()
                showWelcome = false
            },
        )
    }
}

@Composable
private fun HomeHeader(coins: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MgpColors.Pink, MgpColors.Purple)))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("😎", fontSize = 24.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Welcome back", color = MgpColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("MGP", color = MgpColors.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(" Games", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
        CoinPill(coins)
    }
}

@Composable
private fun BonusCard(
    emoji: String,
    title: String,
    subtitle: String,
    ready: Boolean,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse = if (ready) rememberPulse(1.04f, 650) else 1f
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .scale(pulse)
            .bouncyClick(onClick = onClick)
            .clip(shape)
            .background(brush)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
            .border(1.5.dp, Color.White.copy(alpha = if (ready) 0.7f else 0.25f), shape)
            .padding(14.dp),
    ) {
        Column {
            Text(emoji, fontSize = 34.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        if (ready) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MgpColors.Red)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun GameTile(game: GameInfo, best: Int, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "bob")
    val bob by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1400 + game.ordinal * 130), RepeatMode.Reverse),
        label = "bobOffset",
    )
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .aspectRatio(0.82f)
            .bouncyClick(onClick = onClick)
            .clip(shape)
            .background(game.brush)
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), shape),
    ) {
        // Decorative bubbles
        Box(
            Modifier
                .size(110.dp)
                .align(Alignment.TopEnd)
                .offset(36.dp, (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .size(60.dp)
                .align(Alignment.BottomStart)
                .offset((-18).dp, 18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
        )
        Text(
            game.emoji,
            fontSize = 52.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .offset(y = bob.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
                .padding(14.dp),
        ) {
            Text(game.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(game.tagline, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    if (best > 0) "🏆 ${formatCoins(best)}" else "▶ PLAY",
                    color = MgpColors.Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
