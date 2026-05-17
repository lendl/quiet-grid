import type { PuzzleDifficulty } from '../../shared/types';

export interface MinesweeperSizeProfile {
  id: string;
  rows: number;
  cols: number;
  compatibilityIds?: readonly string[];
}

export interface MinesweeperProfileLookup {
  profileId?: string;
  rows?: number;
  cols?: number;
}

const SIZE_PROFILES: Record<PuzzleDifficulty, readonly MinesweeperSizeProfile[]> = {
  easy: [
    { id: 'easy-11x12', rows: 12, cols: 11, compatibilityIds: ['legacy-easy-11x12'] },
  ],
  medium: [
    { id: 'medium-11x14', rows: 14, cols: 11, compatibilityIds: ['legacy-medium-11x14'] },
  ],
  hard: [
    { id: 'hard-11x16', rows: 16, cols: 11, compatibilityIds: ['legacy-hard-11x16'] },
  ],
  expert: [
    { id: 'expert-11x18', rows: 18, cols: 11, compatibilityIds: ['legacy-expert-11x18'] },
  ],
};

function getProfilesForDifficulty(difficulty: PuzzleDifficulty): readonly MinesweeperSizeProfile[] {
  return SIZE_PROFILES[difficulty];
}

export function getMinesweeperSizeProfile(
  difficulty: PuzzleDifficulty,
): MinesweeperSizeProfile {
  const profiles = SIZE_PROFILES[difficulty];
  const profile = profiles[0];

  if (!profile) {
    throw new Error(`Unsupported Minesweeper difficulty: ${difficulty}`);
  }

  return profile;
}

export function resolveMinesweeperSizeProfile(
  difficulty: PuzzleDifficulty,
  lookup: MinesweeperProfileLookup,
): MinesweeperSizeProfile | null {
  const profiles = getProfilesForDifficulty(difficulty);

  if (lookup.profileId) {
    const profileId = lookup.profileId;
    const matchedProfile = profiles.find((profile) => (
      profile.id === profileId || profile.compatibilityIds?.includes(profileId)
    ));
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
