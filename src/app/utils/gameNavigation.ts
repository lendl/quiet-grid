import { StackActions, type NavigationProp } from '@react-navigation/native';
import type { ActivePuzzle } from '../shell/activePuzzleTypes';
import type { RootStackParamList } from '../navigation/types';
import type { Difficulty, PuzzleTypeId } from '../types';
import { getActivePuzzleDifficulty } from './activePuzzle';

type RootNavigation = NavigationProp<RootStackParamList>;

export function startGame(
  navigation: RootNavigation,
  puzzleTypeId: PuzzleTypeId,
  difficulty: Difficulty,
  replace = false,
) {
  const params = { puzzleTypeId, difficulty };

  if (replace) {
    navigation.dispatch(StackActions.replace('PuzzlePlay', params));
    return;
  }

  navigation.navigate('PuzzlePlay', params);
}

export function resumeActivePuzzle(navigation: RootNavigation, activePuzzle: ActivePuzzle) {
  const difficulty = getActivePuzzleDifficulty(activePuzzle);
  navigation.navigate('PuzzlePlay', { puzzleTypeId: activePuzzle.puzzleTypeId, difficulty, resume: true });
}
