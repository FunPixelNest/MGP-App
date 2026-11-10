package com.funpixelnest.mgp

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
import com.funpixelnest.mgp.ui.theme.MgpTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen to /screenshots when run with `./gradlew testDebugUnitTest -Pscreenshots`.
 * Without that flag the captures are skipped and these tests just check the screens render.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun newStore(claimWelcome: Boolean = true) =
        PlayerStore(ApplicationProvider.getApplicationContext()).also { if (claimWelcome) it.claimWelcomeBonus() }

    private fun shot(name: String, interact: () -> Unit = {}, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { MgpTheme { AppBackground { content() } } }
        compose.mainClock.advanceTimeBy(1_500)
        interact()
        compose.mainClock.advanceTimeBy(2_500)
        captureScreenRoboImage("../screenshots/$name.png")
    }

    private fun click(text: String, substring: Boolean = true) {
        compose.onAllNodesWithText(text, substring = substring).onFirst().performClick()
        compose.mainClock.advanceTimeBy(600)
    }

    @Test
    fun welcomeBonus() {
        val store = newStore(claimWelcome = false)
        shot("01_welcome_bonus") { HomeScreen(store, {}, {}, {}) }
    }

    @Test
    fun home() {
        val store = newStore()
        shot("02_home") { HomeScreen(store, {}, {}, {}) }
    }

    @Test
    fun dailyBonus() {
        val store = newStore()
        shot("03_daily_bonus") { DailyBonusScreen(store) {} }
    }

    @Test
    fun spinWheel() {
        val store = newStore()
        shot("04_spin_wheel") { SpinWheelScreen(store) {} }
    }

    @Test
    fun blockPuzzle() {
        val store = newStore()
        shot("05_block_puzzle") { BlockPuzzleScreen(store) {} }
    }

    @Test
    fun yatzy() {
        val store = newStore()
        shot("06_yatzy", interact = { click("ROLL DICE") }) { YatzyScreen(store) {} }
    }

    @Test
    fun solitaire() {
        val store = newStore()
        shot("07_solitaire") { SolitaireScreen(store) {} }
    }

    @Test
    fun bingo() {
        val store = newStore()
        shot("08_bingo", interact = {
            click("START GAME")
            compose.mainClock.advanceTimeBy(14_000)
        }) { BingoScreen(store) {} }
    }

    @Test
    fun blackjack() {
        val store = newStore()
        shot("09_blackjack", interact = {
            click("100", substring = false)
            click("DEAL")
        }) { BlackjackScreen(store) {} }
    }

    @Test
    fun wordSearch() {
        val store = newStore()
        shot("10_word_search") { WordSearchScreen(store) {} }
    }
}
