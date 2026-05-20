import type { ActiveSessionTemplate } from '../types';
import type { GamePlaySessionTemplate } from './activePuzzle';

export function restoreGamePlaySession(activeSession: ActiveSessionTemplate): GamePlaySessionTemplate {
  return {
    puzzle: activeSession.puzzle,
    score: activeSession.score,
    mistakes: activeSession.mistakes,
    loss: activeSession.loss,
  };
}

export const playContractTemplate = {
  id: '__GAME_ID__',
  createSession: '__WIRE_CREATE_PLAY_SESSION__',
  canResume: '__WIRE_CAN_RESUME_ACTIVE_SESSION__',
  restoreSession: '__WIRE_RESTORE_PLAY_SESSION__',
  serializeSession: '__WIRE_SERIALIZE_ACTIVE_SESSION__',
  getPersistenceKey: '__WIRE_PERSISTENCE_KEY_OR_NULL__',
  getHudState: '__WIRE_HUD_STATE__',
  getSolvedState: '__WIRE_SOLVED_STATE_OR_NULL__',
  isInProgress: '__WIRE_IS_IN_PROGRESS__',
  hasMeaningfulProgress: '__WIRE_HAS_MEANINGFUL_PROGRESS__',
} as const;

export const gameSemanticsTemplate = {
  canonicalMoves: [
    {
      key: '__MOVE_KEY__',
      title: '__MOVE_TITLE__',
      summary: '__WHY_THIS_MOVE_WORKS__',
    },
  ],
  supportActions: [
    {
      key: '__SUPPORT_ACTION_KEY__',
      title: '__SUPPORT_ACTION_TITLE__',
      optional: true,
    },
  ],
  analyzerMode: 'loss-state',
  mistakePolicy: {
    hasMistakes: true,
    detection: '__WIRE_MISTAKE_DETECTION__',
    affectsScore: true,
    endsRun: false,
  },
  hasLossCondition: false,
} as const;
