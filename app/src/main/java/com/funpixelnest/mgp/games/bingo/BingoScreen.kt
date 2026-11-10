package com.funpixelnest.mgp.games.bingo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.GameInfo
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GlassCard
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.bouncyClick
import com.funpixelnest.mgp.ui.components.rememberPulse
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CALL_INTERVAL_MS = 2600L
private val LETTER_COLORS = listOf(
    Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFFA855F7),
)

private fun ballColor(number: Int): Color = LETTER_COLORS[(number - 1) / 15]

private fun rewardFor(calls: Int): Int = maxOf(60, 400 - calls * 6)

@Composable
fun BingoScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.BINGO.id
    var state by remember { mutableStateOf(BingoState.newGame()) }
    var rewarded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.phase) {
        if (state.phase == BingoPhase.PLAYING) {
            delay(900)
            while (state.phase == BingoPhase.PLAYING) {
                state = state.callNext()
                delay(CALL_INTERVAL_MS)
            }
        }
        if (state.phase == BingoPhase.WON && !rewarded) {
            rewarded = true
            store.addCoins(rewardFor(state.called.size))
            store.submitScore(gameId, 100 - state.called.size)
        }
    }

    fun claim() {
        val next = state.claimBingo()
        if (next.phase == BingoPhase.WON) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            message = "Not yet! Complete a row, column or diagonal."
            scope.launch {
                for (x in listOf(18f, -18f, 12f, -12f, 6f, 0f)) {
                    shake.animateTo(x, tween(45))
                }
                delay(1500)
                message = null
            }
        }
        state = next
    }

    fun restart() {
        state = BingoState.newGame()
        rewarded = false
        message = null
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Bingo", store.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Caller area
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val last = state.lastBall
                    if (last != null) {
                        key(last) { BigBall(last) }
                    } else {
                        Box(
                            Modifier.size(78.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) { Text("🎱", fontSize = 36.sp) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (state.phase) {
                                BingoPhase.READY -> "Ready to play?"
                                BingoPhase.PLAYING -> "Ball ${state.called.size} of 75"
                                else -> "Game over"
                            },
                            color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.called.dropLast(1).takeLast(5).reversed().forEach { SmallBall(it, 30.dp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // Bots
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.bots.forEach { bot ->
                    val toGo = cellsToGo(bot.marked)
                    GlassCard(Modifier.weight(1f), corner = 16.dp) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(bot.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (toGo == 0) "BINGO!" else "$toGo to go",
                                    color = if (toGo <= 1) MgpColors.Red else MgpColors.TextSecondary,
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Player card
            BingoCardView(
                state = state,
                onDaub = { i ->
                    state.daub(i)?.let {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state = it
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message ?: if (state.phase == BingoPhase.PLAYING) "Tap called numbers to daub them · ${state.toGo} to go" else " ",
                color = if (message != null) MgpColors.Red else MgpColors.TextSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            when (state.phase) {
                BingoPhase.READY -> GradientButton("START GAME", Modifier.fillMaxWidth(), brush = MgpBrushes.Green) {
                    state = state.start()
                }
                BingoPhase.PLAYING -> GradientButton(
                    "BINGO!",
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shake.value }
                        .scale(if (hasBingo(state.daubed)) rememberPulse(1.06f, 400) else 1f),
                    brush = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF97316))),
                    fontSize = 24,
                ) { claim() }
                else -> GradientButton("PLAY AGAIN", Modifier.fillMaxWidth()) { restart() }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.phase == BingoPhase.WON || state.phase == BingoPhase.LOST) {
        val won = state.phase == BingoPhase.WON
        ResultDialog(
            emoji = if (won) "🥳" else "😵",
            title = if (won) "BINGO!" else "So close!",
            message = when {
                won -> "You won in ${state.called.size} balls!"
                state.winner != null -> "${state.winner} shouted BINGO first."
                else -> "All balls were called."
            },
            reward = if (won) rewardFor(state.called.size) else 0,
            primaryText = "PLAY AGAIN",
            onPrimary = { restart() },
            secondaryText = "HOME",
            onSecondary = onBack,
        )
    }
}

@Composable
private fun BingoCardView(state: BingoState, onDaub: (Int) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFFFF7ED))
            .border(3.dp, Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF97316))), shape)
            .padding(8.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            "BINGO".forEachIndexed { i, ch ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LETTER_COLORS[i])
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(ch.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp) }
            }
        }
        for (r in 0 until 5) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 5) {
                    val i = r * 5 + c
                    BingoCell(
                        number = state.card[i],
                        daubed = state.daubed[i],
                        callable = state.phase == BingoPhase.PLAYING && !state.daubed[i] && state.isCalled(state.card[i]),
                        onClick = { onDaub(i) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BingoCell(number: Int, daubed: Boolean, callable: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val stamp = remember { Animatable(if (daubed) 1f else 0f) }
    LaunchedEffect(daubed) {
        if (daubed) stamp.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) else stamp.snapTo(0f)
    }
    val glow = if (callable) rememberPulse(1.08f, 450) else 1f
    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .scale(glow)
            .clip(RoundedCornerShape(12.dp))
            .background(if (callable) Color(0xFFFDE68A) else Color.White)
            .border(1.dp, Color(0x22000000), RoundedCornerShape(12.dp))
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (number == FREE) "★" else number.toString(),
            color = if (number == FREE) Color(0xFFF59E0B) else Color(0xFF1F2937),
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
        if (daubed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .scale(stamp.value)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFF4ECD).copy(alpha = 0.85f), Color(0xFFBE185D).copy(alpha = 0.9f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (number == FREE) "★" else number.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun BigBall(number: Int) {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)) }
    Box(
        Modifier
            .size(78.dp)
            .scale(pop.value)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, ballColor(number))))
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(bingoLetter(number).toString(), color = Color(0xFF1F2937), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(number.toString(), color = Color(0xFF1F2937), fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
    }
}

@Composable
private fun SmallBall(number: Int, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(ballColor(number))
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
