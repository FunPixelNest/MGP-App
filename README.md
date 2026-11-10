# MGP Games

An Android game collection written in Kotlin with Jetpack Compose. It has six games, a coin wallet,
a welcome bonus, a daily bonus and a lucky spin wheel.

## Games

| Game | How it plays | Coins earned |
|------|--------------|--------------|
| 🧩 Block Puzzle | Drag pieces onto an 8×8 board. Fill rows or columns to clear them and chain combos. | score ÷ 10 |
| 🎲 Yatzy | Scandinavian Yatzy: 5 dice, 3 rolls per turn, 15 categories and a +50 upper bonus. | total ÷ 2 |
| 🃏 Solitaire | Klondike, draw 1. Tap a card to auto-move it. Includes undo and auto-finish. | 300 per win |
| 🎱 Bingo | 75-ball bingo against 2 bots. Daub the called numbers and hit BINGO first. | 60–400 (faster = more) |
| ♠️ Blackjack | Bet your coins. Hit, stand or double. Blackjack pays 3:2. | your winnings |
| 🔤 Word Search | Themed 10×10 puzzles. Drag across letters in any of 8 directions. | 100 + speed bonus |

## Bonuses

- **Welcome bonus:** 1,000 coins, shown once on first launch.
- **Daily bonus:** a 7-day streak paying 100 → 150 → 200 → 300 → 400 → 500 → 1,000. Missing a day resets the streak.
- **Lucky Spin:** a free spin every 4 hours, or an extra spin for 100 coins. Prizes range from 25 to 1,000 coins.

## Project layout

```
app/src/main/java/com/funpixelnest/mgp/
├── MainActivity.kt, MgpApp.kt      # entry point + screen navigation
├── GameInfo.kt                     # list of games shown on the home screen
├── data/PlayerStore.kt             # coins, bonuses, best scores (SharedPreferences)
├── ui/theme/                       # colours & gradients
├── ui/components/                  # buttons, coin pill, dialogs, playing cards
├── ui/home/                        # home screen
├── ui/bonus/                       # daily bonus + spin wheel
└── games/<game>/
    ├── <Game>Logic.kt              # pure Kotlin rules (unit tested, no Android code)
    └── <Game>Screen.kt             # Compose UI
```

Each game's rules are kept separate from its UI, so you can restyle a screen without touching the rules.
To add a game, add an entry to `GameInfo`, create `games/<name>/`, and add its route in `MgpApp.kt`.

## Build

Open the folder in Android Studio (Ladybug or newer) and press Run. You can also build from the command line:

```
./gradlew assembleDebug        # build the APK
./gradlew testDebugUnitTest    # run the game-logic unit tests
```

Requirements: JDK 17+, Android SDK 35, minSdk 24 (Android 7.0).
