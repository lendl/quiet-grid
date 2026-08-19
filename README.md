# <img src="docs/logo.svg" alt="" width="32" height="32" valign="middle"> Quiet Grid: Logic Puzzles

<a href="https://play.google.com/store/apps/details?id=com.quietgrid.app">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="60">
</a>

A privacy-first logic puzzle app for Android, built natively with Kotlin and Jetpack Compose.

No ads. No account. No internet required. Everything stays on your device.

## Puzzles

| Game | Status |
| --- | --- |
| Chimp Test | Available |
| Takuzu | Available |
| Nonogram | Beta |
| Minesweeper | Available |
| Sudoku | Available |
| Word Search | Available |
| Block Fill | Beta |
| Word Guess | Available |
| Animal Doku | Beta |

Each puzzle type has Easy, Medium, Hard, and Expert difficulty levels.

## Languages

English, Dutch, German, French, Spanish.

## Getting Started

### Requirements

- [Android Studio](https://developer.android.com/studio) (or a JDK 17 + Android SDK setup for command-line builds)
- An Android emulator, or a physical Android device (minSdk 26 / Android 8.0+)

> Android only. iOS and web are not supported.

### Build and run

```bash
./gradlew installDebug
```

Or open the project root in Android Studio and run the `app` configuration.

## Puzzle generation

Puzzle banks are generated offline by the `:cli` module (backed by shared rules in `:engine`) and committed as static assets — never generated on-device.

```bash
./gradlew :cli:run --args="generate --game <game> --difficulty <difficulty> [--count <n>] [--out <dir>] [--locale <locale>]"
```

| Flag | Required | Default | Notes |
| --- | --- | --- | --- |
| `--game` | yes | — | `sudoku`, `takuzu`, `nonogram`, `wordsearch`, `wordguess` |
| `--difficulty` | yes | — | `easy`, `medium`, `hard`, `expert` |
| `--count` | no | `1` | number of puzzles to attempt |
| `--out` | no | `app/src/main/assets` | output dir; merges into that game's `*_puzzles.json` |
| `--locale` | no | `en` | `wordsearch` and `wordguess` only — `en`, `nl`, `de`, `fr`, `es` |

`nonogram` requires existing seed puzzles already present in `<out>/nonogram_puzzles.json` (variants are generated from those seeds). Generation is deduplicated per game (and per locale, for `wordsearch`/`wordguess`) via state files under `<out>/.generation-state/`.

Examples:

```bash
# 20 Dutch word search puzzles, medium difficulty
./gradlew :cli:run --args="generate --game wordsearch --difficulty medium --count 20 --locale nl"

# 10 hard sudoku puzzles
./gradlew :cli:run --args="generate --game sudoku --difficulty hard --count 10"
```

## Privacy

All data is stored on-device. No network requests are made. See [PRIVACY.md](PRIVACY.md).

## License

[GNU General Public License v3.0](LICENSE)
