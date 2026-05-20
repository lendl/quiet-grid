import { useCallback } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { SolvedResultVariant } from '../../completion/types';
import type { SessionResult } from '../types';

export function usePuzzleOutcome() {
  return useCallback((
    result: SessionResult,
    variant: SolvedResultVariant,
  ): RootStackParamList['Completion'] => ({
    result,
    variant,
  }), []);
}
