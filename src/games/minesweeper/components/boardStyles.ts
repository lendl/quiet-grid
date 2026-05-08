import type { TextStyle, ViewStyle } from 'react-native';
import type { Theme } from '../../../app/theme';
import { withAlpha } from '../../../app/utils/color';

const FRAME_BORDER_WIDTH = 3;
const FRAME_PADDING = 4;
const CELL_GAP = 4;
const CELL_BORDER_RADIUS = 4;

export const MINESWEEPER_BOARD_RESERVED_WIDTH = (FRAME_BORDER_WIDTH * 2) + (FRAME_PADDING * 2);
export const MINESWEEPER_BOARD_CELL_GAP = CELL_GAP;

interface SharedMinesweeperBoardStyles {
  frame: ViewStyle;
  row: ViewStyle;
  lastRow: ViewStyle;
  cell: ViewStyle;
  hiddenCell: ViewStyle;
  revealedCell: ViewStyle;
  flaggedCell: ViewStyle;
  label: TextStyle;
  emptyLabel: TextStyle;
}

export function getMinesweeperNumberColor(theme: Theme, adjacentMines: number): string {
  if (adjacentMines === 1) return theme.primaryLight;
  if (adjacentMines === 2) return theme.success;
  if (adjacentMines === 3) return theme.difficultyHard;
  if (adjacentMines >= 4) return theme.difficultyExpert;
  return theme.textSecondary;
}

export function getMinesweeperBoardCellSize(
  containerWidth: number,
  cols: number,
  minCellSize: number,
  maxCellSize: number,
  rows?: number,
  containerHeight?: number,
): number {
  const safeCols = Math.max(cols, 1);
  const reservedWidth = MINESWEEPER_BOARD_RESERVED_WIDTH
    + (Math.max(safeCols - 1, 0) * MINESWEEPER_BOARD_CELL_GAP);
  const widthCellSize = Math.floor((containerWidth - reservedWidth) / safeCols);
  const heightCellSize = typeof rows === 'number' && typeof containerHeight === 'number'
    ? Math.floor((
      containerHeight
      - MINESWEEPER_BOARD_RESERVED_WIDTH
      - (Math.max(rows - 1, 0) * MINESWEEPER_BOARD_CELL_GAP)
    ) / Math.max(rows, 1))
    : widthCellSize;
  const fittedCellSize = Math.min(widthCellSize, heightCellSize);

  if (fittedCellSize >= minCellSize) {
    return Math.min(maxCellSize, fittedCellSize);
  }

  return Math.max(1, Math.min(maxCellSize, fittedCellSize));
}

export function buildSharedMinesweeperBoardStyles(
  theme: Theme,
  cellSize: number,
): SharedMinesweeperBoardStyles {
  return {
    frame: {
      alignSelf: 'center',
      borderWidth: FRAME_BORDER_WIDTH,
      borderColor: theme.gridFrame,
      backgroundColor: theme.gridFrame,
      borderRadius: 18,
      padding: FRAME_PADDING,
    },
    row: {
      flexDirection: 'row',
      gap: CELL_GAP,
      marginBottom: CELL_GAP,
    },
    lastRow: {
      marginBottom: 0,
    },
    cell: {
      width: cellSize,
      height: cellSize,
      borderWidth: 1,
      borderColor: theme.gridCellBorder,
      borderRadius: CELL_BORDER_RADIUS,
      alignItems: 'center',
      justifyContent: 'center',
    },
    hiddenCell: {
      backgroundColor: theme.surfaceElevated,
    },
    revealedCell: {
      backgroundColor: theme.gridCellBackground,
    },
    flaggedCell: {
      backgroundColor: withAlpha(theme.primaryLight, 0.2),
    },
    label: {
      fontSize: cellSize * 0.46,
      fontWeight: '800',
      color: theme.text,
    },
    emptyLabel: {
      color: 'transparent',
    },
  };
}
