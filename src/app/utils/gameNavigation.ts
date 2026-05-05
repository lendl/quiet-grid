import type { StackNavigationProp } from '@react-navigation/stack';
import type { ActivePuzzle } from '../shell/activePuzzleTypes';
import type { RootStackParamList } from '../navigation/types';
import type { Difficulty, PuzzleTypeId } from '../types';
import { getActivePuzzleDifficulty } from './activePuzzle';

type RootNavigation = StackNavigationProp<RootStackParamList>;

export function startGame(
  navigation: RootNavigation,
  puzzleTypeId: PuzzleTypeId,
  difficulty: Difficulty,
  replace = false,
) {
  const params = { puzzleTypeId, difficulty };

  if (replace) {
    navigation.replace('PuzzlePlay', params);
    return;
  }

  navigation.navigate('PuzzlePlay', params);
}

export function resumeActivePuzzle(navigation: RootNavigation, activePuzzle: ActivePuzzle) {
  const difficulty = getActivePuzzleDifficulty(activePuzzle);
  navigation.navigate('PuzzlePlay', { puzzleTypeId: activePuzzle.puzzleTypeId, difficulty, resume: true });
}
