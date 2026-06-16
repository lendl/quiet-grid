# Engine

The engine is the offline puzzle generator and classifier. It runs outside the Expo runtime as a Node CLI and produces puzzle catalogs that ship with the app.

Load this file when working on generation, difficulty classification, catalog shape, or bucket supply problems.

## Architecture

- Engine code lives in `src/games/<id>/engine/` and shares infrastructure from `src/engine/`.
- Engine-capable games are registered in `src/engine/gameRegistry.ts` (separate from the app registry).
- The engine tsconfig (`src/engine/tsconfig.json`) compiles to CommonJS with no Expo or React Native imports allowed — keep engine code free of runtime dependencies.
- Run: `npm run engine -- --game=<id>`

## Engine registration

Each engine-backed game provides an `EngineGameDefinition` with:
- `id`, `catalogPath`, `entryIdPrefix`
- `listAllowedSizes()` — the size matrix for generation
- `pickTargetDifficulty()` — chooses a difficulty target for each generation attempt
- `generateOne(size, targetDifficulty)` — produces one candidate puzzle
- `getEntryDedupeKey(entry)` — returns a stable key to prevent catalog duplicates
- `reclassifyEntries(entries)` — re-scores all existing entries when scoring weights change

## Generation pipeline

Each puzzle goes through every stage; failure at any stage discards the candidate:

1. **Generate** — produce a candidate (grid, mask, or template) for the target size and difficulty
2. **Solve** — run the logical solver; discard if the solver stalls (puzzle is not logically solvable with the approved technique set)
3. **Measure** — collect difficulty metrics from the solve path (technique levels used, step counts, branching factor, etc.)
4. **Score** — compute a numeric difficulty score from the metrics using weighted signals
5. **Classify** — map the score to a difficulty tier (easy/medium/hard/expert)
6. **Check rails** — verify per-difficulty safety constraints (opening technique level, sparse move count, branching cap, etc.); discard if any rail is violated
7. **Dedupe** — skip if the deduplication key already exists in the catalog
8. **Catalog** — append to the chunked catalog file

## Difficulty classification

Classification uses three gates that must all agree before a puzzle is accepted into a bucket:

- **Technique gate** — the highest technique required must match the target difficulty tier
- **Score gate** — the weighted score must fall within the bucket's min/max range
- **Safety rails** — per-difficulty hard limits: max opening technique level, max sparse move count, max branching factor, allowed technique types

A puzzle that passes score but fails rails is discarded. A puzzle that fails technique gate is discarded even if score and rails pass.

## Difficulty balancing

This is the most common source of problems in engine-backed games — both when setting up a new game and when tuning an existing one.

### The core problem

If the score weights or rail definitions don't match the actual distribution of generated puzzles, buckets fill unevenly — some overflow while others stay empty. An empty bucket means that difficulty is unplayable. This can happen at any point: initial setup, after adding a new technique to the solver, or after changing generation parameters.

### What to check

- **Verify bucket supply by bulk-generating** — run at target-like volume (hundreds or thousands of attempts) and check the distribution across buckets. If a bucket receives fewer than ~20 puzzles per size/difficulty combination, the weights or rails need tuning.
- **Check per-size separately** — difficulty thresholds must be calibrated per grid size. A score that is hard at 6×6 may be easy at 10×10. Size-based score offsets are usually necessary.
- **Watch opening technique level** — the "opening" gate (technique level required in the first 20–30% of moves) frequently misclassifies puzzles as easier than they are. Tune `maxOpeningTipLevel` rails first when puzzles feel harder than their label.
- **Monitor stall rate** — if the solver stalls frequently, the generator is producing puzzles that require techniques the solver does not implement. Either extend the solver or constrain generation. Adding a new technique to the solver can shift the stall rate significantly.
- **Reclassify after weight changes** — when score weights change, run `reclassifyEntries()` on the existing catalog rather than regenerating from scratch.

### Common failure modes

- **Lopsided buckets** — generation consistently produces the wrong difficulty tier because score weights do not match the target distribution. Fix: instrument the metrics, plot the score distribution, and adjust weights to center each bucket.
- **Expert starvation** — expert puzzles are rare by definition. If the expert bucket is empty, check whether the size ceiling gate (expert may require the largest grid size) is too strict.
- **Easy overflow** — if the generator always produces easy puzzles when targeting hard, the mask or template generation is not sparse enough. Make the generator produce denser/harder candidates before scoring.
- **Misclassified openings** — a puzzle labeled easy or medium that requires a hard technique in the first move. Lower `maxOpeningTipLevel` rails or adjust early-move generation logic.
- **Cross-size inconsistency** — a technique that is hard at size 8 becomes trivial at size 10 because there are more constraints. Score offsets must account for this.

## Catalog structure

- Catalogs are chunked into files of ~500 entries to keep TypeScript files below 10 MB.
- `src/engine/catalog.ts` provides `buildChunkedCatalogContent()` which writes the chunked format.
- Entries are loaded at runtime via `readGameCatalog()` → `readChunkedCatalog()` using dynamic require.
- Each entry must have a stable `id` (prefix + incrementing number) and a `dedupeKey`.

Takuzu entry shape (hex-encoded):
```
{ id: 'p3', size: 10, difficulty: 'hard', solution: '<hex>', mask: '<hex>' }
```

Nonogram entry shape:
```
{ id: 'n1', difficulty: 'hard', rows: 10, cols: 5, solution: boolean[][] }
```

## File map

- `src/engine/gameRegistry.ts` — engine game registration
- `src/engine/catalog.ts` — chunked catalog writer
- `src/engine/difficultyConfig.ts` — shared bucket definitions and rail types
- `src/games/<id>/engine/generator.ts` — puzzle generator
- `src/games/<id>/engine/classifier.ts` or `difficulty.ts` — metrics, scoring, classification
- `src/games/<id>/engine/definition.ts` — implements `EngineGameDefinition`
- `src/games/<id>/puzzles/` — generated catalog files

## Mistakes to avoid

- Do not wire or ship app surfaces that depend on a difficulty bucket before verifying that bucket has sufficient supply.
- Do not change score weights without running `reclassifyEntries()` on the existing catalog.
- Do not ship a game with empty or near-empty buckets — a difficulty with fewer than ~20 puzzles per size is not launchable.
- Do not import Expo or React Native in engine code — the engine tsconfig will not resolve them.
- Do not hardcode difficulty assumptions that do not survive a size change.
- Do not treat a puzzle that stalls the solver as an error to suppress — it signals a gap in the technique set.
