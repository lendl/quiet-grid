# RN puzzle engine (reference only)

Offline puzzle generator/solver code from the deleted React Native app, kept here for reference while porting generation logic to Kotlin. Not part of the Android build — not referenced by any Gradle module, not compiled, not shipped in the APK.

Extracted from git history (commit `59987f2`, last commit before the RN app was removed) as the full import-closure of `src/engine/` and each game's `src/games/<id>/engine/` — i.e. every file those generators/solvers actually depend on (types, codecs, puzzle-technique modules, bundled word search corpora, etc). Original relative paths preserved under `src/` here so imports between these files still line up; nothing else from the RN app (UI, screens, navigation) was restored.

TypeScript, depends on `better-sqlite3` (used by `src/engine/db.ts` for generation dedupe tracking) — not runnable as-is without a `package.json`/`node_modules` of its own.
