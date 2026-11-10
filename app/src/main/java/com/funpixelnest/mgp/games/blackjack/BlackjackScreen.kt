package com.funpixelnest.mgp.games.blackjack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.funpixelnest.mgp.games.cards.Card
import com.funpixelnest.mgp.ui.components.CoinIcon
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.PlayingCardView
import com.funpixelnest.mgp.ui.components.bouncyClick
import com.funpixelnest.mgp.ui.components.formatCoins
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay

private data class Chip(val value: Int, val color: Color)

private val CHIPS = listOf(
    Chip(10, Color(0xFF3B82F6)),
    Chip(50, Color(0xFFEF4444)),
    Chip(100, Color(0xFF16A34A)),
    Chip(500, Color(0xFF111827)),
)

private val CARD_WIDTH = 72.dp

@Composable
fun BlackjackScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.BLACKJACK.id
    var state by remember { mutableStateOf(BlackjackState.create()) }
    var pendingBet by remember { mutableIntStateOf(0) }
    var lastBet by remember { mutableIntStateOf(0) }
    var paidHand by remember { mutableIntStateOf(0) }
    var lastWin by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    // Dealer plays out automatically, one card at a time.
    LaunchedEffect(state.phase, state.dealer.size) {
        if (state.phase == BlackjackPhase.DEALER_TURN) {
            delay(650)
            state = state.dealerStep()
        }
    }

    // Pay out each settled hand exactly once.
    LaunchedEffect(state.handId, state.outcome) {
        val outcome = state.outcome ?: return@LaunchedEffect
        if (paidHand == state.handId) return@LaunchedEffect
        paidHand = state.handId
        val won = payout(outcome, state.bet)
        lastWin = won
        store.addCoins(won)
        streak = if (won > state.bet) streak + 1 else if (won == state.bet) streak else 0
        store.submitScore(gameId, store.coins)
        if (won > state.bet) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun deal() {
        val bet = pendingBet
        if (bet <= 0 || !store.spendCoins(bet)) return
        lastBet = bet
        pendingBet = 0
        state = state.deal(bet)
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Blackjack", store.coins, onBack)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF14955A), MgpColors.Felt, MgpColors.FeltDark), radius = 1400f))
                .border(
                    6.dp,
                    Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFD97706), Color(0xFF92400E))),
                    RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                ),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HandLabel("DEALER", if (state.dealer.isEmpty()) null else state.dealerVisibleValue)
                Spacer(Modifier.height(8.dp))
                HandRow(state.dealer, state.handId, hideSecond = state.dealerHidden)

                Spacer(Modifier.weight(1f))
                Center(state, lastWin, streak)
                Spacer(Modifier.weight(1f))

                HandRow(state.player, state.handId, hideSecond = false)
                Spacer(Modifier.height(8.dp))
                HandLabel("YOU", if (state.player.isEmpty()) null else state.playerValue)
            }
        }

        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            when (state.phase) {
                BlackjackPhase.BETTING -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CHIPS.forEach { chip ->
                            val affordable = store.coins >= pendingBet + chip.value
                            ChipView(chip, enabled = affordable) { if (affordable) pendingBet += chip.value }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("CLEAR", Modifier.weight(1f), brush = MgpBrushes.Glass, enabled = pendingBet > 0, fontSize = 15) { pendingBet = 0 }
                        GradientButton(
                            if (pendingBet > 0) "DEAL ${formatCoins(pendingBet)}" else "PLACE A BET",
                            Modifier.weight(2f),
                            brush = MgpBrushes.Gold,
                            textColor = Color(0xFF5B2A00),
                            enabled = pendingBet > 0,
                        ) { deal() }
                    }
                    if (store.coins < CHIPS.first().value && pendingBet == 0) {
                        Text(
                            "Out of coins? Grab your Daily Bonus or a Lucky Spin!",
                            color = MgpColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                }
                BlackjackPhase.PLAYER_TURN -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("HIT", Modifier.weight(1f), brush = MgpBrushes.Green) { state = state.hit() }
                    GradientButton("STAND", Modifier.weight(1f), brush = MgpBrushes.Danger) { state = state.stand() }
                    GradientButton(
                        "DOUBLE",
                        Modifier.weight(1f),
                        brush = MgpBrushes.Cool,
                        enabled = state.canDouble && store.coins >= state.bet,
                    ) {
                        if (store.spendCoins(state.bet)) state = state.doubleDown()
                    }
                }
                BlackjackPhase.DEALER_TURN -> Text(
                    "Dealer is drawing...",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                )
                BlackjackPhase.RESULT -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("NEW BET", Modifier.weight(1f), brush = MgpBrushes.Glass, fontSize = 15) {
                        state = state.nextHand()
                    }
                    GradientButton(
                        "REBET ${formatCoins(lastBet)}",
                        Modifier.weight(1f),
                        brush = MgpBrushes.Gold,
                        textColor = Color(0xFF5B2A00),
                        enabled = store.coins >= lastBet,
                        fontSize = 15,
                    ) {
                        state = state.nextHand()
                        pendingBet = lastBet
                        deal()
                    }
                }
            }
        }
    }
}

@Composable
private fun Center(state: BlackjackState, lastWin: Int, streak: Int) {
    val outcome = state.outcome
    if (outcome != null) {
        val pop = remember(state.handId) { Animatable(0.4f) }
        LaunchedEffect(state.handId) { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 350f)) }
        val good = outcome == Outcome.WIN || outcome == Outcome.BLACKJACK || outcome == Outcome.DEALER_BUST
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pop.value)) {
            Text(
                outcome.title,
                color = when {
                    good -> MgpColors.Gold
                    outcome == Outcome.PUSH -> Color.White
                    else -> Color(0xFFFFB4B4)
                },
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
            )
            if (lastWin > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(22.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("+${formatCoins(lastWin)}", color = MgpColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            if (good && streak >= 2) {
                Text("🔥 $streak win streak!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else if (state.bet > 0 && state.phase != BlackjackPhase.BETTING) {
        BetBadge(state.bet)
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BLACKJACK PAYS 3 TO 2", color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
            Text("Dealer stands on 17", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun BetBadge(bet: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("BET ", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        CoinIcon(20.dp)
        Spacer(Modifier.width(6.dp))
        Text(formatCoins(bet), color = MgpColors.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun HandLabel(label: String, value: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 13.sp)
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (value > 21) MgpColors.Red else Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(value.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun HandRow(cards: List<Card>, handId: Int, hideSecond: Boolean) {
    val overlap = CARD_WIDTH * 0.45f
    Box(Modifier.height(CARD_WIDTH * 1.4f + 4.dp)) {
        if (cards.isNotEmpty()) {
            val total = CARD_WIDTH + (CARD_WIDTH - overlap) * (cards.size - 1)
            Box(Modifier.width(total)) {
                cards.forEachIndexed { i, card ->
                    key(handId, i) {
                        DealtCard(
                            card = card,
                            faceUp = !(hideSecond && i == 1),
                            x = (CARD_WIDTH - overlap) * i,
                            delayMs = if (i < 2) i * 150 else 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DealtCard(card: Card, faceUp: Boolean, x: Dp, delayMs: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    PlayingCardView(
        card = card,
        faceUp = faceUp,
        width = CARD_WIDTH,
        modifier = Modifier
            .offset(x = x)
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * -220f
                rotationZ = (1f - progress.value) * -18f
            },
    )
}

@Composable
private fun ChipView(chip: Chip, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .bouncyClick(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .clip(CircleShape)
            .background(chip.color)
            .border(5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .padding(9.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(chip.value.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}
