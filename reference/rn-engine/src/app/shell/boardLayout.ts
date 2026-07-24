export interface FixedGridLayoutInput {
  rows: number;
  cols: number;
  cellSize: number;
  gap: number;
  padding: number;
  borderWidth?: number;
}

export interface FixedGridLayout {
  rows: number;
  cols: number;
  cellSize: number;
  gap: number;
  padding: number;
  borderWidth: number;
  contentWidth: number;
  contentHeight: number;
  frameWidth: number;
  frameHeight: number;
}

export interface BoundedGridLayoutInput {
  containerWidth: number;
  containerHeight: number;
  rows: number;
  cols: number;
  gap: number;
  padding: number;
  minCellSize: number;
  maxCellSize: number;
  borderWidth?: number;
}

export interface GridCellRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface NonogramBoardLayoutInput {
  containerWidth: number;
  rows: number;
  cols: number;
  rowClues: readonly (readonly number[])[];
  colClues: readonly (readonly number[])[];
  interactive: boolean;
}

export interface NonogramBoardLayout {
  boardWidth: number;
  boardHeight: number;
  cellSize: number;
  cellGap: number;
  boardCardPadding: number;
  boardSectionGap: number;
  gridPadding: number;
  rowClueValueGap: number;
  colClueValueGap: number;
  rowClueDepth: number;
  colClueDepth: number;
  rowClueSlotSize: number;
  colClueSlotSize: number;
  rowRailWidth: number;
  colRailHeight: number;
  rowRailX: number;
  rowRailY: number;
  colRailX: number;
  colRailY: number;
  gridFrameX: number;
  gridFrameY: number;
  gridFrameWidth: number;
  gridFrameHeight: number;
  gridX: number;
  gridY: number;
}

const NONOGRAM_GRID_PADDING = 4;
const NONOGRAM_CELL_GAP = 2;
const NONOGRAM_BOARD_SECTION_GAP = 8;
const NONOGRAM_BOARD_CARD_PADDING = 12;
const NONOGRAM_ROW_CLUE_VALUE_GAP = 4;
const NONOGRAM_COL_CLUE_VALUE_GAP = 2;

export function createFixedGridLayout({
  rows,
  cols,
  cellSize,
  gap,
  padding,
  borderWidth = 0,
}: FixedGridLayoutInput): FixedGridLayout {
  const safeRows = Math.max(rows, 1);
  const safeCols = Math.max(cols, 1);
  const contentWidth = safeCols * cellSize + Math.max(0, safeCols - 1) * gap;
  const contentHeight = safeRows * cellSize + Math.max(0, safeRows - 1) * gap;

  return {
    rows: safeRows,
    cols: safeCols,
    cellSize,
    gap,
    padding,
    borderWidth,
    contentWidth,
    contentHeight,
    frameWidth: contentWidth + (padding * 2) + (borderWidth * 2),
    frameHeight: contentHeight + (padding * 2) + (borderWidth * 2),
  };
}

export function createBoundedGridLayout({
  containerWidth,
  containerHeight,
  rows,
  cols,
  gap,
  padding,
  minCellSize,
  maxCellSize,
  borderWidth = 0,
}: BoundedGridLayoutInput): FixedGridLayout {
  const safeRows = Math.max(rows, 1);
  const safeCols = Math.max(cols, 1);
  const usableWidth = containerWidth
    - (borderWidth * 2)
    - (padding * 2)
    - Math.max(0, safeCols - 1) * gap;
  const usableHeight = containerHeight
    - (borderWidth * 2)
    - (padding * 2)
    - Math.max(0, safeRows - 1) * gap;
  const fittedCellSize = Math.floor(Math.min(usableWidth / safeCols, usableHeight / safeRows));
  const cellSize = fittedCellSize >= minCellSize
    ? Math.min(maxCellSize, fittedCellSize)
    : Math.max(1, Math.min(maxCellSize, fittedCellSize));

  return createFixedGridLayout({
    rows: safeRows,
    cols: safeCols,
    cellSize,
    gap,
    padding,
    borderWidth,
  });
}

export function getGridCellRect(
  layout: Pick<FixedGridLayout, 'cellSize' | 'gap' | 'padding' | 'borderWidth'>,
  row: number,
  col: number,
): GridCellRect {
  return {
    x: layout.borderWidth + layout.padding + col * (layout.cellSize + layout.gap),
    y: layout.borderWidth + layout.padding + row * (layout.cellSize + layout.gap),
    width: layout.cellSize,
    height: layout.cellSize,
  };
}

function getMaxClueDepth(lines: readonly (readonly number[])[]): number {
  return Math.max(
    1,
    ...lines.map((line) => (line.length === 1 && line[0] === 0 ? 0 : line.length)),
  );
}

export function padNonogramClues(
  clues: readonly number[],
  targetLength: number,
): Array<number | null> {
  const normalized = clues.length === 1 && clues[0] === 0 ? [] : [...clues];
  const padding = Array.from({ length: Math.max(0, targetLength - normalized.length) }, () => null);
  return [...padding, ...normalized];
}

export function createNonogramBoardLayout({
  containerWidth,
  rows,
  cols,
  rowClues,
  colClues,
  interactive,
}: NonogramBoardLayoutInput): NonogramBoardLayout {
  const rowClueDepth = getMaxClueDepth(rowClues);
  const colClueDepth = getMaxClueDepth(colClues);
  const boardWidth = Math.min(containerWidth || 384, interactive ? 456 : 420);
  const minCellSize = interactive ? 30 : 24;
  const maxCellSize = interactive ? 44 : 38;
  const baseGridWidth = boardWidth
    - (NONOGRAM_BOARD_CARD_PADDING * 2)
    - NONOGRAM_BOARD_SECTION_GAP
    - NONOGRAM_GRID_PADDING * 2
    - Math.max(0, cols - 1) * NONOGRAM_CELL_GAP;
  const estimatedCellSize = Math.max(
    minCellSize,
    Math.min(maxCellSize, Math.floor(baseGridWidth / (cols + rowClueDepth * 0.72))),
  );
  const rowClueSlotSize = Math.max(20, Math.floor(estimatedCellSize * 0.72));
  const colClueSlotSize = Math.max(20, Math.floor(estimatedCellSize * 0.72));
  const rowRailWidth = rowClueDepth * rowClueSlotSize
    + Math.max(0, rowClueDepth - 1) * NONOGRAM_ROW_CLUE_VALUE_GAP;
  const colRailHeight = colClueDepth * colClueSlotSize
    + Math.max(0, colClueDepth - 1) * NONOGRAM_COL_CLUE_VALUE_GAP;
  const gridInnerWidth = boardWidth
    - (NONOGRAM_BOARD_CARD_PADDING * 2)
    - rowRailWidth
    - NONOGRAM_BOARD_SECTION_GAP
    - NONOGRAM_GRID_PADDING * 2
    - Math.max(0, cols - 1) * NONOGRAM_CELL_GAP;
  const cellSize = Math.max(minCellSize, Math.min(maxCellSize, Math.floor(gridInnerWidth / cols)));
  const gridWidth = cols * cellSize + Math.max(0, cols - 1) * NONOGRAM_CELL_GAP;
  const gridHeight = rows * cellSize + Math.max(0, rows - 1) * NONOGRAM_CELL_GAP;
  const gridFrameWidth = gridWidth + NONOGRAM_GRID_PADDING * 2;
  const gridFrameHeight = gridHeight + NONOGRAM_GRID_PADDING * 2;
  const rowRailX = NONOGRAM_BOARD_CARD_PADDING;
  const colRailY = NONOGRAM_BOARD_CARD_PADDING;
  const gridFrameX = NONOGRAM_BOARD_CARD_PADDING + rowRailWidth + NONOGRAM_BOARD_SECTION_GAP;
  const gridFrameY = NONOGRAM_BOARD_CARD_PADDING + colRailHeight + NONOGRAM_BOARD_SECTION_GAP;
  const gridX = gridFrameX + NONOGRAM_GRID_PADDING;
  const gridY = gridFrameY + NONOGRAM_GRID_PADDING;

  return {
    boardWidth,
    boardHeight: gridFrameY + gridFrameHeight + NONOGRAM_BOARD_CARD_PADDING,
    cellSize,
    cellGap: NONOGRAM_CELL_GAP,
    boardCardPadding: NONOGRAM_BOARD_CARD_PADDING,
    boardSectionGap: NONOGRAM_BOARD_SECTION_GAP,
    gridPadding: NONOGRAM_GRID_PADDING,
    rowClueValueGap: NONOGRAM_ROW_CLUE_VALUE_GAP,
    colClueValueGap: NONOGRAM_COL_CLUE_VALUE_GAP,
    rowClueDepth,
    colClueDepth,
    rowClueSlotSize,
    colClueSlotSize,
    rowRailWidth,
    colRailHeight,
    rowRailX,
    rowRailY: gridY,
    colRailX: gridX,
    colRailY,
    gridFrameX,
    gridFrameY,
    gridFrameWidth,
    gridFrameHeight,
    gridX,
    gridY,
  };
}

export function getNonogramCellRect(
  layout: Pick<NonogramBoardLayout, 'gridX' | 'gridY' | 'cellSize' | 'cellGap'>,
  row: number,
  col: number,
): GridCellRect {
  return {
    x: layout.gridX + col * (layout.cellSize + layout.cellGap),
    y: layout.gridY + row * (layout.cellSize + layout.cellGap),
    width: layout.cellSize,
    height: layout.cellSize,
  };
}
