import type { CellValue, Grid, LineKey } from './types';

export type LineStatus = 'empty' | 'ok' | 'mismatch' | 'complete';
export type CompletedLineState = 'incomplete' | 'correct' | 'incorrect';

function getLineStatus(line: CellValue[], solLine: CellValue[]): LineStatus {
  const filled = line.filter(v => v !== null).length;
  if (filled === 0) return 'empty';
  for (let i = 0; i < line.length; i++) {
    if (line[i] !== null && line[i] !== solLine[i]) return 'mismatch';
  }
  return filled === line.length ? 'complete' : 'ok';
}

function getCompletedLineState(line: CellValue[], solLine: CellValue[]): CompletedLineState {
  if (line.some((value) => value === null)) return 'incomplete';
  return line.every((value, index) => value === solLine[index]) ? 'correct' : 'incorrect';
}

export function getCompletedLineStateForKey(
  board: Grid,
  solution: Grid,
  lineKey: LineKey,
): CompletedLineState {
  const lineIndex = Number(lineKey.slice(1));

  if (lineKey.startsWith('r')) {
    return getCompletedLineState(board[lineIndex], solution[lineIndex]);
  }

  return getCompletedLineState(
    board.map((row) => row[lineIndex]),
    solution.map((row) => row[lineIndex]),
  );
}

export function getRowColStatus(
  board: Grid,
  solution: Grid,
): { rows: LineStatus[]; cols: LineStatus[] } {
  const n = board.length;
  const rows = board.map((row, r) => getLineStatus(row, solution[r]));
  const cols = Array.from({ length: n }, (_, c) =>
    getLineStatus(board.map(r => r[c]), solution.map(r => r[c])),
  );
  return { rows, cols };
}

export function getTouchedLineStates(
  board: Grid,
  solution: Grid,
  row: number,
  col: number,
): { rowState: CompletedLineState; colState: CompletedLineState } {
  return {
    rowState: getCompletedLineState(board[row], solution[row]),
    colState: getCompletedLineState(
      board.map((currentRow) => currentRow[col]),
      solution.map((currentRow) => currentRow[col]),
    ),
  };
}

/**
 * Returns keys ('r0'…'rN', 'c0'…'cN') for every fully-filled row or column
 * whose values do not match the solution.
 */
export function getMismatchedCompletedLines(board: Grid, solution: Grid): LineKey[] {
  const n = board.length;
  const result: LineKey[] = [];
  for (let r = 0; r < n; r++) {
    if (getCompletedLineStateForKey(board, solution, `r${r}`) === 'incorrect') {
      result.push(`r${r}`);
    }
  }
  for (let c = 0; c < n; c++) {
    if (getCompletedLineStateForKey(board, solution, `c${c}`) === 'incorrect') {
      result.push(`c${c}`);
    }
  }
  return result;
}

/** Returns true when every cell on the board matches the solution. */
export function isBoardSolved(board: Grid, solution: Grid): boolean {
  return board.every((row, r) => row.every((v, c) => v === solution[r][c]));
}
