import { StackActions, type NavigationProp } from '@react-navigation/native';
import type { ActiveSession } from '../shell/activeSessionTypes';
import type { RootStackParamList } from '../navigation/types';
import type { Difficulty, GameId } from '../types';
import { getActivePuzzleDifficulty } from './activePuzzle';

type RootNavigation = NavigationProp<RootStackParamList>;

export function startGame(
  navigation: RootNavigation,
  gameId: GameId,
  difficulty: Difficulty,
  replace = false,
) {
  const params = { puzzleTypeId: gameId, difficulty };

  if (replace) {
    navigation.dispatch(StackActions.replace('PuzzlePlay', params));
    return;
  }

  navigation.navigate('PuzzlePlay', params);
}

export function resumeActiveSession(navigation: RootNavigation, activeSession: ActiveSession) {
  const difficulty = getActivePuzzleDifficulty(activeSession);
  navigation.navigate('PuzzlePlay', { puzzleTypeId: activeSession.gameId, difficulty, resume: true });
}
