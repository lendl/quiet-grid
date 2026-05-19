export type BoardFeedbackCell = {
  row: number;
  col: number;
};

export type BoardFeedbackKind = 'spin' | 'shake';

export type BoardFeedbackEffect = {
  id: number | string;
  kind: BoardFeedbackKind;
  cells: readonly BoardFeedbackCell[];
};

export function buildBoardFeedbackCellKey(cell: BoardFeedbackCell): string {
  return `${cell.row}:${cell.col}`;
}

export function buildBoardFeedbackCellsFromLines(
  rows: readonly number[],
  cols: readonly number[],
  size: number,
): BoardFeedbackCell[] {
  const keys = new Set<string>();
  const cells: BoardFeedbackCell[] = [];

  rows.forEach((row) => {
    for (let col = 0; col < size; col += 1) {
      const cell = { row, col };
      const key = buildBoardFeedbackCellKey(cell);

      if (!keys.has(key)) {
        keys.add(key);
        cells.push(cell);
      }
    }
  });

  cols.forEach((col) => {
    for (let row = 0; row < size; row += 1) {
      const cell = { row, col };
      const key = buildBoardFeedbackCellKey(cell);

      if (!keys.has(key)) {
        keys.add(key);
        cells.push(cell);
      }
    }
  });

  return cells;
}

export function getBoardFeedbackPriority(kind: BoardFeedbackKind): number {
  return kind === 'shake' ? 2 : 1;
}

export function createBoardFeedbackLookup(
  effects: readonly BoardFeedbackEffect[],
): Map<string, BoardFeedbackKind> {
  const lookup = new Map<string, BoardFeedbackKind>();

  effects.forEach((effect) => {
    effect.cells.forEach((cell) => {
      const key = buildBoardFeedbackCellKey(cell);
      const current = lookup.get(key);

      if (
        !current
        || getBoardFeedbackPriority(effect.kind) >= getBoardFeedbackPriority(current)
      ) {
        lookup.set(key, effect.kind);
      }
    });
  });

  return lookup;
}
