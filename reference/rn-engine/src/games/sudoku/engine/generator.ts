import type { PuzzleDifficulty } from '../../shared/types';
import {
  classifySudokuDifficulty,
  cloneSudokuBitmaskState,
  collectSudokuDifficultyMetrics,
  computeSudokuDifficultyScore,
  createSudokuBitmaskStateFromBoard,
  iterateMaskDigits,
  placeSudokuDigit,
  popcount,
  SUDOKU_DIFFICULTY_PROFILES,
  sudokuTechniqueDifficultyFloor,
  sudokuTechniques,
  traceSudokuHumanSolve,
  type SudokuTechnique,
} from '../gameplay/analysis';
import {
  countSudokuFullGrids,
  getSudokuUniquenessCache,
  insertSudokuFullGrid,
  openDb,
  pickSudokuFullGrid,
  pickSudokuRemovalPattern,
  recordSudokuDifficultyLog,
  recordSudokuRemovalPattern,
  recordSudokuUniquenessCache,
} from '../../../engine/db';
import type { SudokuCatalogEntry } from '../platform/codecs/codec';
import {
  cloneSudokuBoard,
  countFilledSudokuCells,
  sudokuDigits,
  type SudokuBoard,
  type SudokuDigit,
  type SudokuSolution,
} from '../types';

interface SudokuClassificationResult {
  difficulty: PuzzleDifficulty;
  score: number;
  scoreBucket: PuzzleDifficulty;
  techniqueBucket: PuzzleDifficulty;
}

interface SudokuGenerationCandidate {
  givens: SudokuBoard;
  classification: SudokuClassificationResult;
  scoreDistance: number;
}

const difficultyOrder: readonly PuzzleDifficulty[] = ['easy', 'medium', 'hard', 'expert'];

const targetGivenRanges: Record<PuzzleDifficulty, readonly [number, number]> = {
  easy: [36, 45],
  medium: [30, 36],
  hard: [26, 32],
  expert: [22, 28],
};

const FULL_GRID_STOCK_MIN = 100;
const REMOVAL_PATH_ATTEMPTS = 8;
const GENERATION_ATTEMPTS = 160;

function shuffle<T>(values: readonly T[]): T[] {
  const next = [...values];
  for (let index = next.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [next[index], next[swapIndex]] = [next[swapIndex], next[index]];
  }
  return next;
}

function encodeSudokuBoard(board: SudokuBoard): string {
  return board.map((row) => row.map((value) => value ?? 0).join('')).join('|');
}

function encodeSudokuPattern(board: SudokuBoard): string {
  return board.map((row) => row.map((value) => (value === null ? '0' : '1')).join('')).join('');
}

function encodeSudokuSolution(solution: SudokuSolution): string {
  return solution.flatMap((row) => row).join('');
}

function decodeSudokuSolution(encoded: string): SudokuSolution {
  return Array.from({ length: 9 }, (_, row) => (
    Array.from({ length: 9 }, (_, col) => Number(encoded[(row * 9) + col]) as SudokuDigit)
  ));
}

function getDifficultyRank(difficulty: PuzzleDifficulty): number {
  return difficultyOrder.indexOf(difficulty);
}

function getAllowedTechniquesForDifficulty(difficulty: PuzzleDifficulty): readonly SudokuTechnique[] {
  const targetRank = getDifficultyRank(difficulty);
  return sudokuTechniques.filter((technique) => (
    getDifficultyRank(sudokuTechniqueDifficultyFloor[technique]) <= targetRank
  ));
}

function createSolvedBoard(): SudokuSolution {
  const digits = shuffle(sudokuDigits);
  const rowBands = shuffle([0, 1, 2]);
  const colStacks = shuffle([0, 1, 2]);
  const rows = rowBands.flatMap((band) => shuffle([0, 1, 2]).map((offset) => (band * 3) + offset));
  const cols = colStacks.flatMap((stack) => shuffle([0, 1, 2]).map((offset) => (stack * 3) + offset));

  return rows.map((row) => cols.map((col) => digits[((row * 3) + Math.floor(row / 3) + col) % 9]));
}

function createPuzzleFromSolution(solution: SudokuSolution): SudokuBoard {
  return solution.map((row) => row.map((digit) => digit as SudokuDigit | null));
}

function countSolutionsFromState(
  state: ReturnType<typeof createSudokuBitmaskStateFromBoard>,
  limit: number,
): number {
  let bestIndex = -1;
  let bestMask = 0;

  for (let index = 0; index < state.board.length; index += 1) {
    if (state.board[index] !== 0) {
      continue;
    }

    const mask = state.candidateMask[index];
    if (mask === 0) {
      return 0;
    }
    if (bestIndex === -1 || popcount[mask] < popcount[bestMask]) {
      bestIndex = index;
      bestMask = mask;
      if (popcount[mask] === 1) {
        break;
      }
    }
  }

  if (bestIndex === -1) {
    return 1;
  }

  let solutions = 0;
  for (const digit of iterateMaskDigits(bestMask)) {
    const branch = cloneSudokuBitmaskState(state);
    placeSudokuDigit(branch, bestIndex, digit);
    solutions += countSolutionsFromState(branch, limit - solutions);
    if (solutions >= limit) {
      return solutions;
    }
  }

  return solutions;
}

function countSolutions(board: SudokuBoard, limit: number): number {
  return countSolutionsFromState(createSudokuBitmaskStateFromBoard(board), limit);
}

function classifyTrace(traceMoves: ReturnType<typeof traceSudokuHumanSolve>['moves']): SudokuClassificationResult | null {
  const metrics = collectSudokuDifficultyMetrics(traceMoves);
  const score = computeSudokuDifficultyScore(metrics);
  const classification = classifySudokuDifficulty(metrics, score);
  if (!classification) {
    return null;
  }

  return {
    difficulty: classification.difficulty,
    score,
    scoreBucket: classification.scoreBucket,
    techniqueBucket: classification.techniqueBucket,
  };
}

function getTargetScoreCenter(targetDifficulty: PuzzleDifficulty): number {
  const { min, max } = SUDOKU_DIFFICULTY_PROFILES[targetDifficulty].scoreBand;
  if (Number.isFinite(max)) {
    return (min + max) / 2;
  }
  return min + 160;
}

function isBetterSudokuGenerationCandidate(
  candidate: SudokuGenerationCandidate,
  best: SudokuGenerationCandidate | null,
): boolean {
  if (!best) {
    return true;
  }

  if (candidate.scoreDistance !== best.scoreDistance) {
    return candidate.scoreDistance < best.scoreDistance;
  }

  const candidateGivens = countFilledSudokuCells(candidate.givens);
  const bestGivens = countFilledSudokuCells(best.givens);
  if (candidateGivens !== bestGivens) {
    return candidateGivens < bestGivens;
  }

  return candidate.classification.score > best.classification.score;
}

function ensureStoredFullGridStock(db: ReturnType<typeof openDb>): void {
  let count = countSudokuFullGrids(db);
  while (count < FULL_GRID_STOCK_MIN) {
    insertSudokuFullGrid(db, encodeSudokuSolution(createSolvedBoard()));
    count = countSudokuFullGrids(db);
  }
}

function pickStoredSolution(db: ReturnType<typeof openDb>): SudokuSolution {
  const encoded = pickSudokuFullGrid(db);
  if (!encoded) {
    const solution = createSolvedBoard();
    insertSudokuFullGrid(db, encodeSudokuSolution(solution));
    return solution;
  }
  return decodeSudokuSolution(encoded);
}

function countSolutionsCached(
  db: ReturnType<typeof openDb>,
  board: SudokuBoard,
): number {
  const puzzleHash = encodeSudokuBoard(board);
  const structureHash = encodeSudokuPattern(board);
  const cached = getSudokuUniquenessCache(db, puzzleHash, structureHash);
  if (cached !== null) {
    return cached;
  }

  const solutionCount = countSolutions(cloneSudokuBoard(board), 2);
  recordSudokuUniquenessCache(db, puzzleHash, structureHash, solutionCount);
  return solutionCount;
}

function buildRemovalOrder(
  db: ReturnType<typeof openDb>,
  targetDifficulty: PuzzleDifficulty,
): Array<{ row: number; col: number }> {
  const preferredPattern = pickSudokuRemovalPattern(db, targetDifficulty);
  const preferredCells = preferredPattern
    ? Array.from(preferredPattern)
      .map((value, index) => ({ value, index }))
      .filter(({ value }) => value === '0')
      .map(({ index }) => ({ row: Math.floor(index / 9), col: index % 9 }))
    : [];
  const seen = new Set(preferredCells.map(({ row, col }) => `${row}:${col}`));
  const remaining = Array.from({ length: 81 }, (_, index) => ({
    row: Math.floor(index / 9),
    col: index % 9,
  })).filter(({ row, col }) => !seen.has(`${row}:${col}`));

  return [...shuffle(preferredCells), ...shuffle(remaining)];
}

function classifySudokuProgressForTarget(
  givens: SudokuBoard,
  targetDifficulty: PuzzleDifficulty,
): SudokuGenerationCandidate | null {
  const trace = traceSudokuHumanSolve(givens, getAllowedTechniquesForDifficulty(targetDifficulty));
  if (!trace.solved) {
    return null;
  }

  const classification = classifyTrace(trace.moves);
  if (!classification) {
    return null;
  }

  return {
    givens: cloneSudokuBoard(givens),
    classification,
    scoreDistance: Math.abs(classification.score - getTargetScoreCenter(targetDifficulty)),
  };
}

export function classifySudokuEntry(
  entry: Pick<SudokuCatalogEntry, 'givens' | 'solution'>,
  options?: { skipUniquenessCheck?: boolean },
): SudokuClassificationResult | null {
  if (!options?.skipUniquenessCheck && countSolutions(cloneSudokuBoard(entry.givens), 2) !== 1) {
    return null;
  }

  const trace = traceSudokuHumanSolve(entry.givens, sudokuTechniques);
  if (!trace.solved) {
    return null;
  }

  const classification = classifyTrace(trace.moves);
  if (!classification) {
    return null;
  }

  const metrics = collectSudokuDifficultyMetrics(trace.moves);
  const db = openDb();
  try {
    recordSudokuDifficultyLog(db, {
      puzzleHash: encodeSudokuBoard(entry.givens),
      difficulty: classification.difficulty,
      highestTechnique: metrics.hardestTechnique,
      stepCount: metrics.stepCount,
      branchingFactor: metrics.branchingFactor,
      branchingScore: metrics.branchingScore,
      score: classification.score,
      techniqueLogJson: JSON.stringify(trace.moves.map((move) => ({
        technique: move.technique,
        kind: move.kind,
        targets: move.kind === 'placement' ? 1 : move.eliminations.length,
      }))),
    });
  } finally {
    db.close();
  }

  return classification;
}

function searchSudokuRemovalPath(
  db: ReturnType<typeof openDb>,
  solution: SudokuSolution,
  targetDifficulty: PuzzleDifficulty,
): SudokuGenerationCandidate | null {
  const targetRank = getDifficultyRank(targetDifficulty);
  const [minGivens, maxGivens] = targetGivenRanges[targetDifficulty];
  const givens = createPuzzleFromSolution(solution);
  const removalOrder = buildRemovalOrder(db, targetDifficulty);
  let bestCandidate: SudokuGenerationCandidate | null = null;

  for (const { row, col } of removalOrder) {
    const previous = givens[row][col];
    if (previous === null) {
      continue;
    }

    givens[row][col] = null;
    const filledCells = countFilledSudokuCells(givens);

    if (filledCells < minGivens) {
      givens[row][col] = previous;
      break;
    }

    if (countSolutionsCached(db, givens) !== 1) {
      givens[row][col] = previous;
      continue;
    }

    const progress = classifySudokuProgressForTarget(givens, targetDifficulty);
    if (!progress || getDifficultyRank(progress.classification.difficulty) > targetRank) {
      givens[row][col] = previous;
      continue;
    }

    if (
      progress.classification.difficulty === targetDifficulty
      && filledCells >= minGivens
      && filledCells <= maxGivens
      && isBetterSudokuGenerationCandidate(progress, bestCandidate)
    ) {
      bestCandidate = progress;
    }
  }

  recordSudokuRemovalPattern(
    db,
    encodeSudokuPattern(bestCandidate?.givens ?? givens),
    countFilledSudokuCells(bestCandidate?.givens ?? givens),
    targetDifficulty,
    bestCandidate !== null,
  );

  return bestCandidate;
}

function searchSudokuRemovalBatch(
  db: ReturnType<typeof openDb>,
  solution: SudokuSolution,
  targetDifficulty: PuzzleDifficulty,
): SudokuGenerationCandidate | null {
  let bestCandidate: SudokuGenerationCandidate | null = null;

  for (let attempt = 0; attempt < REMOVAL_PATH_ATTEMPTS; attempt += 1) {
    const candidate = searchSudokuRemovalPath(db, solution, targetDifficulty);
    if (candidate && isBetterSudokuGenerationCandidate(candidate, bestCandidate)) {
      bestCandidate = candidate;
    }
  }

  return bestCandidate;
}

export function generateSudokuPuzzle(
  targetDifficulty: PuzzleDifficulty,
): {
  dedupeKey: string;
  entry: Omit<SudokuCatalogEntry, 'id'>;
  label: string;
  score: number;
} | null {
  const db = openDb();
  try {
    ensureStoredFullGridStock(db);

    for (let attempt = 0; attempt < GENERATION_ATTEMPTS; attempt += 1) {
      const solution = pickStoredSolution(db);
      const candidate = searchSudokuRemovalBatch(db, solution, targetDifficulty);
      if (!candidate) {
        continue;
      }

      const trace = traceSudokuHumanSolve(candidate.givens, getAllowedTechniquesForDifficulty(targetDifficulty));
      const metrics = collectSudokuDifficultyMetrics(trace.moves);
      recordSudokuDifficultyLog(db, {
        puzzleHash: encodeSudokuBoard(candidate.givens),
        difficulty: candidate.classification.difficulty,
        highestTechnique: metrics.hardestTechnique,
        stepCount: metrics.stepCount,
        branchingFactor: metrics.branchingFactor,
        branchingScore: metrics.branchingScore,
        score: candidate.classification.score,
        techniqueLogJson: JSON.stringify(trace.moves.map((move) => ({
          technique: move.technique,
          kind: move.kind,
          targets: move.kind === 'placement' ? 1 : move.eliminations.length,
        }))),
      });

      return {
        dedupeKey: encodeSudokuBoard(candidate.givens),
        entry: {
          difficulty: candidate.classification.difficulty,
          rows: 9,
          cols: 9,
          givens: candidate.givens,
          solution,
        },
        label: `9x9 ${candidate.classification.difficulty}`,
        score: candidate.classification.score,
      };
    }
  } finally {
    db.close();
  }

  return null;
}

export function getSudokuEntryDedupeKey(entry: Pick<SudokuCatalogEntry, 'givens'>): string {
  return encodeSudokuBoard(entry.givens);
}
