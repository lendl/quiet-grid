import type { ReactNode } from 'react';
import type { GameId } from '../../games/shared/types';

export interface PuzzleAnalysisSource {
  gameId: GameId;
  payload: unknown;
}

export interface PuzzleAnalysisStep {
  key: string;
  title: string;
  body: string;
  ruleKey?: string;
  evidenceCells: Array<{ row: number; col: number }>;
  targetCells: Array<{ row: number; col: number; value?: 0 | 1 }>;
  highlightRows: number[];
  highlightCols: number[];
  beforeState: unknown;
  afterState: unknown;
}

export interface PuzzleAnalysisPayload {
  gameId: GameId;
  steps: PuzzleAnalysisStep[];
  payload: unknown;
}

export interface PuzzleAnalysisRendererArgs {
  analysis: PuzzleAnalysisPayload;
  stepIndex: number;
  containerWidth: number;
  containerHeight: number;
}

export interface PuzzleAnalysisAdapter {
  buildAnalysisSource: (session: unknown) => PuzzleAnalysisSource | null;
  supportsAnalysis: (source: PuzzleAnalysisSource | undefined) => boolean;
  buildAnalysis: (source: PuzzleAnalysisSource) => PuzzleAnalysisPayload;
  renderAnalysisStep: (args: PuzzleAnalysisRendererArgs) => ReactNode;
}
