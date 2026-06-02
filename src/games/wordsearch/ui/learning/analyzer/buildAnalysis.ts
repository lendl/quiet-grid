import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import {
  buildWordSearchAnalysis as buildWordSearchAnalysisFromGameplay,
  buildWordSearchAnalysisSource as buildWordSearchAnalysisSourceFromGameplay,
  supportsWordSearchAnalysis as supportsWordSearchAnalysisFromGameplay,
} from '../../../gameplay/analysis';

export function buildWordSearchAnalysisSource(session: unknown): PuzzleAnalysisSource | null {
  return buildWordSearchAnalysisSourceFromGameplay(session);
}

export function supportsWordSearchAnalysis(source: PuzzleAnalysisSource | undefined): boolean {
  return supportsWordSearchAnalysisFromGameplay(source);
}

export function buildWordSearchAnalysis(source: PuzzleAnalysisSource): PuzzleAnalysisPayload {
  return buildWordSearchAnalysisFromGameplay(source);
}
