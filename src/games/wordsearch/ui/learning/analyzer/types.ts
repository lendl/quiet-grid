import type {
  PuzzleAnalysisPayload,
  PuzzleAnalysisSource,
  PuzzleAnalysisStep,
} from '../../../../../app/analysis/types';
import type { WordSearchPuzzle } from '../../../types';

export interface WordSearchAnalysisSource extends PuzzleAnalysisSource {
  gameId: 'wordsearch';
  payload: {
    puzzle: WordSearchPuzzle;
    foundWordIds: string[];
  };
}

export interface WordSearchAnalysisStep extends PuzzleAnalysisStep {
  beforeState: {
    foundWordIds: string[];
  };
  afterState: {
    foundWordIds: string[];
  };
}

export interface WordSearchAnalysisPayload extends PuzzleAnalysisPayload {
  gameId: 'wordsearch';
  steps: WordSearchAnalysisStep[];
  payload: {
    puzzle: WordSearchPuzzle;
  };
}
