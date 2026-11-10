package com.funpixelnest.mgp.ui.bonus

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.ui.components.GameTopBar
import com.funpixelnest.mgp.ui.components.GradientButton
import com.funpixelnest.mgp.ui.components.ResultDialog
import com.funpixelnest.mgp.ui.components.bouncyClick
import com.funpixelnest.mgp.ui.components.formatDuration
import com.funpixelnest.mgp.ui.components.rememberNow
import com.funpixelnest.mgp.ui.components.rememberPulse
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.launch
import kotlin.random.Random

private val REWARDS = listOf(50, 100, 25, 500, 75, 200, 150, 1000)
private val WEIGHTS = listOf(22, 18, 20, 4, 18, 9, 8, 1)
private val SEGMENT_COLORS = listOf(
    Color(0xFFFF4ECD), Color(0xFF8B5CF6), Color(0xFF22D3EE), Color(0xFFFF8A3D),
    Color(0xFF34D399), Color(0xFF3B82F6), Color(0xFFFF5470), Color(0xFFFFC233),
)
private const val SEGMENT = 360f / 8

private fun pickSegment(random: Random): Int {
    var roll = random.nextInt(WEIGHTS.sum())
    WEIGHTS.forEachIndexed { i, w ->
        if (roll < w) return i
        roll -= w
    }
    return 0
}

@Composable
fun SpinWheelScreen(store: PlayerStore, onBack: () -> Unit) {
    val now by rememberNow()
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var reward by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val freeReady = store.canFreeSpin(now)

    fun spin(paid: Boolean) {
        if (spinning) return
        val allowed = if (paid) store.spendCoins(PlayerStore.SPIN_COST) else store.useFreeSpin(System.currentTimeMillis())
        if (!allowed) return
        val index = pickSegment(Random.Default)
        // Segment i is centred at (i + 0.5) * SEGMENT clockwise from the top pointer.
        val landing = (360f - (index + 0.5f) * SEGMENT) + Random.nextInt(-15, 16)
        val current = rotation.value
        val target = current - (current % 360f) + 360f * 7 + landing
        spinning = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            rotation.animateTo(target, tween(durationMillis = 5000, easing = CubicBezierEasing(0.12f, 0.8f, 0.2f, 1f)))
            store.addCoins(REWARDS[index])
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            reward = REWARDS[index]
            spinning = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar("Lucky Spin", store.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Spin to win up to", color = MgpColors.TextSecondary, fontSize = 16.sp)
            Text("1,000 COINS", color = MgpColors.Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                WheelRim(Modifier.fillMaxSize())
                Wheel(Modifier.fillMaxSize().padding(14.dp).graphicsLayer { rotationZ = rotation.value })
                Box(
                    Modifier
                        .size(78.dp)
                        .scale(if (!spinning && freeReady) rememberPulse(1.08f, 500) else 1f)
                        .bouncyClick(enabled = !spinning && freeReady) { spin(paid = false) }
                        .clip(CircleShape)
                        .background(MgpBrushes.Gold)
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("SPIN", color = Color(0xFF5B2A00), fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Pointer(Modifier.align(Alignment.TopCenter).size(40.dp, 46.dp))
            }

            Spacer(Modifier.height(24.dp))
            if (freeReady) {
                GradientButton(
                    if (spinning) "Good luck..." else "FREE SPIN",
                    Modifier.fillMaxWidth(),
                    brush = MgpBrushes.Gold,
                    textColor = Color(0xFF5B2A00),
                    enabled = !spinning,
                ) { spin(paid = false) }
            } else {
                Text(
                    "Next free spin in ${formatDuration(store.freeSpinReadyAt() - now)}",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    if (spinning) "Good luck..." else "SPIN AGAIN FOR ${PlayerStore.SPIN_COST} COINS",
                    Modifier.fillMaxWidth(),
                    enabled = !spinning && store.coins >= PlayerStore.SPIN_COST,
                ) { spin(paid = true) }
            }
            Text(
                "You get a free spin every 4 hours.",
                color = MgpColors.TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }

    if (reward > 0) {
        ResultDialog(
            emoji = if (reward >= 500) "🤑" else "🎉",
            title = if (reward >= 500) "JACKPOT!" else "You won!",
            message = "The coins have been added to your wallet.",
            reward = reward,
            primaryText = "COLLECT",
            onPrimary = { reward = 0 },
        )
    }
}

@Composable
private fun Wheel(modifier: Modifier) {
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 3f, Color.Black.copy(alpha = 0.45f).toArgb())
        }
    }
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val arcSize = Size(radius * 2, radius * 2)
        val topLeft = Offset(center.x - radius, center.y - radius)
        REWARDS.indices.forEach { i ->
            val start = -90f + i * SEGMENT
            val base = SEGMENT_COLORS[i]
            drawArc(
                brush = Brush.radialGradient(listOf(base.copy(alpha = 0.75f), base), center = center, radius = radius),
                startAngle = start, sweepAngle = SEGMENT, useCenter = true, topLeft = topLeft, size = arcSize,
            )
            drawArc(
                color = Color.White.copy(alpha = 0.5f),
                startAngle = start, sweepAngle = SEGMENT, useCenter = true, topLeft = topLeft, size = arcSize,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        paint.textSize = radius * 0.15f
        REWARDS.forEachIndexed { i, value ->
            rotate(degrees = (i + 0.5f) * SEGMENT, pivot = center) {
                drawContext.canvas.nativeCanvas.drawText(value.toString(), center.x, center.y - radius * 0.62f, paint)
            }
        }
        drawCircle(Color.Black.copy(alpha = 0.18f), radius = radius * 0.3f, center = center)
    }
}

@Composable
private fun WheelRim(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "lights")
    val blink by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "blink")
    Canvas(modifier) {
        val radius = size.minDimension / 2
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE08A), Color(0xFFB45309), Color(0xFFFFE08A))), radius = radius)
        drawCircle(Color(0xFF2A1A6E), radius = radius - 6.dp.toPx())
        val lights = 16
        val lightRadius = radius - 7.dp.toPx()
        for (i in 0 until lights) {
            val angle = Math.toRadians((i * 360.0 / lights) - 90.0)
            val p = Offset(center.x + (lightRadius * kotlin.math.cos(angle)).toFloat(), center.y + (lightRadius * kotlin.math.sin(angle)).toFloat())
            val on = (i % 2 == 0) == (blink > 0.5f)
            drawCircle(if (on) Color(0xFFFFF7D6) else Color(0xFFB45309), radius = 4.dp.toPx(), center = p)
        }
    }
}

@Composable
private fun Pointer(modifier: Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.1f, 0f)
            lineTo(size.width * 0.9f, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color.White, Color(0xFFFFD25A))))
        drawPath(path, Color(0xFFB45309), style = Stroke(width = 3.dp.toPx()))
    }
}
