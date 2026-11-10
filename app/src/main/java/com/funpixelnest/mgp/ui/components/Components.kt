package com.funpixelnest.mgp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.funpixelnest.mgp.ui.theme.MgpBrushes
import com.funpixelnest.mgp.ui.theme.MgpColors
import kotlinx.coroutines.delay
import java.util.Locale

fun formatCoins(value: Int): String = String.format(Locale.US, "%,d", value)

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

/** Current time in millis, ticking every [periodMs]. */
@Composable
fun rememberNow(periodMs: Long = 1000L): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(periodMs)
        value = System.currentTimeMillis()
    }
}

/** Click with a springy "squish" instead of a ripple. */
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/** A gentle infinite pulse between 1 and [max]. */
@Composable
fun rememberPulse(max: Float = 1.06f, durationMs: Int = 700): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val value by transition.animateFloat(
        initialValue = 1f,
        targetValue = max,
        animationSpec = infiniteRepeatable(tween(durationMs), RepeatMode.Reverse),
        label = "pulseValue",
    )
    return value
}

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MgpColors.BgTop, MgpColors.BgMid, MgpColors.BgBottom))),
    ) {
        Box(
            Modifier
                .size(360.dp)
                .offset((-140).dp, (-100).dp)
                .background(Brush.radialGradient(listOf(MgpColors.Pink.copy(alpha = 0.28f), Color.Transparent)), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(420.dp)
                .offset(160.dp, 140.dp)
                .background(Brush.radialGradient(listOf(MgpColors.Cyan.copy(alpha = 0.18f), Color.Transparent)), CircleShape),
        )
        Box(Modifier.fillMaxSize().systemBarsPadding(), content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(MgpBrushes.Glass)
            .border(1.dp, MgpColors.GlassStroke, shape),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    brush: Brush = MgpBrushes.Primary,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    fontSize: Int = 17,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .bouncyClick(enabled, onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(brush)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)))
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .bouncyClick(onClick = onClick)
            .clip(CircleShape)
            .background(MgpColors.GlassStrong)
            .border(1.dp, MgpColors.GlassStroke, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(coins, animationSpec = tween(900), label = "coins")
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.5.dp, MgpBrushes.Gold, shape)
            .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(size = 24.dp)
        Spacer(Modifier.width(6.dp))
        Text(formatCoins(animated), color = MgpColors.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

/** A drawn gold coin (no image assets needed). */
@Composable
fun CoinIcon(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MgpBrushes.Gold)
            .border(size * 0.08f, Color(0xFFB45309), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$", color = Color(0xFF92400E), fontWeight = FontWeight.Black, fontSize = (size.value * 0.55f).sp)
    }
}

@Composable
fun GameTopBar(title: String, coins: Int, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        CoinPill(coins)
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier, accent: Color = Color.White) {
    GlassCard(modifier, corner = 16.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label.uppercase(), color = MgpColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Rotating light rays, used behind rewards. */
@Composable
fun SunburstRays(modifier: Modifier = Modifier, color: Color = MgpColors.Gold) {
    val transition = rememberInfiniteTransition(label = "rays")
    val angle by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(12000, easing = androidx.compose.animation.core.LinearEasing)), label = "rayAngle",
    )
    androidx.compose.foundation.Canvas(modifier.rotate(angle)) {
        val rays = 12
        val r = size.minDimension / 2
        for (i in 0 until rays) {
            drawArc(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center = center, radius = r),
                startAngle = i * (360f / rays),
                sweepAngle = 360f / rays / 2,
                useCenter = true,
            )
        }
    }
}

/**
 * The end-of-game / reward popup.
 */
@Composable
fun ResultDialog(
    emoji: String,
    title: String,
    message: String,
    reward: Int,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    onDismiss: () -> Unit = onSecondary ?: onPrimary,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = false)) {
        val pop = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        val shape = RoundedCornerShape(30.dp)
        Column(
            Modifier
                .scale(pop.value)
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1A6E), Color(0xFF140C38))))
                .border(2.dp, MgpBrushes.Primary, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                if (reward > 0) SunburstRays(Modifier.fillMaxSize())
                Text(emoji, fontSize = 64.sp)
            }
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MgpColors.TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
            if (reward > 0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinIcon(28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("+${formatCoins(reward)}", color = MgpColors.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(22.dp))
            GradientButton(primaryText, Modifier.fillMaxWidth(), onClick = onPrimary)
            if (secondaryText != null && onSecondary != null) {
                Spacer(Modifier.height(10.dp))
                GradientButton(secondaryText, Modifier.fillMaxWidth(), brush = MgpBrushes.Glass, onClick = onSecondary)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MgpBrushes.Primary),
        )
        Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}
