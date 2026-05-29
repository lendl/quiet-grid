import Database from 'better-sqlite3';
import { createHash } from 'crypto';
import path from 'path';
import type { PuzzleDifficulty } from '../games/shared/types';

const DB_PATH = path.join(__dirname, 'puzzles.db');

/** Open (or create) the engine's SQLite tracking database and ensure tables exist. */
export function openDb(): Database.Database {
  const db = new Database(DB_PATH);
  db.exec(`
    CREATE TABLE IF NOT EXISTS tried_solutions (
      hash     TEXT PRIMARY KEY,
      size     INTEGER NOT NULL,
      status   TEXT    NOT NULL,
      tried_at TEXT    NOT NULL
    );
    CREATE TABLE IF NOT EXISTS sudoku_full_grids (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      grid       TEXT NOT NULL,
      grid_hash  TEXT NOT NULL UNIQUE,
      created_at TEXT NOT NULL,
      used_at    TEXT,
      use_count  INTEGER NOT NULL DEFAULT 0
    );
    CREATE TABLE IF NOT EXISTS sudoku_removal_patterns (
      id                INTEGER PRIMARY KEY AUTOINCREMENT,
      pattern_mask      TEXT NOT NULL,
      clue_count        INTEGER NOT NULL,
      target_difficulty TEXT NOT NULL,
      success_count     INTEGER NOT NULL DEFAULT 0,
      fail_count        INTEGER NOT NULL DEFAULT 0,
      last_used_at      TEXT,
      UNIQUE (pattern_mask, target_difficulty)
    );
    CREATE TABLE IF NOT EXISTS sudoku_difficulty_logs (
      id                 INTEGER PRIMARY KEY AUTOINCREMENT,
      puzzle_hash        TEXT NOT NULL,
      difficulty         TEXT NOT NULL,
      highest_technique  TEXT,
      step_count         INTEGER NOT NULL,
      branching_factor   REAL NOT NULL,
      branching_score    INTEGER NOT NULL,
      score              INTEGER NOT NULL,
      technique_log_json TEXT NOT NULL,
      created_at         TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS sudoku_uniqueness_cache (
      puzzle_hash     TEXT NOT NULL,
      structure_hash  TEXT NOT NULL,
      solution_count  INTEGER NOT NULL,
      checked_at      TEXT NOT NULL,
      PRIMARY KEY (puzzle_hash, structure_hash)
    );
  `);
  return db;
}

/** SHA-256 hash of a game-specific deduplication key. */
export function hashDedupeKey(dedupeKey: string): string {
  return createHash('sha256').update(dedupeKey).digest('hex');
}

export const hashSolution = hashDedupeKey;

/** Returns true if this solution hash has been attempted before. */
export function hasTried(db: Database.Database, hash: string): boolean {
  return (
    db
      .prepare('SELECT 1 FROM tried_solutions WHERE hash = ?')
      .get(hash) !== undefined
  );
}

/** Record an attempted deduplication key. Uses INSERT OR IGNORE so repeated calls are safe. */
export function recordTried(
  db: Database.Database,
  hash: string,
  size: number,
  status: 'valid' | 'invalid',
): void {
  db
    .prepare(
      'INSERT OR IGNORE INTO tried_solutions (hash, size, status, tried_at) VALUES (?, ?, ?, ?)',
    )
    .run(hash, size, status, new Date().toISOString());
}

export function countSudokuFullGrids(db: Database.Database): number {
  const row = db.prepare('SELECT COUNT(*) as count FROM sudoku_full_grids').get() as { count: number } | undefined;
  return Number(row?.count ?? 0);
}

export function insertSudokuFullGrid(db: Database.Database, grid: string): void {
  db.prepare(`
    INSERT OR IGNORE INTO sudoku_full_grids (grid, grid_hash, created_at)
    VALUES (?, ?, ?)
  `).run(grid, hashDedupeKey(grid), new Date().toISOString());
}

export function pickSudokuFullGrid(db: Database.Database): string | null {
  const row = db.prepare(`
    SELECT grid, grid_hash
    FROM sudoku_full_grids
    ORDER BY COALESCE(used_at, created_at) ASC, use_count ASC, RANDOM()
    LIMIT 1
  `).get() as { grid: string; grid_hash: string } | undefined;
  if (!row) {
    return null;
  }

  db.prepare(`
    UPDATE sudoku_full_grids
    SET used_at = ?, use_count = use_count + 1
    WHERE grid_hash = ?
  `).run(new Date().toISOString(), row.grid_hash);
  return row.grid;
}

export function pickSudokuRemovalPattern(
  db: Database.Database,
  targetDifficulty: PuzzleDifficulty,
): string | null {
  const row = db.prepare(`
    SELECT pattern_mask
    FROM sudoku_removal_patterns
    WHERE target_difficulty = ?
    ORDER BY success_count DESC, fail_count ASC, RANDOM()
    LIMIT 1
  `).get(targetDifficulty) as { pattern_mask: string } | undefined;
  return row?.pattern_mask ?? null;
}

export function recordSudokuRemovalPattern(
  db: Database.Database,
  patternMask: string,
  clueCount: number,
  targetDifficulty: PuzzleDifficulty,
  accepted: boolean,
): void {
  db.prepare(`
    INSERT INTO sudoku_removal_patterns (
      pattern_mask,
      clue_count,
      target_difficulty,
      success_count,
      fail_count,
      last_used_at
    )
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(pattern_mask, target_difficulty) DO UPDATE SET
      clue_count = excluded.clue_count,
      success_count = sudoku_removal_patterns.success_count + excluded.success_count,
      fail_count = sudoku_removal_patterns.fail_count + excluded.fail_count,
      last_used_at = excluded.last_used_at
  `).run(
    patternMask,
    clueCount,
    targetDifficulty,
    accepted ? 1 : 0,
    accepted ? 0 : 1,
    new Date().toISOString(),
  );
}

export function getSudokuUniquenessCache(
  db: Database.Database,
  puzzleHash: string,
  structureHash: string,
): number | null {
  const row = db.prepare(`
    SELECT solution_count
    FROM sudoku_uniqueness_cache
    WHERE puzzle_hash = ? AND structure_hash = ?
  `).get(puzzleHash, structureHash) as { solution_count: number } | undefined;
  return row?.solution_count ?? null;
}

export function recordSudokuUniquenessCache(
  db: Database.Database,
  puzzleHash: string,
  structureHash: string,
  solutionCount: number,
): void {
  db.prepare(`
    INSERT INTO sudoku_uniqueness_cache (puzzle_hash, structure_hash, solution_count, checked_at)
    VALUES (?, ?, ?, ?)
    ON CONFLICT(puzzle_hash, structure_hash) DO UPDATE SET
      solution_count = excluded.solution_count,
      checked_at = excluded.checked_at
  `).run(puzzleHash, structureHash, solutionCount, new Date().toISOString());
}

export function recordSudokuDifficultyLog(
  db: Database.Database,
  input: {
    puzzleHash: string;
    difficulty: PuzzleDifficulty;
    highestTechnique: string | null;
    stepCount: number;
    branchingFactor: number;
    branchingScore: number;
    score: number;
    techniqueLogJson: string;
  },
): void {
  db.prepare(`
    INSERT INTO sudoku_difficulty_logs (
      puzzle_hash,
      difficulty,
      highest_technique,
      step_count,
      branching_factor,
      branching_score,
      score,
      technique_log_json,
      created_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    input.puzzleHash,
    input.difficulty,
    input.highestTechnique,
    input.stepCount,
    input.branchingFactor,
    input.branchingScore,
    input.score,
    input.techniqueLogJson,
    new Date().toISOString(),
  );
}
