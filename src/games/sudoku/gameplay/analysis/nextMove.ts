import { getSudokuStrings } from '../../content/strings';
import type { SudokuBoard, SudokuDigit, SudokuSession } from '../../types';
import { getSudokuBoxIndex } from '../rules/validation';
import { buildSudokuCandidateGrid } from './candidates';
import { traceSudokuHumanSolve } from './dispatcher';
import type { SudokuCanonicalMove, SudokuCellRef, SudokuHouseRef } from './moves';
import type { SudokuTechnique } from './techniques';

export interface SudokuHintTargetCell extends SudokuCellRef {
  digit: SudokuDigit;
  action: 'place' | 'eliminate';
}

export interface SudokuNextMoveHint {
  kind: 'invalid-board' | 'progress';
  ruleKey: 'invalid-board' | SudokuTechnique;
  title: string;
  body: string;
  evidenceCells: SudokuCellRef[];
  targetCells: SudokuHintTargetCell[];
  highlightRows: number[];
  highlightCols: number[];
  highlightBoxes: number[];
}

interface SudokuInvalidConflictState {
  title: string;
  body: string;
  evidenceCells: SudokuCellRef[];
  highlightRows: number[];
  highlightCols: number[];
  highlightBoxes: number[];
}

function buildCellKey({ row, col }: SudokuCellRef): string {
  return `${row}:${col}`;
}

function compareCellRefs(a: SudokuCellRef, b: SudokuCellRef): number {
  if (a.row !== b.row) {
    return a.row - b.row;
  }

  return a.col - b.col;
}

function dedupeCells(cells: readonly SudokuCellRef[]): SudokuCellRef[] {
  const unique = new Map<string, SudokuCellRef>();

  cells.forEach((cell) => {
    unique.set(buildCellKey(cell), cell);
  });

  return Array.from(unique.values()).sort(compareCellRefs);
}

function getHouseCells(house: SudokuHouseRef): SudokuCellRef[] {
  if (house.kind === 'row') {
    return Array.from({ length: 9 }, (_, col) => ({ row: house.index, col }));
  }

  if (house.kind === 'column') {
    return Array.from({ length: 9 }, (_, row) => ({ row, col: house.index }));
  }

  const rowStart = Math.floor(house.index / 3) * 3;
  const colStart = (house.index % 3) * 3;
  return Array.from({ length: 9 }, (_, offset) => ({
    row: rowStart + Math.floor(offset / 3),
    col: colStart + (offset % 3),
  }));
}

function formatHouseLabel(house: SudokuHouseRef): string {
  const strings = getSudokuStrings();

  switch (house.kind) {
    case 'row':
      return strings.learning.labels.row(house.index + 1);
    case 'column':
      return strings.learning.labels.column(house.index + 1);
    default:
      return strings.learning.labels.box(house.index + 1);
  }
}

function formatCellLabel(cell: SudokuCellRef): string {
  return getSudokuStrings().learning.labels.cell(cell.row + 1, cell.col + 1);
}

function joinLabels(labels: readonly string[]): string {
  return getSudokuStrings().learning.labels.joinList([...labels]);
}

function collectHouseHighlights(houses: readonly SudokuHouseRef[]) {
  const rows = new Set<number>();
  const cols = new Set<number>();
  const boxes = new Set<number>();

  houses.forEach((house) => {
    if (house.kind === 'row') {
      rows.add(house.index);
      return;
    }

    if (house.kind === 'column') {
      cols.add(house.index);
      return;
    }

    boxes.add(house.index);
  });

  return {
    highlightRows: Array.from(rows).sort((a, b) => a - b),
    highlightCols: Array.from(cols).sort((a, b) => a - b),
    highlightBoxes: Array.from(boxes).sort((a, b) => a - b),
  };
}

function findDuplicateConflict(
  board: SudokuBoard,
): SudokuInvalidConflictState | null {
  const strings = getSudokuStrings();
  const houses: SudokuHouseRef[] = [
    ...Array.from({ length: 9 }, (_, index) => ({ kind: 'row' as const, index })),
    ...Array.from({ length: 9 }, (_, index) => ({ kind: 'column' as const, index })),
    ...Array.from({ length: 9 }, (_, index) => ({ kind: 'box' as const, index })),
  ];

  for (const house of houses) {
    const digitMap = new Map<SudokuDigit, SudokuCellRef[]>();

    getHouseCells(house).forEach((cell) => {
      const digit = board[cell.row]?.[cell.col];
      if (digit === null || typeof digit === 'undefined') {
        return;
      }

      digitMap.set(digit, [...(digitMap.get(digit) ?? []), cell]);
    });

    for (const [digit, cells] of digitMap.entries()) {
      if (cells.length < 2) {
        continue;
      }

      const highlights = collectHouseHighlights([house]);
      return {
        title: strings.play.nextMove.invalidConflictTitle,
        body: strings.play.nextMove.invalidConflictBody(formatHouseLabel(house), digit),
        evidenceCells: dedupeCells(cells),
        ...highlights,
      };
    }
  }

  return null;
}

function findDeadCellConflict(
  session: SudokuSession,
): SudokuInvalidConflictState | null {
  const strings = getSudokuStrings();
  const candidateGrid = buildSudokuCandidateGrid({
    board: session.board,
    givens: session.puzzle.givens,
    notes: session.notes,
  });

  for (const row of candidateGrid) {
    for (const cell of row) {
      if (cell.value !== null || cell.logicalCandidates.length > 0) {
        continue;
      }

      const boxIndex = getSudokuBoxIndex(cell.row, cell.col);
      const evidenceCells = dedupeCells([
        ...Array.from({ length: 9 }, (_, col) => ({ row: cell.row, col })),
        ...Array.from({ length: 9 }, (_, rowIndex) => ({ row: rowIndex, col: cell.col })),
        ...getHouseCells({ kind: 'box', index: boxIndex }),
      ].filter((entry) => session.board[entry.row]?.[entry.col] !== null));

      return {
        title: strings.play.nextMove.invalidDeadCellTitle,
        body: strings.play.nextMove.invalidDeadCellBody(formatCellLabel(cell)),
        evidenceCells,
        highlightRows: [cell.row],
        highlightCols: [cell.col],
        highlightBoxes: [boxIndex],
      };
    }
  }

  return null;
}

export function findSudokuInvalidState(
  session: SudokuSession,
): SudokuInvalidConflictState | null {
  return findDuplicateConflict(session.board) ?? findDeadCellConflict(session);
}

export function buildSudokuNextMoveHintFromMove(
  move: SudokuCanonicalMove,
): SudokuNextMoveHint {
  const strings = getSudokuStrings();
  const techniqueLabel = strings.learning.techniqueLabels[move.technique];
  const highlights = collectHouseHighlights(move.houses);

  if (move.kind === 'placement') {
    const cellLabel = formatCellLabel(move.target);
    const primaryHouse = move.houses[0];
    const houseLabel = primaryHouse ? formatHouseLabel(primaryHouse) : '';
    const body = move.technique === 'naked-single'
      ? strings.play.nextMove.nakedSingleBody(move.target.digit, cellLabel)
      : move.technique === 'hidden-single' && houseLabel
        ? strings.play.nextMove.hiddenSingleBody(move.target.digit, houseLabel, cellLabel)
        : strings.play.nextMove.placementBody(
          techniqueLabel,
          move.target.digit,
          cellLabel,
          joinLabels(move.houses.map(formatHouseLabel)),
        );

    return {
      kind: 'progress',
      ruleKey: move.technique,
      title: strings.play.nextMove.placementTitle(techniqueLabel, move.target.digit),
      body,
      evidenceCells: dedupeCells(move.evidenceCells),
      targetCells: [{ ...move.target, action: 'place' }],
      ...highlights,
    };
  }

  const uniqueTargets = dedupeCells(move.eliminations);
  const digitsLabel = Array.from(new Set(move.eliminations.map((cell) => cell.digit)))
    .sort((a, b) => a - b)
    .join(', ');
  const targetLabels = joinLabels(uniqueTargets.map(formatCellLabel));
  const houseLabels = joinLabels(move.houses.map(formatHouseLabel));
  const primaryHouse = move.houses[0];
  const secondaryHouse = move.houses[1];
  const body = (
    (move.technique === 'pointing-pair-triple' || move.technique === 'box-line-reduction')
    && primaryHouse
    && secondaryHouse
  )
    ? strings.play.nextMove.lockedCandidatesBody(
      digitsLabel,
      formatHouseLabel(primaryHouse),
      formatHouseLabel(secondaryHouse),
    )
    : strings.play.nextMove.eliminationBody(techniqueLabel, digitsLabel, targetLabels, houseLabels);

  return {
    kind: 'progress',
    ruleKey: move.technique,
    title: strings.play.nextMove.eliminationTitle(techniqueLabel, digitsLabel),
    body,
    evidenceCells: dedupeCells(move.evidenceCells),
    targetCells: move.eliminations.map((cell) => ({ ...cell, action: 'eliminate' })),
    ...highlights,
  };
}

export function getSudokuNextMoveHint(
  session: SudokuSession,
): SudokuNextMoveHint | null {
  const invalidState = findSudokuInvalidState(session);
  if (invalidState) {
    return {
      kind: 'invalid-board',
      ruleKey: 'invalid-board',
      targetCells: [],
      ...invalidState,
    };
  }

  const move = traceSudokuHumanSolve(session.board).moves[0] ?? null;
  if (!move) {
    return null;
  }

  return buildSudokuNextMoveHintFromMove(move);
}
