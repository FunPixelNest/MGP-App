package com.funpixelnest.mgp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funpixelnest.mgp.games.cards.Card
import com.funpixelnest.mgp.ui.theme.MgpColors

const val CARD_ASPECT = 1.4f

@Composable
fun PlayingCardView(
    card: Card?,
    faceUp: Boolean,
    width: Dp,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val height = width * CARD_ASPECT
    val shape = RoundedCornerShape(width * 0.12f)
    val base = modifier
        .size(width, height)
        .shadow(3.dp, shape)
        .clip(shape)
    if (card == null || !faceUp) {
        CardBack(base, width)
        return
    }
    val density = LocalDensity.current
    val color = if (card.suit.isRed) MgpColors.CardRed else MgpColors.CardBlack
    val rankSize = with(density) { (width * 0.30f).toSp() }
    val smallSuit = with(density) { (width * 0.24f).toSp() }
    val bigSuit = with(density) { (width * 0.52f).toSp() }
    Box(
        base
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))))
            .border(if (highlighted) 2.5.dp else 0.5.dp, if (highlighted) MgpColors.Gold else Color(0x33000000), shape),
    ) {
        Column(Modifier.padding(start = width * 0.08f, top = width * 0.04f)) {
            Text(card.rankLabel, color = color, fontWeight = FontWeight.Black, style = TextStyle(fontSize = rankSize, lineHeight = rankSize))
            Text(card.suit.symbol, color = color, style = TextStyle(fontSize = smallSuit, lineHeight = smallSuit))
        }
        Text(
            card.suit.symbol,
            color = color,
            style = TextStyle(fontSize = bigSuit, lineHeight = bigSuit),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = width * 0.08f, bottom = width * 0.04f),
        )
    }
}

@Composable
private fun CardBack(modifier: Modifier, width: Dp) {
    val inner = RoundedCornerShape(width * 0.08f)
    val density = LocalDensity.current
    Box(
        modifier
            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFDB2777))))
            .border(0.5.dp, Color(0x55000000), RoundedCornerShape(width * 0.12f))
            .padding(width * 0.08f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.5f), inner)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)), inner),
            contentAlignment = Alignment.Center,
        ) {
            val s = with(density) { (width * 0.36f).toSp() }
            Text("✦", color = Color.White.copy(alpha = 0.85f), style = TextStyle(fontSize = s, lineHeight = s))
        }
    }
}

/** An empty outlined place where a card can go. */
@Composable
fun CardSlot(width: Dp, modifier: Modifier = Modifier, label: String = "") {
    val shape = RoundedCornerShape(width * 0.12f)
    val density = LocalDensity.current
    Box(
        modifier
            .size(width, width * CARD_ASPECT)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        val s = with(density) { (width * 0.4f).toSp() }
        Text(label, color = Color.White.copy(alpha = 0.35f), style = TextStyle(fontSize = s, lineHeight = s))
    }
}
