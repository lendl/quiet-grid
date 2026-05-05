import { openDb, hashDedupeKey, hasTried, recordTried } from './db';
import { getEngineGameDefinition } from './gameRegistry';
import {
  appendGameCatalogEntry,
  catalogContainsDedupeKey,
  readGameCatalog,
  resetGameCatalog,
  writeGameCatalog,
} from './writer';

const MAX_ATTEMPTS = 100;

function parseGameArg(): string {
  const gameIndex = process.argv.findIndex((arg) => arg === '--game');
  const pairedArg = gameIndex >= 0 ? process.argv[gameIndex + 1] : null;
  const inlineArg = process.argv.find((arg) => arg.startsWith('--game='))?.split('=')[1];
  return inlineArg ?? pairedArg ?? 'binary';
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

function formatSizeLabel(sizes: readonly number[]): string {
  return sizes.map((size) => `${size}x${size}`).join('/');
}

function reclassifyExistingCatalog(gameId: string): void {
  const game = getEngineGameDefinition(gameId);
  const existingEntries = readGameCatalog(game);
  const rewrittenEntries = game.reclassifyEntries(existingEntries);
  const droppedCount = existingEntries.length - rewrittenEntries.length;
  writeGameCatalog(game, rewrittenEntries);

  console.log(`\nReclassified ${rewrittenEntries.length} existing ${game.title} puzzle(s).`);
  if (droppedCount > 0) {
    console.log(`Dropped ${droppedCount} existing puzzle(s) that no longer fit a supported difficulty bucket.`);
  }
}

function main(): void {
  const game = getEngineGameDefinition(parseGameArg());
  const requestedCount = parseRequestedCount();
  const replaceCatalog = process.argv.includes('--replace');
  const reclassifyExisting = process.argv.includes('--reclassify-existing');
  const forcedSize = parseSizeArg();
  const allowedSizes = game.listAllowedSizes();

  if (forcedSize !== null && !allowedSizes.includes(forcedSize)) {
    throw new Error(
      `Game ${game.id} does not support size ${forcedSize}. Allowed sizes: ${allowedSizes.join(', ')}`,
    );
  }

  const selectedSizes = forcedSize === null ? [...allowedSizes] : [forcedSize];
  const db = openDb();
  let totalGenerated = 0;

  if (reclassifyExisting) {
    reclassifyExistingCatalog(game.id);
    db.close();
    return;
  }

  if (replaceCatalog) {
    resetGameCatalog(game);
  }

  const sizeLabel = forcedSize === null ? formatSizeLabel(allowedSizes) : `${forcedSize}x${forcedSize}`;
  console.log(`\nGenerating ${requestedCount} scored ${game.title} ${sizeLabel} puzzle(s)...`);

  while (totalGenerated < requestedCount) {
    let generated = false;

    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
      const puzzleSize = selectedSizes[Math.floor(Math.random() * selectedSizes.length)];
      const targetDifficulty = game.pickTargetDifficulty(puzzleSize);
      const generatedPuzzle = game.generateOne(puzzleSize, targetDifficulty);
      if (!generatedPuzzle) {
        console.log(`  [skip] ${game.title} generator hit backtrack limit (attempt ${attempt})`);
        continue;
      }

      const dedupeHash = hashDedupeKey(`${game.id}:${generatedPuzzle.dedupeKey}`);
      if (hasTried(db, dedupeHash)) {
        continue;
      }

      if (catalogContainsDedupeKey(game, generatedPuzzle.dedupeKey)) {
        recordTried(db, dedupeHash, puzzleSize, 'valid');
        continue;
      }

      recordTried(db, dedupeHash, puzzleSize, 'valid');
      const id = appendGameCatalogEntry(game, generatedPuzzle.entry);

      const scoreLabel = generatedPuzzle.score === undefined ? '' : `, score ${generatedPuzzle.score}`;
      console.log(`  ✓ Generated puzzle ${id} (${generatedPuzzle.label}${scoreLabel})`);
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

  console.log(`\nDone. Generated ${totalGenerated} new ${game.title} puzzle(s).`);
  db.close();
}

main();
