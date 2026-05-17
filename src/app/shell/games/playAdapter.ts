import type {
  Dispatch,
  ReactNode,
  SetStateAction,
} from 'react';
import type { MutableRefObject } from 'react';
import type { ActivePuzzle } from '../activePuzzleTypes';
import type { DialogConfig } from '../../components/AppDialog';
import type { PuzzlePlayContract, PuzzlePlayContractBase } from '../playContract';
import type { PuzzleDifficulty } from '../types';

export interface PuzzleMetaItem {
  key: string;
  label: string;
  value: string;
}

export interface PuzzleHeaderAction {
  key: string;
  accessibilityLabel: string;
  iconName: 'bulb' | 'bulb-outline' | 'refresh-outline';
  active?: boolean;
  onPress: () => void;
}

export interface PuzzlePlayAdapterShellArgs {
  difficulty: PuzzleDifficulty;
  resumeRequested: boolean;
  setDialog: Dispatch<SetStateAction<DialogConfig | null>>;
  goHome: () => void;
  goBack: () => void;
}

export interface PuzzleRenderState<TSession, TAction = unknown> {
  session: TSession | null;
  setSession: Dispatch<SetStateAction<TSession | null>>;
  loading: boolean;
  running: boolean;
  setRunning: Dispatch<SetStateAction<boolean>>;
  elapsedSeconds: number;
  sessionRef: MutableRefObject<TSession | null>;
  finalizedRef: MutableRefObject<boolean>;
  pauseTimer: () => number;
  finishSolvedSession: (
    solvedSession?: TSession,
    showCompletionScreen?: boolean,
  ) => Promise<boolean>;
  completeExitToHome: (sessionOverride?: TSession | null) => Promise<void>;
  loadFreshSession: () => Promise<TSession | null>;
  runImmediateAction: (action: TAction) => Promise<void>;
  setDialog: Dispatch<SetStateAction<DialogConfig | null>>;
  goHome: () => void;
  goBack: () => void;
}

export interface PuzzleActionResult<TSession, TEffect> {
  changed: boolean;
  session: TSession;
  effects: readonly TEffect[];
}

export interface PuzzleImmediateActionRunner<TSession, TAction = unknown, TEffect = unknown> {
  run(this: void, session: TSession, action: TAction): PuzzleActionResult<TSession, TEffect>;
}

export interface PuzzleEffectHandlerArgs<TSession, TEffect = unknown> {
  previousSession: TSession;
  session: TSession;
  effects: readonly TEffect[];
  setSession: Dispatch<SetStateAction<TSession | null>>;
  sessionRef: MutableRefObject<TSession | null>;
  finalizedRef: MutableRefObject<boolean>;
  setRunning: Dispatch<SetStateAction<boolean>>;
  pauseTimer: () => number;
  finishSolvedSession: (
    solvedSession?: TSession,
    showCompletionScreen?: boolean,
  ) => Promise<boolean>;
  finishLossSession: (reason: 'forfeit' | 'rule-based', sessionOverride?: TSession | null) => Promise<void>;
  loadFreshSession: () => Promise<TSession | null>;
  setDialog: Dispatch<SetStateAction<DialogConfig | null>>;
  goHome: () => void;
}

export interface PuzzlePlayAdapterState {
  loading?: boolean;
  loadingLabel?: string;
  exitToHome?: () => Promise<void>;
  headerActions?: readonly PuzzleHeaderAction[];
  headerMeta?: readonly PuzzleMetaItem[];
  main: ReactNode;
  footer?: ReactNode;
}

export interface PuzzlePlayAdapterInstance<TSession = unknown, TAction = unknown, TEffect = unknown> {
  onMissing?(this: void): void | Promise<void>;
  onFreshMissing?(this: void): void | Promise<void>;
  onBeforeLoad?(this: void): void;
  onCleanup?(this: void): void;
  runImmediateAction?: PuzzleImmediateActionRunner<TSession, TAction, TEffect>;
  handleEffects?(this: void, args: PuzzleEffectHandlerArgs<TSession, TEffect>): Promise<void> | void;
  getState(this: void, args: PuzzleRenderState<TSession, TAction>): PuzzlePlayAdapterState;
}

export interface PuzzlePlayAdapterShellInstance {
  onMissing?(this: void): void | Promise<void>;
  onFreshMissing?(this: void): void | Promise<void>;
  onBeforeLoad?(this: void): void;
  onCleanup?(this: void): void;
  runImmediateAction?: PuzzleImmediateActionRunner<unknown, unknown, unknown>;
  handleEffects?(this: void, args: PuzzleEffectHandlerArgs<unknown, unknown>): Promise<void> | void;
  getState(this: void, args: PuzzleRenderState<unknown, unknown>): PuzzlePlayAdapterState;
}

export interface PuzzlePlayAdapterBase {
  contract: PuzzlePlayContractBase;
  useAdapter(this: void, args: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterShellInstance;
}

export interface PuzzlePlayAdapter<
  TSession,
  TActivePuzzle extends ActivePuzzle,
  THud,
  TAction = unknown,
  TEffect = unknown,
> {
  contract: PuzzlePlayContract<TSession, TActivePuzzle, THud>;
  useAdapter(this: void, args: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<TSession, TAction, TEffect>;
}

export function createPuzzlePlayAdapter<
  TSession,
  TActivePuzzle extends ActivePuzzle,
  THud,
  TAction = unknown,
  TEffect = unknown,
>(
  adapter: PuzzlePlayAdapter<TSession, TActivePuzzle, THud, TAction, TEffect>,
): PuzzlePlayAdapterBase {
  return {
    contract: adapter.contract,
    useAdapter(args) {
      const instance = adapter.useAdapter(args);

      return {
        onMissing: instance.onMissing,
        onFreshMissing: instance.onFreshMissing,
        onBeforeLoad: instance.onBeforeLoad,
        onCleanup: instance.onCleanup,
        runImmediateAction: instance.runImmediateAction
          ? {
              run(session: unknown, action: unknown) {
                return instance.runImmediateAction!.run(session as TSession, action as TAction);
              },
            }
          : undefined,
        handleEffects: instance.handleEffects
          ? (effectArgs) => instance.handleEffects!({
              session: effectArgs.session as TSession,
              previousSession: effectArgs.previousSession as TSession,
              effects: effectArgs.effects as readonly TEffect[],
              setSession: effectArgs.setSession,
              sessionRef: effectArgs.sessionRef as MutableRefObject<TSession | null>,
              finalizedRef: effectArgs.finalizedRef,
              setRunning: effectArgs.setRunning,
              pauseTimer: effectArgs.pauseTimer,
              finishSolvedSession: effectArgs.finishSolvedSession,
              finishLossSession: effectArgs.finishLossSession,
              loadFreshSession: effectArgs.loadFreshSession as () => Promise<TSession | null>,
              setDialog: effectArgs.setDialog,
              goHome: effectArgs.goHome,
            })
          : undefined,
        getState(renderState) {
          return instance.getState({
            session: renderState.session as TSession | null,
            setSession: renderState.setSession,
            loading: renderState.loading,
            running: renderState.running,
            setRunning: renderState.setRunning,
            elapsedSeconds: renderState.elapsedSeconds,
            sessionRef: renderState.sessionRef as MutableRefObject<TSession | null>,
            finalizedRef: renderState.finalizedRef,
            pauseTimer: renderState.pauseTimer,
            finishSolvedSession: renderState.finishSolvedSession,
            completeExitToHome: renderState.completeExitToHome,
            loadFreshSession: renderState.loadFreshSession as () => Promise<TSession | null>,
            runImmediateAction: (action: TAction) => renderState.runImmediateAction(action),
            setDialog: renderState.setDialog,
            goHome: renderState.goHome,
            goBack: renderState.goBack,
          });
        },
      };
    },
  };
}
