package com.funpixelnest.mgp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.funpixelnest.mgp.data.PlayerStore
import com.funpixelnest.mgp.games.bingo.BingoScreen
import com.funpixelnest.mgp.games.blackjack.BlackjackScreen
import com.funpixelnest.mgp.games.blockpuzzle.BlockPuzzleScreen
import com.funpixelnest.mgp.games.solitaire.SolitaireScreen
import com.funpixelnest.mgp.games.wordsearch.WordSearchScreen
import com.funpixelnest.mgp.games.yatzy.YatzyScreen
import com.funpixelnest.mgp.ui.bonus.DailyBonusScreen
import com.funpixelnest.mgp.ui.bonus.SpinWheelScreen
import com.funpixelnest.mgp.ui.components.AppBackground
import com.funpixelnest.mgp.ui.home.HomeScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_DAILY = "daily"
private const val ROUTE_SPIN = "spin"

@Composable
fun MgpApp(store: PlayerStore) {
    var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }
    BackHandler(enabled = route != ROUTE_HOME) { route = ROUTE_HOME }

    AppBackground {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (targetState == ROUTE_HOME) {
                    (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.06f)) togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f))
                } else {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.88f)) togetherWith
                        fadeOut(tween(200))
                }
            },
            label = "navigation",
        ) { current ->
            val back: () -> Unit = { route = ROUTE_HOME }
            when (current) {
                ROUTE_DAILY -> DailyBonusScreen(store, back)
                ROUTE_SPIN -> SpinWheelScreen(store, back)
                GameInfo.BLOCK_PUZZLE.id -> BlockPuzzleScreen(store, back)
                GameInfo.YATZY.id -> YatzyScreen(store, back)
                GameInfo.SOLITAIRE.id -> SolitaireScreen(store, back)
                GameInfo.BINGO.id -> BingoScreen(store, back)
                GameInfo.BLACKJACK.id -> BlackjackScreen(store, back)
                GameInfo.WORD_SEARCH.id -> WordSearchScreen(store, back)
                else -> HomeScreen(
                    store = store,
                    onOpenGame = { route = it.id },
                    onOpenDaily = { route = ROUTE_DAILY },
                    onOpenSpin = { route = ROUTE_SPIN },
                )
            }
        }
    }
}
