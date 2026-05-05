import Database from 'better-sqlite3';
import { createHash } from 'crypto';
import path from 'path';

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

