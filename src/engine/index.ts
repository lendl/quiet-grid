import type { PuzzleDifficulty } from '../games/shared/types';
import { openDb, hashDedupeKey, hasTried, recordTried } from './db';
import type { EngineGameDefinition } from './gameDefinition';
import { getEngineGameDefinition } from './gameRegistry';
import {
  readGameCatalog,
  writeGameCatalog,
} from './writer';
import { WORD_SEARCH_DIFFICULTY_CONFIG } from '../games/wordsearch/engine/constraints';
import { wordSearchSeedCorpus } from '../games/wordsearch/engine/seedCorpus';
import type { WordSearchLanguage } from '../games/wordsearch/types';
import { getWordSearchGenerationStats, resetWordSearchGenerationStats } from '../games/wordsearch/engine/generator';

const MAX_ATTEMPTS = 200;
const WORD_SEARCH_LANGUAGES = ['en', 'nl', 'de', 'fr', 'es'] as const;
const WORD_SEARCH_DIFFICULTIES = ['easy', 'medium', 'hard', 'expert'] as const;

interface ThemeEntry {
  capable: Set<PuzzleDifficulty>;
  counts: Map<PuzzleDifficulty, number>;
}

type ThemeCapabilityMap = Map<WordSearchLanguage, Map<string, ThemeEntry>>;

function hasLanguageField(value: unknown): value is { language: string } {
  if (!value || typeof value !== 'object') {
    return false;
  }
  return 'language' in value && typeof (value as { language?: unknown }).language === 'string';
}

function buildLanguageCounts(
  entries: readonly unknown[],
): Map<(typeof WORD_SEARCH_LANGUAGES)[number], number> {
  const counts = new Map<(typeof WORD_SEARCH_LANGUAGES)[number], number>(
    WORD_SEARCH_LANGUAGES.map((language) => [language, 0]),
  );

  entries.forEach((entry) => {
    if (!hasLanguageField(entry)) {
      return;
    }
    const language = entry.language as (typeof WORD_SEARCH_LANGUAGES)[number];
    if (!counts.has(language)) {
      return;
    }
    counts.set(language, (counts.get(language) ?? 0) + 1);
  });

  return counts;
}

function hasLanguageThemeFields(value: unknown): value is { language: string; themeId: string } {
  if (!value || typeof value !== 'object') {
    return false;
  }
  return typeof (value as { language?: unknown }).language === 'string'
    && typeof (value as { themeId?: unknown }).themeId === 'string';
}

function hasLanguageThemeDifficultyFields(
  value: unknown,
): value is { language: string; themeId: string; difficulty: string } {
  return hasLanguageThemeFields(value)
    && typeof (value as { difficulty?: unknown }).difficulty === 'string';
}

function normalizeWord(word: string): string {
  return word.normalize('NFKD').replace(/[^A-Za-z]/g, '').toUpperCase();
}

function computeThemeCapability(themeWords: readonly string[]): Set<PuzzleDifficulty> {
  const capable = new Set<PuzzleDifficulty>();
  for (const difficulty of WORD_SEARCH_DIFFICULTIES) {
    const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
    const qualifyingCount = [...new Set(themeWords.map(normalizeWord))]
      .filter((w) => w.length >= config.wordLengthProfile.min && w.length <= config.wordLengthProfile.max)
      .length;
    if (qualifyingCount >= config.wordCount.min) {
      capable.add(difficulty);
    }
  }
  return capable;
}

function buildThemeCapabilityAndCounts(entries: readonly unknown[]): ThemeCapabilityMap {
  const map: ThemeCapabilityMap = new Map(
    WORD_SEARCH_LANGUAGES.map((language) => [
      language,
      new Map(
        (wordSearchSeedCorpus[language] ?? []).map((theme) => [
          theme.themeId,
          {
            capable: computeThemeCapability(theme.words),
            counts: new Map(WORD_SEARCH_DIFFICULTIES.map((d) => [d, 0])),
          },
        ]),
      ),
    ]),
  );

  for (const entry of entries) {
    if (!hasLanguageThemeDifficultyFields(entry)) {
      continue;
    }
    const language = entry.language as WordSearchLanguage;
    const difficulty = entry.difficulty as PuzzleDifficulty;
    const themeEntry = map.get(language)?.get(entry.themeId);
    if (!themeEntry) {
      continue;
    }
    themeEntry.counts.set(difficulty, (themeEntry.counts.get(difficulty) ?? 0) + 1);
  }

  return map;
}

function getLeastRepresentedLanguages(
  counts: ReadonlyMap<(typeof WORD_SEARCH_LANGUAGES)[number], number>,
): Set<(typeof WORD_SEARCH_LANGUAGES)[number]> {
  let minimum = Number.POSITIVE_INFINITY;
  counts.forEach((value) => {
    if (value < minimum) {
      minimum = value;
    }
  });

  return new Set(
    [...counts.entries()]
      .filter(([, value]) => value === minimum)
      .map(([language]) => language),
  );
}

function getMinimumLanguageCount(
  counts: ReadonlyMap<(typeof WORD_SEARCH_LANGUAGES)[number], number>,
): number {
  let minimum = Number.POSITIVE_INFINITY;
  counts.forEach((value) => {
    if (value < minimum) {
      minimum = value;
    }
  });
  return minimum;
}

function getPreferredLanguagesForBalance(
  counts: ReadonlyMap<(typeof WORD_SEARCH_LANGUAGES)[number], number>,
  attempt: number,
): (typeof WORD_SEARCH_LANGUAGES)[number][] {
  const minimum = getMinimumLanguageCount(counts);
  const allowedGap = Math.floor((attempt - 1) / 30);
  const preferred = [...counts.entries()]
    .filter(([, count]) => count <= minimum + allowedGap)
    .sort((left, right) => left[1] - right[1])
    .map(([language]) => language);

  if (preferred.length > 0) {
    return preferred;
  }

  return [...getLeastRepresentedLanguages(counts)];
}

function getPreferredThemesForBalance(
  capabilityMap: ThemeCapabilityMap,
  targetDifficulty: PuzzleDifficulty,
  attempt: number,
): Map<WordSearchLanguage, string[]> {
  const allowedGap = Math.floor((attempt - 1) / 30);
  const preferred = new Map<WordSearchLanguage, string[]>();

  capabilityMap.forEach((themeMap, language) => {
    // Only balance among themes that can actually generate the target difficulty.
    const capable = [...themeMap.entries()]
      .filter(([, entry]) => entry.capable.has(targetDifficulty));

    if (capable.length === 0) {
      return;
    }

    let minimum = Number.POSITIVE_INFINITY;
    for (const [, entry] of capable) {
      const count = entry.counts.get(targetDifficulty) ?? 0;
      if (count < minimum) {
        minimum = count;
      }
    }

    const selected = capable
      .filter(([, entry]) => (entry.counts.get(targetDifficulty) ?? 0) <= minimum + allowedGap)
      .sort((a, b) => (a[1].counts.get(targetDifficulty) ?? 0) - (b[1].counts.get(targetDifficulty) ?? 0))
      .map(([themeId]) => themeId);

    if (selected.length > 0) {
      preferred.set(language, selected);
    }
  });

  return preferred;
}

function printHelp(): void {
  const knownGames = ['takuzu', 'nonogram', 'sudoku', 'wordsearch'];
  console.log(`
Usage: tsx src/engine/index.ts [options] [count]

Generates puzzle catalog entries for a given game.

Arguments:
  count                      Number of puzzles to generate (default: 1)

Options:
  --game <id>                Game to generate puzzles for (default: takuzu)
                             Known games: ${knownGames.join(', ')}
  --size <n>                 Force a specific puzzle size (game-dependent)
  --difficulty <level>       Force a specific difficulty: easy, medium, hard, expert
  --language <code>          Force a specific language (wordsearch only): en, nl, de, fr, es
  --replace                  Replace the existing catalog instead of appending
  --reclassify-existing      Reclassify existing catalog entries without generating new ones
  --help, -h                 Show this help message
`);
}

function parseGameArg(): string {
  const gameIndex = process.argv.findIndex((arg) => arg === '--game');
  const pairedArg = gameIndex >= 0 ? process.argv[gameIndex + 1] : null;
  const inlineArg = process.argv.find((arg) => arg.startsWith('--game='))?.split('=')[1];
  return inlineArg ?? pairedArg ?? 'takuzu';
}

function parseRequestedCount(): number {
  const requestedCountArg = process.argv.slice(2).find((arg) => /^\d+$/.test(arg));
  return Math.max(1, Number(requestedCountArg ?? '1'));
}

function parseSizeArg(): number | null {
  const sizeIndex = process.argv.findIndex((arg) => arg === '--size');
  const pairedArg = sizeIndex >= 0 ? process.argv[sizeIndex + 1] : null;
  const inlineArg = process.argv.find((arg) => arg.startsWith('--size='))?.split('=')[1];
  const rawSize = inlineArg ?? pairedArg;
  if (!rawSize || !/^\d+$/.test(rawSize)) {
    return null;
  }

  return Number(rawSize);
}

function parseDifficultyArg(): PuzzleDifficulty | null {
  const difficultyIndex = process.argv.findIndex((arg) => arg === '--difficulty');
  const pairedArg = difficultyIndex >= 0 ? process.argv[difficultyIndex + 1] : null;
  const inlineArg = process.argv.find((arg) => arg.startsWith('--difficulty='))?.split('=')[1];
  const rawDifficulty = inlineArg ?? pairedArg;
  return rawDifficulty === 'easy'
    || rawDifficulty === 'medium'
    || rawDifficulty === 'hard'
    || rawDifficulty === 'expert'
    ? rawDifficulty
    : null;
}

function parseLanguageArg(): string | null {
  const languageIndex = process.argv.findIndex((arg) => arg === '--language');
  const pairedArg = languageIndex >= 0 ? process.argv[languageIndex + 1] : null;
  const inlineArg = process.argv.find((arg) => arg.startsWith('--language='))?.split('=')[1];
  return inlineArg ?? pairedArg ?? null;
}

function formatSizeLabel(
  game: EngineGameDefinition,
  sizes: readonly number[],
): string {
  return sizes.flatMap((size) => game.describeSizeOptions?.(size) ?? [`${size}x${size}`]).join('/');
}

function reclassifyExistingCatalog(gameId: string): void {
  console.log(`\nReclassifying existing ${gameId} catalog entries...`);
  const game = getEngineGameDefinition(gameId);
  const existingEntries = readGameCatalog(game);
  console.log(`Found ${existingEntries.length} existing ${game.title} puzzle(s) in the catalog.`);
  const rewrittenEntries = game.reclassifyEntries(existingEntries);
  const droppedCount = existingEntries.length - rewrittenEntries.length;
  writeGameCatalog(game, rewrittenEntries);

  console.log(`\nReclassified ${rewrittenEntries.length} existing ${game.title} puzzle(s).`);
  if (droppedCount > 0) {
    console.log(`Dropped ${droppedCount} existing puzzle(s) that no longer fit a supported difficulty bucket.`);
  }
}

function main(): void {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    printHelp();
    process.exit(0);
  }

  const game = getEngineGameDefinition(parseGameArg());
  if (game.id === 'wordsearch') {
    resetWordSearchGenerationStats();
  }
  const requestedCount = parseRequestedCount();
  const replaceCatalog = process.argv.includes('--replace');
  const reclassifyExisting = process.argv.includes('--reclassify-existing');
  const forcedSize = parseSizeArg();
  const forcedDifficulty = parseDifficultyArg();
  const forcedLanguage = parseLanguageArg();
  const allowedSizes = game.listAllowedSizes();

  if (forcedSize !== null && !allowedSizes.includes(forcedSize)) {
    throw new Error(
      `Game ${game.id} does not support size ${forcedSize}. Allowed sizes: ${allowedSizes.join(', ')}`,
    );
  }

  if (forcedDifficulty !== null) {
    const difficultyAllowed = (forcedSize === null ? allowedSizes : [forcedSize])
      .some((size) => game.listAllowedDifficulties(size).includes(forcedDifficulty));
    if (!difficultyAllowed) {
      throw new Error(
        `Game ${game.id} does not support difficulty ${forcedDifficulty} for the selected size filter.`,
      );
    }
  }

  if (forcedLanguage !== null) {
    if (game.id !== 'wordsearch') {
      throw new Error(`--language is only supported for game wordsearch. Received game ${game.id}.`);
    }
    if (!WORD_SEARCH_LANGUAGES.includes(forcedLanguage as (typeof WORD_SEARCH_LANGUAGES)[number])) {
      throw new Error(
        `Unsupported language "${forcedLanguage}" for wordsearch. Allowed: ${WORD_SEARCH_LANGUAGES.join(', ')}`,
      );
    }
  }

  const selectedSizes = forcedSize !== null
    ? [forcedSize]
    : forcedDifficulty !== null
      ? allowedSizes.filter((size) => game.listAllowedDifficulties(size).includes(forcedDifficulty))
      : [...allowedSizes];
  const db = openDb();
  let totalGenerated = 0;
  let catalogEntries = readGameCatalog(game);
  if (replaceCatalog) {
    catalogEntries = [];
  }
  const catalogDedupeKeys = new Set(catalogEntries.map((entry) => game.getEntryDedupeKey(entry)));
  let nextCatalogId = catalogEntries
    .map((entry) => {
      const suffix = entry.id.slice(game.entryIdPrefix.length);
      return /^\d+$/.test(suffix) ? Number(suffix) : 0;
    })
    .reduce((max, current) => Math.max(max, current), 0);

  if (reclassifyExisting) {
    reclassifyExistingCatalog(game.id);
    db.close();
    return;
  }

  const languageCounts = game.id === 'wordsearch'
    ? buildLanguageCounts(catalogEntries)
    : null;
  const themeCapabilityMap = game.id === 'wordsearch'
    ? buildThemeCapabilityAndCounts(catalogEntries)
    : null;

  const sizeLabel = forcedSize === null
    ? formatSizeLabel(game, allowedSizes)
    : (game.describeSizeOptions?.(forcedSize) ?? [`${forcedSize}x${forcedSize}`]).join('/');
  console.log(`\nGenerating ${requestedCount} scored ${game.title} ${sizeLabel} puzzle(s)...`);

  while (totalGenerated < requestedCount) {
    let generated = false;

    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
      const puzzleSize = selectedSizes[Math.floor(Math.random() * selectedSizes.length)];
      const targetDifficulty = forcedDifficulty ?? game.pickTargetDifficulty(puzzleSize);
      const preferredLanguages = forcedLanguage
        ? [forcedLanguage]
        : (languageCounts ? getPreferredLanguagesForBalance(languageCounts, attempt) : undefined);
      const preferredThemeIds = themeCapabilityMap
        ? [...new Set(
          [...getPreferredThemesForBalance(themeCapabilityMap, targetDifficulty, attempt).entries()]
            .filter(([language]) => !forcedLanguage || language === forcedLanguage)
            .flatMap(([, themeIds]) => themeIds),
        )]
        : undefined;
      const generatedPuzzle = game.generateOne(puzzleSize, targetDifficulty, {
        attempt,
        requestedCount,
        preferredLanguages,
        preferredThemeIds,
      });
      if (!generatedPuzzle) {
        console.log(`  [skip] ${game.title} generator hit backtrack limit (attempt ${attempt})`);
        continue;
      }

      const dedupeHash = hashDedupeKey(`${game.id}:${generatedPuzzle.dedupeKey}`);
      if (hasTried(db, dedupeHash)) {
        continue;
      }

      if (catalogDedupeKeys.has(generatedPuzzle.dedupeKey)) {
        recordTried(db, dedupeHash, puzzleSize, 'valid');
        continue;
      }

      recordTried(db, dedupeHash, puzzleSize, 'valid');
      nextCatalogId += 1;
      const id = `${game.entryIdPrefix}${nextCatalogId}`;
      const catalogEntry = { ...generatedPuzzle.entry, id };
      catalogEntries.push(catalogEntry);
      catalogDedupeKeys.add(generatedPuzzle.dedupeKey);

      const scoreLabel = generatedPuzzle.score === undefined ? '' : `, score ${generatedPuzzle.score}`;
      console.log(`  ✓ Generated puzzle ${id} (${generatedPuzzle.label}${scoreLabel})`);
      if (languageCounts && hasLanguageField(generatedPuzzle.entry)) {
        const language = generatedPuzzle.entry.language as (typeof WORD_SEARCH_LANGUAGES)[number];
        languageCounts.set(language, (languageCounts.get(language) ?? 0) + 1);
      }
      if (themeCapabilityMap && hasLanguageThemeDifficultyFields(generatedPuzzle.entry)) {
        const language = generatedPuzzle.entry.language as WordSearchLanguage;
        const difficulty = generatedPuzzle.entry.difficulty;
        const themeEntry = themeCapabilityMap.get(language)?.get(generatedPuzzle.entry.themeId);
        if (themeEntry) {
          themeEntry.counts.set(difficulty, (themeEntry.counts.get(difficulty) ?? 0) + 1);
        }
      }
      generated = true;
      totalGenerated += 1;
      break;
    }

    if (!generated) {
      console.warn(
        `  [warn] Could not generate another ${game.title} ${sizeLabel} puzzle after ${MAX_ATTEMPTS} attempts`,
      );
      break;
    }
  }

  if (replaceCatalog || totalGenerated > 0) {
    writeGameCatalog(game, catalogEntries);
  }
  if (game.id === 'wordsearch') {
    const gateStats = getWordSearchGenerationStats();
    console.log('\nWord Search rejection gate breakdown (this run):');
    Object.entries(gateStats).forEach(([gate, count]) => {
      console.log(`  ${gate}: ${count}`);
    });
  }
  console.log(`\nDone. Generated ${totalGenerated} new ${game.title} puzzle(s).`);
  db.close();
}

main();
