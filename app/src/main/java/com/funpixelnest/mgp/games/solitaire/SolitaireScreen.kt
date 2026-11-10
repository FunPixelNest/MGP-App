package com.funpixelnest.mgp.games.solitaire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.funpixelnest.mgp.GameInfo
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.games.cards.Suit
import com.funpixelnest.mgp.ui.components.CARD_ASPECT
import com.funpixelnest.mgp.ui.components.CardSlot
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.PlayingCardView
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.StatChip
import com.funpixelnest.mgp.ui.components.formatDuration
import com.funpixelnest.mgp.ui.components.noRippleClick
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WIN_REWARD = 300

@Composable
fun SolitaireScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.SOLITAIRE.id
    var state by remember { mutableStateOf(SolitaireState.deal()) }
    val history = remember { mutableStateListOf<SolitaireState>() }
    var seconds by remember { mutableIntStateOf(0) }
    var autoFinishing by remember { mutableStateOf(false) }
    var rewarded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isWon) {
        while (!state.isWon) {
            delay(1000)
            seconds++
        }
        if (!rewarded) {
            rewarded = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            store.submitScore(gameId, finalScore(state.score, seconds))
            store.addCoins(WIN_REWARD)
        }
    }

    fun commit(next: SolitaireState?) {
        if (next == null || autoFinishing) return
        history.add(state)
        if (history.size > 200) history.removeAt(0)
        state = next
    }

    fun newGame() {
        state = SolitaireState.deal()
        history.clear()
        seconds = 0
        rewarded = false
        autoFinishing = false
    }

    fun autoFinish() {
        if (autoFinishing) return
        autoFinishing = true
        scope.launch {
            while (true) {
                state = state.autoStep() ?: break
                delay(80)
            }
            autoFinishing = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Solitaire", store.coins, onBack)
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Score", state.score.toString(), Modifier.weight(1f), accent = MgpColors.Gold)
            StatChip("Moves", state.moves.toString(), Modifier.weight(1f))
            StatChip("Time", formatDuration(seconds * 1000L), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            val gap = 4.dp
            val cardW = (maxWidth - gap * 6) / 7
            val cardH = cardW * CARD_ASPECT
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Foundations · waste · stock
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Suit.entries.forEachIndexed { i, suit ->
                        val pile = state.foundations[i]
                        Box(Modifier.noRippleClick { commit(state.tapFoundation(i)) }) {
                            if (pile.isEmpty()) CardSlot(cardW, label = suit.symbol)
                            else PlayingCardView(pile.last(), faceUp = true, width = cardW)
                        }
                    }
                    Spacer(Modifier.width(cardW))
                    Box(Modifier.noRippleClick { commit(state.tapWaste()) }) {
                        val top = state.waste.lastOrNull()
                        if (top == null) CardSlot(cardW) else PlayingCardView(top, faceUp = true, width = cardW)
                    }
                    Box(Modifier.noRippleClick { commit(state.drawFromStock()) }) {
                        if (state.stock.isEmpty()) CardSlot(cardW, label = "↻")
                        else PlayingCardView(null, faceUp = false, width = cardW)
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Tableau
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    state.tableau.forEachIndexed { colIndex, column ->
                        val offsets = remember(column) {
                            var y = 0f
                            column.map { tc ->
                                val here = y
                                y += if (tc.faceUp) 0.30f else 0.14f
                                here
                            }
                        }
                        val height = if (column.isEmpty()) cardH else cardH * (1f + offsets.last())
                        Box(Modifier.width(cardW).height(height)) {
                            if (column.isEmpty()) CardSlot(cardW, label = "K")
                            column.forEachIndexed { index, tc ->
                                PlayingCardView(
                                    card = tc.card,
                                    faceUp = tc.faceUp,
                                    width = cardW,
                                    modifier = Modifier
                                        .offset(y = cardH * offsets[index])
                                        .noRippleClick { commit(state.tapTableau(colIndex, index)) },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GradientButton("NEW", Modifier.weight(1f), brush = MgpBrushes.Glass, fontSize = 15) { newGame() }
            GradientButton(
                "UNDO", Modifier.weight(1f), brush = MgpBrushes.Cool, fontSize = 15,
                enabled = history.isNotEmpty() && !autoFinishing,
            ) {
                state = history.removeAt(history.lastIndex)
            }
            if (state.canAutoFinish) {
                GradientButton("AUTO ⚡", Modifier.weight(1f), brush = MgpBrushes.Gold, textColor = androidx.compose.ui.graphics.Color(0xFF5B2A00), fontSize = 15) { autoFinish() }
            }
        }
    }

    if (state.isWon) {
        ResultDialog(
            emoji = "👑",
            title = "You Win!",
            message = "Solved in ${state.moves} moves and ${formatDuration(seconds * 1000L)}.\nScore: ${finalScore(state.score, seconds)}",
            reward = WIN_REWARD,
            primaryText = "NEW GAME",
            onPrimary = { newGame() },
            secondaryText = "HOME",
            onSecondary = onBack,
        )
    }
}

/** Score plus a time bonus for fast wins. */
private fun finalScore(score: Int, seconds: Int): Int = score + maxOf(0, 600 - seconds) * 2
