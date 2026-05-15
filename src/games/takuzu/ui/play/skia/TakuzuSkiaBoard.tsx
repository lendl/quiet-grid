import React, { useMemo } from 'react';
import {
  Canvas,
  Group,
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
import type {
  Grid,
  TakuzuNextMoveCell,
  TakuzuNextMoveTargetCell,
} from '../../../types';

interface TakuzuSkiaBoardProps {
  board: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  layout: FixedGridLayout;
  nextMoveEvidenceCells?: readonly TakuzuNextMoveCell[];
  nextMoveTargetCells?: readonly TakuzuNextMoveTargetCell[];
  nextMoveHighlightRows?: readonly number[];
  nextMoveHighlightCols?: readonly number[];
  suppressedTextKeys?: ReadonlySet<string>;
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

export default function TakuzuSkiaBoard({
  board,
  isGiven,
  finishedCells,
  layout,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
  suppressedTextKeys,
}: TakuzuSkiaBoardProps) {
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const regularFont = useMemo(() => matchFont({
    fontFamily: 'monospace',
    fontSize: layout.cellSize * 0.46,
    fontWeight: '700',
  }), [layout.cellSize]);
  const lockedFont = useMemo(() => matchFont({
    fontFamily: 'monospace',
    fontSize: layout.cellSize * 0.46,
    fontWeight: '800',
  }), [layout.cellSize]);
  const badgeFont = useMemo(() => matchFont({
    fontSize: Math.max(9, Math.floor(layout.cellSize * 0.16)),
    fontWeight: '900',
  }), [layout.cellSize]);
  const evidenceKeys = useMemo(() => new Set(
    nextMoveEvidenceCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveEvidenceCells]);
  const targetKeys = useMemo(() => new Set(
    nextMoveTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);
  const shellInnerInset = 1;
  const gridInset = 1;

  return (
    <Canvas style={{ width: layout.frameWidth, height: layout.frameHeight }}>
      <RoundedRect
        x={0}
        y={0}
        width={layout.frameWidth}
        height={layout.frameHeight}
        r={22}
        color={tokens.panelBorder}
      />
      <RoundedRect
        x={shellInnerInset}
        y={shellInnerInset}
        width={layout.frameWidth - shellInnerInset * 2}
        height={layout.frameHeight - shellInnerInset * 2}
        r={21}
        color={tokens.panelFill}
      />
      <RoundedRect
        x={layout.padding - gridInset}
        y={layout.padding - gridInset}
        width={layout.contentWidth + gridInset * 2}
        height={layout.contentHeight + gridInset * 2}
        r={16}
        color={tokens.subtleGridFill}
      />
      {board.map((row, rowIndex) => row.map((value, colIndex) => {
        const key = `${rowIndex}:${colIndex}`;
        const rect = getGridCellRect(layout, rowIndex, colIndex);
        const locked = isGiven[rowIndex][colIndex] || finishedCells[rowIndex][colIndex];
        const fill = targetKeys.has(key)
          ? withAlpha(theme.primary, isDark ? 0.28 : 0.18)
          : evidenceKeys.has(key)
            ? withAlpha(theme.primary, isDark ? 0.16 : 0.1)
            : highlightedRows.has(rowIndex) || highlightedCols.has(colIndex)
              ? withAlpha(theme.primary, isDark ? 0.08 : 0.05)
              : locked
                ? withAlpha(theme.surfaceElevated, isDark ? 0.92 : 0.68)
                : withAlpha(theme.background, isDark ? 0.94 : 1);
        const border = targetKeys.has(key)
          ? withAlpha(theme.primaryLight, isDark ? 0.88 : 0.7)
          : evidenceKeys.has(key)
            ? withAlpha(theme.primary, isDark ? 0.6 : 0.42)
            : highlightedRows.has(rowIndex) || highlightedCols.has(colIndex)
              ? withAlpha(theme.primary, isDark ? 0.28 : 0.2)
              : withAlpha(theme.border, isDark ? 0.48 : 0.62);
        const textColor = locked
          ? withAlpha(theme.text, isDark ? 0.9 : 0.82)
          : withAlpha(theme.text, isDark ? 0.98 : 0.88);
        const radius = Math.max(8, Math.floor(layout.cellSize * 0.14));
        const cellInset = 1;
        const badgeSize = Math.max(16, Math.floor(layout.cellSize * 0.34));
        const badgeRect = {
          x: rect.x + rect.width - badgeSize - Math.max(2, Math.floor(layout.cellSize * 0.05)),
          y: rect.y + Math.max(2, Math.floor(layout.cellSize * 0.05)),
          width: badgeSize,
          height: badgeSize,
        };

        return (
          <Group key={key}>
            <RoundedRect
              x={rect.x}
              y={rect.y}
              width={rect.width}
              height={rect.height}
              r={radius}
              color={border}
            />
            <RoundedRect
              x={rect.x + cellInset}
              y={rect.y + cellInset}
              width={Math.max(0, rect.width - cellInset * 2)}
              height={Math.max(0, rect.height - cellInset * 2)}
              r={Math.max(0, radius - cellInset)}
              color={fill}
            />
            {targetKeys.has(key) ? (
              <>
                <RoundedRect
                  x={badgeRect.x}
                  y={badgeRect.y}
                  width={badgeRect.width}
                  height={badgeRect.height}
                  r={badgeRect.height / 2}
                  color={theme.primary}
                />
                <SkiaText
                  font={badgeFont}
                  text="i"
                  color={tokens.onPrimary}
                  {...getCenteredTextOrigin(badgeRect, badgeFont, 'i')}
                />
              </>
            ) : null}
            {value !== null && !suppressedTextKeys?.has(key) ? (
              <SkiaText
                font={locked ? lockedFont : regularFont}
                text={String(value)}
                color={textColor}
                {...getCenteredTextOrigin(rect, locked ? lockedFont : regularFont, String(value))}
              />
            ) : null}
          </Group>
        );
      }))}
    </Canvas>
  );
}
