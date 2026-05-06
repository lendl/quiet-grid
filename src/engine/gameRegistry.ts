import { takuzuEngineDefinition } from '../games/takuzu/engine/definition';
import type { EngineGameDefinition } from './gameDefinition';

export const engineGameRegistry = [
  takuzuEngineDefinition,
] as const satisfies readonly EngineGameDefinition[];

export function getEngineGameDefinition(id: string): EngineGameDefinition {
  const match = engineGameRegistry.find((definition) => definition.id === id);
  if (!match) {
    const knownIds = engineGameRegistry.map((definition) => definition.id).join(', ');
    throw new Error(`Unknown engine game: ${id}. Known games: ${knownIds}`);
  }

  return match;
}
