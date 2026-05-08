import type { PuzzleDifficulty } from '../../shared/types';

export type MinesweeperDeviceBucket = 'small-phone' | 'normal-phone' | 'large-phone' | 'tablet';

export interface MinesweeperSizeProfile {
  id: string;
  rows: number;
  cols: number;
}

export interface MinesweeperProfileLookup {
  profileId?: string;
  rows?: number;
  cols?: number;
}

export const MINESWEEPER_GRID_HORIZONTAL_PADDING = 12;

const MIN_RESPONSIVE_CELL_SIZE = 32;
const MINESWEEPER_FRAME_RESERVED_WIDTH = 14;
const MINESWEEPER_CELL_GAP = 4;

const DEVICE_CAPS: Record<MinesweeperDeviceBucket, { maxCols: number; maxRows: number }> = {
  'small-phone': { maxCols: 9, maxRows: 11 },
  'normal-phone': { maxCols: 10, maxRows: 12 },
  'large-phone': { maxCols: 11, maxRows: 13 },
  tablet: { maxCols: 12, maxRows: 14 },
};

const RESPONSIVE_PROFILES: Record<PuzzleDifficulty, readonly MinesweeperSizeProfile[]> = {
  easy: [
    { id: 'easy-9x9', rows: 9, cols: 9 },
    { id: 'easy-8x8', rows: 8, cols: 8 },
    { id: 'easy-7x7', rows: 7, cols: 7 },
  ],
  medium: [
    { id: 'medium-10x10', rows: 10, cols: 10 },
    { id: 'medium-9x10', rows: 10, cols: 9 },
    { id: 'medium-9x9', rows: 9, cols: 9 },
    { id: 'medium-8x9', rows: 9, cols: 8 },
    { id: 'medium-7x8', rows: 8, cols: 7 },
  ],
  hard: [
    { id: 'hard-11x11', rows: 11, cols: 11 },
    { id: 'hard-10x12', rows: 12, cols: 10 },
    { id: 'hard-10x11', rows: 11, cols: 10 },
    { id: 'hard-9x11', rows: 11, cols: 9 },
    { id: 'hard-8x10', rows: 10, cols: 8 },
    { id: 'hard-7x9', rows: 9, cols: 7 },
  ],
  expert: [
    { id: 'expert-11x13', rows: 13, cols: 11 },
    { id: 'expert-10x12', rows: 12, cols: 10 },
    { id: 'expert-9x11', rows: 11, cols: 9 },
    { id: 'expert-8x11', rows: 11, cols: 8 },
    { id: 'expert-7x10', rows: 10, cols: 7 },
  ],
};

const LEGACY_PROFILES: Record<PuzzleDifficulty, readonly MinesweeperSizeProfile[]> = {
  easy: [
    { id: 'legacy-easy-11x12', rows: 12, cols: 11 },
  ],
  medium: [
    { id: 'legacy-medium-11x14', rows: 14, cols: 11 },
  ],
  hard: [
    { id: 'legacy-hard-11x16', rows: 16, cols: 11 },
  ],
  expert: [
    { id: 'legacy-expert-11x18', rows: 18, cols: 11 },
  ],
};

function getRequiredBoardWidth(cols: number): number {
  return MINESWEEPER_FRAME_RESERVED_WIDTH
    + (cols * MIN_RESPONSIVE_CELL_SIZE)
    + (Math.max(cols - 1, 0) * MINESWEEPER_CELL_GAP);
}

function fitsDeviceCap(profile: MinesweeperSizeProfile, bucket: MinesweeperDeviceBucket): boolean {
  const cap = DEVICE_CAPS[bucket];
  return profile.cols <= cap.maxCols && profile.rows <= cap.maxRows;
}

function getAllProfiles(difficulty: PuzzleDifficulty): readonly MinesweeperSizeProfile[] {
  return [...RESPONSIVE_PROFILES[difficulty], ...LEGACY_PROFILES[difficulty]];
}

export function estimateMinesweeperPlayWidth(windowWidth: number): number {
  return Math.max(0, Math.floor(windowWidth) - (MINESWEEPER_GRID_HORIZONTAL_PADDING * 2));
}

export function getMinesweeperDeviceBucket(availableWidth: number): MinesweeperDeviceBucket {
  if (availableWidth >= getRequiredBoardWidth(12)) {
    return 'tablet';
  }
  if (availableWidth >= getRequiredBoardWidth(11)) {
    return 'large-phone';
  }
  if (availableWidth >= getRequiredBoardWidth(10)) {
    return 'normal-phone';
  }
  return 'small-phone';
}

export function selectMinesweeperSizeProfile(
  difficulty: PuzzleDifficulty,
  availableWidth: number,
): MinesweeperSizeProfile {
  const bucket = getMinesweeperDeviceBucket(availableWidth);
  const profiles = RESPONSIVE_PROFILES[difficulty];
  const profile = profiles.find((candidate) => (
    fitsDeviceCap(candidate, bucket) && availableWidth >= getRequiredBoardWidth(candidate.cols)
  )) ?? profiles.find((candidate) => availableWidth >= getRequiredBoardWidth(candidate.cols));

  return profile ?? profiles[profiles.length - 1];
}

export function resolveMinesweeperSizeProfile(
  difficulty: PuzzleDifficulty,
  lookup: MinesweeperProfileLookup,
): MinesweeperSizeProfile | null {
  const profiles = getAllProfiles(difficulty);

  if (lookup.profileId) {
    const matchedProfile = profiles.find((profile) => profile.id === lookup.profileId);
    if (matchedProfile) {
      return matchedProfile;
    }
  }

  if (lookup.rows && lookup.cols) {
    const matchedProfile = profiles.find((profile) => (
      profile.rows === lookup.rows && profile.cols === lookup.cols
    ));
    if (matchedProfile) {
      return matchedProfile;
    }
  }

  return null;
}
