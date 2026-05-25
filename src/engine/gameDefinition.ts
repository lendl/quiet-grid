import type { CatalogFileOptions } from './catalog';
import type { GameId, PuzzleDifficulty } from '../games/shared/types';

export interface EngineCatalogEntry {
  id: string;
  difficulty: PuzzleDifficulty;
}

export interface EngineGenerateResult<TEntry extends EngineCatalogEntry = EngineCatalogEntry> {
  dedupeKey: string;
  entry: Omit<TEntry, 'id'>;
  label: string;
  score?: number;
}

export interface EngineGameDefinition<TEntry extends EngineCatalogEntry = EngineCatalogEntry> {
  id: GameId;
  title: string;
  catalogPath: string;
  entryIdPrefix: string;
  catalog: CatalogFileOptions<TEntry>;
  listAllowedSizes(): readonly number[];
  listAllowedDifficulties(size: number): readonly PuzzleDifficulty[];
  pickTargetDifficulty(size: number): PuzzleDifficulty;
  generateOne(size: number, targetDifficulty: PuzzleDifficulty): EngineGenerateResult<TEntry> | null;
  getEntryDedupeKey(entry: TEntry | Omit<TEntry, 'id'>): string;
  reclassifyEntries(entries: TEntry[]): TEntry[];
  describeSizeOptions?(size: number): readonly string[];
}
