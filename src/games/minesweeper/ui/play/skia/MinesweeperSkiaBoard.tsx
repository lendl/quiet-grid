import React, { useMemo } from 'react';
import {
  Canvas,
  Circle,
  Line,
  RoundedRect,
  Text as SkiaText,
  matchFont,
} from '@shopify/react-native-skia';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  type FixedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/skia/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/skia/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import type { MinesweeperBoard as MinesweeperBoardState } from '../../../types';
import { getMinesweeperNumberColor } from '../components/boardStyles';

interface MinesweeperSkiaBoardProps {
  board: MinesweeperBoardState;
  layout: FixedGridLayout;
  nextMoveEvidenceCells?: ReadonlyArray<{ row: number; col: number }>;
  nextMoveSafeTargetCells?: ReadonlyArray<{ row: number; col: number }>;
  nextMoveMineTargetCells?: ReadonlyArray<{ row: number; col: number }>;
}

function getCenteredTextOrigin(
  rect: { x: number; y: number; width: number; height: number },
  font: ReturnType<typeof matchFont>,
  text: string,
) {
  const textBounds = font.measureText(text);
  const metrics = font.getMetrics();
  const textHeight = metrics.descent - metrics.ascent;

  return {
    x: rect.x + (rect.width - textBounds.width) / 2,
    y: rect.y + (rect.height - textHeight) / 2 - metrics.ascent,
  };
}

export default function MinesweeperSkiaBoard({
  board,
  layout,
  nextMoveEvidenceCells = [],
  nextMoveSafeTargetCells = [],
  nextMoveMineTargetCells = [],
}: MinesweeperSkiaBoardProps) {
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const numberFont = useMemo(() => matchFont({
    fontFamily: 'monospace',
    fontSize: layout.cellSize * 0.46,
    fontWeight: '700',
  }), [layout.cellSize]);
  const evidenceKeys = useMemo(() => new Set(
    nextMoveEvidenceCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveEvidenceCells]);
  const safeTargetKeys = useMemo(() => new Set(
    nextMoveSafeTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveSafeTargetCells]);
  const mineTargetKeys = useMemo(() => new Set(
    nextMoveMineTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveMineTargetCells]);

  return (
    <Canvas style={{ width: layout.frameWidth, height: layout.frameHeight }}>
      <RoundedRect
        x={0}
        y={0}
        width={layout.frameWidth}
        height={layout.frameHeight}
        r={18}
        color={theme.gridFrame}
      />
      {board.cells.map((row, rowIndex) => row.map((cell, colIndex) => {
        const key = `${rowIndex}:${colIndex}`;
        const rect = getGridCellRect(layout, rowIndex, colIndex);
        const fill = mineTargetKeys.has(key)
          ? withAlpha(theme.difficultyExpert, isDark ? 0.28 : 0.16)
          : safeTargetKeys.has(key)
            ? withAlpha(theme.success, isDark ? 0.3 : 0.16)
            : evidenceKeys.has(key)
              ? withAlpha(theme.primary, isDark ? 0.14 : 0.08)
              : cell.state === 'flagged'
                ? withAlpha(theme.primaryLight, 0.2)
                : cell.state === 'revealed' && cell.isMine
                  ? withAlpha(theme.difficultyExpert, 0.22)
                  : cell.state === 'revealed'
                    ? theme.gridCellBackground
                    : theme.surfaceElevated;
        const border = mineTargetKeys.has(key)
          ? withAlpha(theme.difficultyExpert, isDark ? 0.84 : 0.68)
          : safeTargetKeys.has(key)
            ? withAlpha(theme.success, isDark ? 0.86 : 0.72)
            : evidenceKeys.has(key)
              ? withAlpha(theme.primary, isDark ? 0.62 : 0.44)
              : theme.gridCellBorder;
        const inset = 1;
        const innerRect = {
          x: rect.x + inset,
          y: rect.y + inset,
          width: Math.max(0, rect.width - inset * 2),
          height: Math.max(0, rect.height - inset * 2),
        };

        return (
          <React.Fragment key={key}>
            <RoundedRect
              x={rect.x}
              y={rect.y}
              width={rect.width}
              height={rect.height}
              r={4}
              color={border}
            />
            <RoundedRect
              x={innerRect.x}
              y={innerRect.y}
              width={innerRect.width}
              height={innerRect.height}
              r={Math.max(0, 4 - inset)}
              color={fill}
            />
            {cell.state === 'flagged' ? (
              <>
                <Line
                  p1={{ x: rect.x + rect.width * 0.35, y: rect.y + rect.height * 0.2 }}
                  p2={{ x: rect.x + rect.width * 0.35, y: rect.y + rect.height * 0.8 }}
                  color={tokens.text}
                  strokeWidth={2}
                />
                <Line
                  p1={{ x: rect.x + rect.width * 0.35, y: rect.y + rect.height * 0.22 }}
                  p2={{ x: rect.x + rect.width * 0.72, y: rect.y + rect.height * 0.36 }}
                  color={mineTargetKeys.has(key) ? theme.difficultyExpert : safeTargetKeys.has(key) ? theme.success : theme.primaryLight}
                  strokeWidth={2}
                />
                <Line
                  p1={{ x: rect.x + rect.width * 0.72, y: rect.y + rect.height * 0.36 }}
                  p2={{ x: rect.x + rect.width * 0.35, y: rect.y + rect.height * 0.48 }}
                  color={mineTargetKeys.has(key) ? theme.difficultyExpert : safeTargetKeys.has(key) ? theme.success : theme.primaryLight}
                  strokeWidth={2}
                />
              </>
            ) : null}
            {cell.state === 'revealed' && cell.isMine ? (
              <>
                <Circle
                  cx={rect.x + rect.width / 2}
                  cy={rect.y + rect.height / 2}
                  r={rect.width * 0.18}
                  color={mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger}
                />
                <Line
                  p1={{ x: rect.x + rect.width * 0.15, y: rect.y + rect.height * 0.5 }}
                  p2={{ x: rect.x + rect.width * 0.85, y: rect.y + rect.height * 0.5 }}
                  color={mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger}
                  strokeWidth={2}
                />
                <Line
                  p1={{ x: rect.x + rect.width * 0.5, y: rect.y + rect.height * 0.15 }}
                  p2={{ x: rect.x + rect.width * 0.5, y: rect.y + rect.height * 0.85 }}
                  color={mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger}
                  strokeWidth={2}
                />
              </>
            ) : null}
            {cell.state === 'revealed' && !cell.isMine && cell.adjacentMines > 0 ? (
              <SkiaText
                font={numberFont}
                text={String(cell.adjacentMines)}
                color={mineTargetKeys.has(key)
                  ? theme.difficultyExpert
                  : safeTargetKeys.has(key)
                    ? theme.success
                    : getMinesweeperNumberColor(theme, cell.adjacentMines)}
                {...getCenteredTextOrigin(innerRect, numberFont, String(cell.adjacentMines))}
              />
            ) : null}
          </React.Fragment>
        );
      }))}
    </Canvas>
  );
}
