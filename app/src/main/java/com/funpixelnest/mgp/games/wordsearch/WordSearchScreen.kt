package com.funpixelnest.mgp.games.wordsearch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.GameInfo
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.StatChip
import com.funpixelnest.mgp.ui.components.formatDuration
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay

private val FOUND_COLORS = listOf(
    Color(0xFFFF4ECD), Color(0xFF22D3EE), Color(0xFFFFC233), Color(0xFF34D399),
    Color(0xFF8B5CF6), Color(0xFFFF8A3D), Color(0xFF3B82F6), Color(0xFFFF5470),
)

private fun rewardFor(seconds: Int) = 100 + maxOf(0, 240 - seconds)
private fun scoreFor(seconds: Int) = maxOf(100, 2000 - seconds * 5)

@Composable
fun WordSearchScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.WORD_SEARCH.id
    var state by remember { mutableStateOf(WordSearchState.newGame()) }
    var selStart by remember { mutableStateOf<Cell?>(null) }
    var selEnd by remember { mutableStateOf<Cell?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var rewarded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isComplete, state.theme, state.grid) {
        while (!state.isComplete) {
            delay(1000)
            seconds++
        }
        if (!rewarded) {
            rewarded = true
            store.addCoins(rewardFor(seconds))
            store.submitScore(gameId, scoreFor(seconds))
        }
    }

    fun restart() {
        state = WordSearchState.newGame()
        seconds = 0
        rewarded = false
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Word Search", store.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Theme", state.theme, Modifier.weight(1.3f), accent = MgpColors.Cyan)
                StatChip("Found", "${state.found.size}/${state.words.size}", Modifier.weight(1f), accent = MgpColors.Gold)
                StatChip("Time", formatDuration(seconds * 1000L), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            val n = state.size
            val boardShape = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(boardShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFF3E8FF))))
                    .border(3.dp, Brush.linearGradient(listOf(Color(0xFF14B8A6), Color(0xFF2563EB))), boardShape)
                    .padding(8.dp)
                    .pointerInput(state.grid) {
                        fun cellAt(p: Offset): Cell {
                            val cw = size.width / n.toFloat()
                            val ch = size.height / n.toFloat()
                            return Cell(
                                (p.y / ch).toInt().coerceIn(0, n - 1),
                                (p.x / cw).toInt().coerceIn(0, n - 1),
                            )
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val start = cellAt(down.position)
                            selStart = start
                            selEnd = start
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val cell = cellAt(change.position)
                                if (lineCells(start, cell) != null && cell != selEnd) {
                                    selEnd = cell
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                change.consume()
                            }
                            val end = selEnd
                            if (end != null) {
                                state.select(start, end)?.let {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    state = it
                                }
                            }
                            selStart = null
                            selEnd = null
                        }
                    },
            ) {
                // Highlights behind the letters
                Canvas(Modifier.fillMaxSize()) {
                    val cw = size.width / n
                    val ch = size.height / n
                    fun center(c: Cell) = Offset((c.col + 0.5f) * cw, (c.row + 0.5f) * ch)
                    state.found.forEach { f ->
                        drawLine(
                            FOUND_COLORS[f.colorIndex % FOUND_COLORS.size].copy(alpha = 0.55f),
                            center(f.start), center(f.end),
                            strokeWidth = cw * 0.8f, cap = StrokeCap.Round,
                        )
                    }
                    val s = selStart
                    val e = selEnd
                    if (s != null && e != null) {
                        drawLine(
                            MgpColors.Purple.copy(alpha = 0.5f), center(s), center(e),
                            strokeWidth = cw * 0.8f, cap = StrokeCap.Round,
                        )
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until n) {
                        Row(Modifier.weight(1f)) {
                            for (c in 0 until n) {
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(
                                        state.grid[r][c].toString(),
                                        color = Color(0xFF1E1B4B),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            state.words.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { placed ->
                        val found = state.found.firstOrNull { it.word == placed.word }
                        val shape = RoundedCornerShape(50)
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(
                                    if (found != null) FOUND_COLORS[found.colorIndex % FOUND_COLORS.size].copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.1f),
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                placed.word,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textDecoration = if (found != null) TextDecoration.LineThrough else null,
                                maxLines = 1,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            GradientButton("NEW PUZZLE", Modifier.fillMaxWidth(), brush = MgpBrushes.Glass) { restart() }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.isComplete) {
        ResultDialog(
            emoji = "🧠",
            title = "All Found!",
            message = "You found every ${state.theme.lowercase()} word in ${formatDuration(seconds * 1000L)}.",
            reward = rewardFor(seconds),
            primaryText = "NEXT PUZZLE",
            onPrimary = { restart() },
            secondaryText = "HOME",
            onSecondary = onBack,
        )
    }
}
