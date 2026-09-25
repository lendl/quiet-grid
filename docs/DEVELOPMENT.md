# Development

## Requirements

- [Android Studio](https://developer.android.com/studio) (or a JDK 17 + Android SDK setup for command-line builds)
- An Android emulator, or a physical Android device (minSdk 26 / Android 8.0+)

> Android only. iOS and web are not supported.

## Build and run

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
| `--game` | yes | — | `takuzu`, `nonogram`, `sudoku`, `wordsearch`, `wordguess`, `animaldoku`, `arrowescape`, `starbattle` |
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

## Testing

See [`docs/testing.md`](testing.md) for the full test suite (unit, screenshot, coverage) and test infrastructure notes.

## Store art

Store screenshots, feature graphic, and the README banner are generated from the real app rather than hand-made. See [`tools/store-art/README.md`](../tools/store-art/README.md).
