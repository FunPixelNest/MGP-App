package com.funpixelnest.mgp.games.yatzy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.funpixelnest.mgp.ui.components.StatChip
import com.funpixelnest.mgp.ui.components.bouncyClick
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun YatzyScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.YATZY.id
    var state by remember { mutableStateOf(YatzyState()) }
    var shownDice by remember { mutableStateOf(state.dice) }
    var rolling by remember { mutableStateOf(false) }
    var rewarded by remember { mutableStateOf(false) }
    var bestAtStart by remember { mutableIntStateOf(store.bestScore(gameId)) }
    val spin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun roll() {
        if (rolling) return
        val next = state.roll() ?: return
        val heldBefore = if (state.hasRolled) state.held else List(5) { false }
        state = next
        rolling = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            launch {
                spin.snapTo(0f)
                spin.animateTo(720f, tween(520))
            }
            repeat(8) {
                shownDice = shownDice.mapIndexed { i, d -> if (heldBefore[i]) d else Random.nextInt(1, 7) }
                delay(60)
            }
            shownDice = next.dice
            rolling = false
        }
    }

    fun restart() {
        state = YatzyState()
        shownDice = state.dice
        rewarded = false
        bestAtStart = store.bestScore(gameId)
    }

    LaunchedEffect(state.isOver) {
        if (state.isOver && !rewarded) {
            rewarded = true
            store.submitScore(gameId, state.total)
            store.addCoins(state.total / 2)
        }
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Yatzy", store.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Total", state.total.toString(), Modifier.weight(1f), accent = MgpColors.Gold)
                StatChip("Turn", "${minOf(state.scores.size + 1, 15)}/15", Modifier.weight(1f))
                StatChip("Best", maxOf(bestAtStart, state.total).toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            // Dice tray on felt
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0E7C4A), MgpColors.FeltDark)))
                    .border(3.dp, Brush.linearGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))), RoundedCornerShape(24.dp))
                    .padding(vertical = 18.dp, horizontal = 8.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        shownDice.forEachIndexed { i, value ->
                            val held = state.hasRolled && state.held[i]
                            Die(
                                value = value,
                                held = held,
                                size = 56.dp,
                                rotation = if (!held) spin.value else 0f,
                                dimmed = !state.hasRolled,
                                onClick = { if (!rolling) state.toggleHold(i)?.let { state = it } },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            !state.hasRolled -> "Tap ROLL to start your turn"
                            state.rollsLeft > 0 -> "Tap dice to hold · pick a score below"
                            else -> "No rolls left · choose a score"
                        },
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = if (state.rollsLeft == 3) "ROLL DICE 🎲" else "ROLL AGAIN (${state.rollsLeft} left)",
                modifier = Modifier.fillMaxWidth(),
                brush = MgpBrushes.Gold,
                textColor = Color(0xFF5B2A00),
                enabled = state.canRoll && !rolling,
            ) { roll() }
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreColumn(
                    title = "UPPER",
                    categories = YatzyCategory.entries.filter { it.isUpper },
                    state = state,
                    enabled = !rolling,
                    onPick = { cat -> state.choose(cat)?.let { state = it; shownDice = it.dice } },
                    footer = "Bonus (${state.upperSum}/63)" to (if (state.bonus > 0) "+50" else "—"),
                    modifier = Modifier.weight(1f),
                )
                ScoreColumn(
                    title = "LOWER",
                    categories = YatzyCategory.entries.filter { !it.isUpper },
                    state = state,
                    enabled = !rolling,
                    onPick = { cat -> state.choose(cat)?.let { state = it; shownDice = it.dice } },
                    footer = null,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.isOver) {
        val isBest = state.total > bestAtStart
        ResultDialog(
            emoji = if (isBest) "🏆" else "🎲",
            title = if (isBest) "New Best!" else "Game Over",
            message = "Final score: ${state.total}",
            reward = state.total / 2,
            primaryText = "PLAY AGAIN",
            onPrimary = { restart() },
            secondaryText = "HOME",
            onSecondary = onBack,
        )
    }
}

@Composable
private fun ScoreColumn(
    title: String,
    categories: List<YatzyCategory>,
    state: YatzyState,
    enabled: Boolean,
    onPick: (YatzyCategory) -> Unit,
    footer: Pair<String, String>?,
    modifier: Modifier,
) {
    GlassCard(modifier, corner = 18.dp) {
        Column(Modifier.padding(8.dp)) {
            Text(title, color = MgpColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
            categories.forEach { cat ->
                val scored = state.scores[cat]
                val potential = state.potential(cat)
                val canPick = enabled && potential != null
                val shape = RoundedCornerShape(10.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(shape)
                        .background(
                            when {
                                canPick && potential!! > 0 -> Brush.linearGradient(listOf(MgpColors.Pink.copy(alpha = 0.45f), MgpColors.Purple.copy(alpha = 0.45f)))
                                canPick -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
                                else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                        )
                        .bouncyClick(enabled = canPick) { onPick(cat) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        cat.label,
                        color = if (scored != null) MgpColors.TextMuted else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (cat == YatzyCategory.YATZY) FontWeight.Black else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        text = scored?.toString() ?: potential?.toString() ?: "",
                        color = when {
                            scored != null -> Color.White
                            potential != null && potential > 0 -> MgpColors.Gold
                            else -> MgpColors.TextMuted
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (footer != null) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(footer.first, color = MgpColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(footer.second, color = MgpColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun Die(value: Int, held: Boolean, size: Dp, rotation: Float, dimmed: Boolean, onClick: () -> Unit) {
    val lift by animateDpAsState(if (held) (-10).dp else 0.dp, label = "lift")
    val shape = RoundedCornerShape(size * 0.22f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .offset(y = lift)
                .size(size)
                .graphicsLayer { rotationZ = rotation }
                .bouncyClick(onClick = onClick)
                .alpha(if (dimmed) 0.55f else 1f)
                .shadow(8.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))))
                .border(if (held) 3.dp else 1.dp, if (held) MgpColors.Gold else Color(0x33000000), shape),
        ) {
            Canvas(Modifier.fillMaxSize().padding(size * 0.14f)) {
                val pip = this.size.minDimension * 0.11f
                val w = this.size.width
                val h = this.size.height
                val positions = when (value) {
                    1 -> listOf(0.5f to 0.5f)
                    2 -> listOf(0.2f to 0.2f, 0.8f to 0.8f)
                    3 -> listOf(0.2f to 0.2f, 0.5f to 0.5f, 0.8f to 0.8f)
                    4 -> listOf(0.2f to 0.2f, 0.8f to 0.2f, 0.2f to 0.8f, 0.8f to 0.8f)
                    5 -> listOf(0.2f to 0.2f, 0.8f to 0.2f, 0.5f to 0.5f, 0.2f to 0.8f, 0.8f to 0.8f)
                    else -> listOf(0.2f to 0.2f, 0.8f to 0.2f, 0.2f to 0.5f, 0.8f to 0.5f, 0.2f to 0.8f, 0.8f to 0.8f)
                }
                val color = if (value == 1) MgpColors.CardRed else MgpColors.CardBlack
                positions.forEach { (x, y) ->
                    drawCircle(color, radius = if (value == 1) pip * 1.5f else pip, center = Offset(x * w, y * h))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (held) "HELD" else " ",
            color = MgpColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.width(size),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
