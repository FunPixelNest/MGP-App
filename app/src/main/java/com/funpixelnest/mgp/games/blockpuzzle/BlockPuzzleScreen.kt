package com.funpixelnest.mgp.games.blockpuzzle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.GameInfo
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.StatChip
import com.funpixelnest.mgp.ui.components.formatCoins
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlin.math.roundToInt

private val BLOCK_COLORS = listOf(
    Color.Transparent,
    Color(0xFFFF4D6D), Color(0xFFFF9F1C), Color(0xFFFFD60A), Color(0xFF2EC4B6),
    Color(0xFF3A86FF), Color(0xFF8338EC), Color(0xFFFF4ECD),
)

/** How far (in cells) the dragged piece floats above the finger so it stays visible. */
private const val LIFT_CELLS = 1.6f

@Composable
fun BlockPuzzleScreen(store: PlayerStore, onBack: () -> Unit) {
    val gameId = GameInfo.BLOCK_PUZZLE.id
    var state by remember { mutableStateOf(BlockPuzzleState.newGame()) }
    var boardRect by remember { mutableStateOf(Rect.Zero) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val slotRects = remember { mutableStateListOf(Rect.Zero, Rect.Zero, Rect.Zero) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var popupText by remember { mutableStateOf("") }
    var popupId by remember { mutableIntStateOf(0) }
    var rewarded by remember { mutableStateOf(false) }
    var bestAtStart by remember { mutableIntStateOf(store.bestScore(gameId)) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    fun cellPx(): Float = boardRect.width / BOARD_SIZE

    fun targetFor(piece: Piece): Pair<Int, Int>? {
        val cell = cellPx()
        if (cell <= 0f) return null
        val left = finger.x - piece.cols * cell / 2
        val top = finger.y - piece.rows * cell - cell * LIFT_CELLS
        val col = ((left - boardRect.left) / cell).roundToInt()
        val row = ((top - boardRect.top) / cell).roundToInt()
        return row to col
    }

    fun drop() {
        val index = dragIndex
        dragIndex = -1
        val piece = state.tray.getOrNull(index) ?: return
        val (row, col) = targetFor(piece) ?: return
        val next = state.place(index, row, col) ?: return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (next.lastLines > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            popupText = buildString {
                append("+${next.lastPoints}")
                if (next.combo > 1) append("  COMBO x${next.combo}!")
                else if (next.lastLines > 1) append("  ${next.lastLines} LINES!")
            }
            popupId++
        }
        state = next
    }

    fun restart() {
        state = BlockPuzzleState.newGame()
        rewarded = false
        bestAtStart = store.bestScore(gameId)
    }

    LaunchedEffect(state.isOver) {
        if (state.isOver && !rewarded) {
            rewarded = true
            store.submitScore(gameId, state.score)
            store.addCoins(state.score / 10)
        }
    }

    val dragPiece = state.tray.getOrNull(dragIndex)
    val target = dragPiece?.let { targetFor(it) }
    val canDrop = dragPiece != null && target != null && state.canPlace(dragPiece, target.first, target.second)
    val ghostCells: Set<Int> = if (canDrop) {
        dragPiece!!.cells.map { (r, c) -> (target!!.first + r) * BOARD_SIZE + target.second + c }.toSet()
    } else emptySet()
    val clearCells: Set<Int> = if (canDrop) state.previewClear(dragPiece!!, target!!.first, target.second) else emptySet()

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar("Block Puzzle", store.coins, onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip("Score", formatCoins(state.score), Modifier.weight(1f), accent = MgpColors.Gold)
                StatChip("Best", formatCoins(maxOf(bestAtStart, state.score)), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D0A26))
                    .border(2.dp, Brush.linearGradient(listOf(MgpColors.Purple, MgpColors.Pink)), RoundedCornerShape(20.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.fillMaxSize().onGloballyPositioned { boardRect = it.boundsInRoot() }) {
                    for (r in 0 until BOARD_SIZE) {
                        Row(Modifier.weight(1f)) {
                            for (c in 0 until BOARD_SIZE) {
                                val idx = r * BOARD_SIZE + c
                                BlockCell(
                                    color = state.board[idx],
                                    ghost = if (idx in ghostCells) dragPiece!!.color else 0,
                                    glow = idx in clearCells,
                                    modifier = Modifier.weight(1f).fillMaxHeight().padding(1.5.dp),
                                )
                            }
                        }
                    }
                }
                FloatingPopup(popupText, popupId)
            }

            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                state.tray.forEachIndexed { i, piece ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { slotRects[i] = it.boundsInRoot() }
                            .pointerInput(i, piece?.id) {
                                if (piece == null) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    dragIndex = i
                                    finger = slotRects[i].topLeft + down.position
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        finger = slotRects[i].topLeft + change.position
                                        change.consume()
                                    }
                                    drop()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (piece != null && dragIndex != i) {
                            PieceView(
                                piece,
                                cell = 22.dp,
                                modifier = Modifier.alpha(if (state.fitsAnywhere(piece)) 1f else 0.3f),
                            )
                        }
                    }
                }
            }
            Text(
                "Drag blocks onto the board. Fill rows or columns to clear them!",
                color = MgpColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
            )
        }

        // The piece being dragged, drawn at full board size above the finger.
        if (dragPiece != null && cellPx() > 0f) {
            val cell = cellPx()
            val cellDp = with(density) { cell.toDp() }
            Box(
                Modifier.offset {
                    IntOffset(
                        (finger.x - dragPiece.cols * cell / 2 - rootOffset.x).roundToInt(),
                        (finger.y - dragPiece.rows * cell - cell * LIFT_CELLS - rootOffset.y).roundToInt(),
                    )
                },
            ) {
                PieceView(dragPiece, cellDp, Modifier.graphicsLayer { alpha = 0.92f; scaleX = 1.04f; scaleY = 1.04f })
            }
        }
    }

    if (state.isOver) {
        val isBest = state.score > bestAtStart
        ResultDialog(
            emoji = if (isBest) "🏆" else "🧱",
            title = if (isBest) "New Best!" else "No More Moves",
            message = "You scored ${formatCoins(state.score)} points.",
            reward = state.score / 10,
            primaryText = "PLAY AGAIN",
            onPrimary = { restart() },
            secondaryText = "HOME",
            onSecondary = onBack,
        )
    }
}

@Composable
private fun BlockCell(color: Int, ghost: Int, glow: Boolean, modifier: Modifier) {
    val target = when {
        color > 0 -> BLOCK_COLORS[color]
        ghost > 0 -> BLOCK_COLORS[ghost].copy(alpha = 0.4f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val animated by animateColorAsState(target, tween(160), label = "cell")
    val shape = RoundedCornerShape(6.dp)
    var m = modifier.clip(shape).background(animated)
    if (color > 0) {
        m = m.background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.2f))))
    }
    if (glow) m = m.border(2.dp, Color.White, shape)
    Box(m)
}

@Composable
private fun PieceView(piece: Piece, cell: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(cell * piece.cols, cell * piece.rows)) {
        piece.cells.forEach { (r, c) ->
            Box(
                Modifier
                    .offset(cell * c, cell * r)
                    .size(cell)
                    .padding(1.5.dp)
                    .clip(RoundedCornerShape(cell * 0.2f))
                    .background(BLOCK_COLORS[piece.color])
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.2f)))),
            )
        }
    }
}

/** "+30 COMBO x2!" text that floats up and fades each time [id] changes. */
@Composable
private fun FloatingPopup(text: String, id: Int) {
    if (id == 0) return
    val progress = remember(id) { Animatable(0f) }
    LaunchedEffect(id) { progress.animateTo(1f, tween(1100)) }
    Text(
        text,
        color = MgpColors.Gold,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.graphicsLayer {
            translationY = -progress.value * 160f
            alpha = 1f - progress.value
            val s = 0.8f + progress.value * 0.4f
            scaleX = s
            scaleY = s
        },
    )
}
