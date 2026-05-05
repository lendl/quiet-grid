import { useCallback } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { CompletionVariant } from '../../completion/types';
import type { PuzzleOutcome } from '../types';

export function usePuzzleOutcome() {
  return useCallback((
    outcome: PuzzleOutcome,
    variant: CompletionVariant,
  ): RootStackParamList['Completion'] => ({
    outcome,
    variant,
  }), []);
}
