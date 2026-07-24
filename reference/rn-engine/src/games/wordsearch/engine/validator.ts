import {
  WORD_SEARCH_MIN_THEMES_PER_LANGUAGE,
  WORD_SEARCH_MIN_WORDS_PER_THEME,
  WORD_SEARCH_QUALITY_THRESHOLDS,
} from './constraints';
import {
  listWordSearchLanguages,
  wordSearchSeedCorpus,
  type WordSearchSeedCorpus,
} from './seedCorpus';
import {
  materializeWordSearchCatalogEntry,
  type WordSearchCatalogEntry,
} from '../platform/codecs/codec';

export interface WordSearchSeedValidationError {
  language: string;
  code: 'theme-count' | 'word-count';
  message: string;
}

export function validateWordSearchSeedCorpus(
  corpus: WordSearchSeedCorpus = wordSearchSeedCorpus,
): WordSearchSeedValidationError[] {
  const errors: WordSearchSeedValidationError[] = [];

  listWordSearchLanguages().forEach((language) => {
    const themes = corpus[language] ?? [];
    if (themes.length < WORD_SEARCH_MIN_THEMES_PER_LANGUAGE) {
      errors.push({
        language,
        code: 'theme-count',
        message: `Language ${language} has ${themes.length} themes; minimum is ${WORD_SEARCH_MIN_THEMES_PER_LANGUAGE}.`,
      });
    }

    themes.forEach((theme) => {
      if (theme.words.length < WORD_SEARCH_MIN_WORDS_PER_THEME) {
        errors.push({
          language,
          code: 'word-count',
          message: `Language ${language}, theme ${theme.themeId} has ${theme.words.length} words; minimum is ${WORD_SEARCH_MIN_WORDS_PER_THEME}.`,
        });
      }
    });
  });

  return errors;
}

export interface WordSearchCatalogValidationError {
  id: string;
  code: 'schema-version' | 'duplicate-signature' | 'quality-threshold' | 'invalid-recipe' | 'incomplete-coverage';
  message: string;
}

function qualityPassesThreshold(entry: WordSearchCatalogEntry): boolean {
  const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[entry.difficulty];
  return entry.quality.score >= threshold.minScore
    && entry.quality.overlapRatio >= threshold.minOverlapRatio
    && entry.quality.directionEntropy >= threshold.minDirectionEntropy;
}

function buildCatalogSignature(entry: WordSearchCatalogEntry): string {
  return `${entry.language}:${entry.themeId}:${entry.difficulty}:${entry.rows}:${entry.diversitySignature}`;
}

export function validateWordSearchCatalog(entries: readonly WordSearchCatalogEntry[]): WordSearchCatalogValidationError[] {
  const errors: WordSearchCatalogValidationError[] = [];
  const seenSignatures = new Set<string>();

  entries.forEach((entry) => {
    if (entry.schemaVersion !== 1) {
      errors.push({
        id: entry.id,
        code: 'schema-version',
        message: `Entry ${entry.id} has schemaVersion ${String(entry.schemaVersion)}; expected 1.`,
      });
    }

    const signature = buildCatalogSignature(entry);
    if (seenSignatures.has(signature)) {
      errors.push({
        id: entry.id,
        code: 'duplicate-signature',
        message: `Entry ${entry.id} duplicates diversity signature ${signature}.`,
      });
    } else {
      seenSignatures.add(signature);
    }

    if (!qualityPassesThreshold(entry)) {
      const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[entry.difficulty];
      errors.push({
        id: entry.id,
        code: 'quality-threshold',
        message: `Entry ${entry.id} quality below threshold for ${entry.difficulty}. score=${entry.quality.score.toFixed(3)} min=${threshold.minScore.toFixed(3)}.`,
      });
    }

    try {
      const materialized = materializeWordSearchCatalogEntry(entry);
      if (materialized.grid.length !== entry.rows || (materialized.grid[0]?.length ?? 0) !== entry.cols) {
        errors.push({
          id: entry.id,
          code: 'invalid-recipe',
          message: `Entry ${entry.id} materialized dimensions do not match recipe dimensions.`,
        });
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      const code = message.includes('not covered') ? 'incomplete-coverage' : 'invalid-recipe';
      errors.push({ id: entry.id, code, message: `Entry ${entry.id} failed to materialize: ${message}` });
    }
  });

  return errors;
}
