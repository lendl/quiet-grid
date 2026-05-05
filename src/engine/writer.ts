import {
  getNextId,
  readChunkedCatalog,
  writeChunkedCatalog,
} from './catalog';
import type {
  EngineCatalogEntry,
  EngineGameDefinition,
} from './gameDefinition';

export function readGameCatalog<TEntry extends EngineCatalogEntry>(
  game: EngineGameDefinition<TEntry>,
): TEntry[] {
  return readChunkedCatalog(game.catalogPath, game.catalog);
}

export function writeGameCatalog<TEntry extends EngineCatalogEntry>(
  game: EngineGameDefinition<TEntry>,
  entries: TEntry[],
): void {
  writeChunkedCatalog(game.catalogPath, entries, game.catalog);
}

export function catalogContainsDedupeKey<TEntry extends EngineCatalogEntry>(
  game: EngineGameDefinition<TEntry>,
  dedupeKey: string,
): boolean {
  return readGameCatalog(game).some((entry) => game.getEntryDedupeKey(entry) === dedupeKey);
}

export function appendGameCatalogEntry<TEntry extends EngineCatalogEntry>(
  game: EngineGameDefinition<TEntry>,
  entry: Omit<TEntry, 'id'>,
): string {
  const id = getNextId(game.catalogPath, game.entryIdPrefix);
  const entries = readGameCatalog(game);
  const nextEntry = { ...entry, id } as TEntry;
  writeGameCatalog(game, [...entries, nextEntry]);
  return id;
}

export function resetGameCatalog<TEntry extends EngineCatalogEntry>(
  game: EngineGameDefinition<TEntry>,
): void {
  writeGameCatalog(game, []);
}
