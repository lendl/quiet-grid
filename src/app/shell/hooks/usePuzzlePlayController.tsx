import { useCallback } from 'react';
import { useIsFocused } from '@react-navigation/native';
import type { StackScreenProps } from '@react-navigation/stack';
import type { RootStackParamList } from '../../navigation/types';
import { returnToHome } from '../../navigation/returnToHome';
import {
  clearActivePuzzleState,
  saveActivePuzzleState,
} from '../../utils/activePuzzleStateStorage';
import { getAppStrings } from '../../i18n';
import { getPuzzleDefinition } from '../games/gameRegistry';
import type { PuzzleRenderState } from '../games/playAdapter';
import type { PuzzlePlayLayoutState } from '../playScreenTypes';
import { useLeavePuzzleDialog } from './useLeavePuzzleDialog';
import { usePuzzleControllerBootstrap } from './usePuzzleControllerBootstrap';

type Props = StackScreenProps<RootStackParamList, 'PuzzlePlay'>;

export function usePuzzlePlayController(props: Props): PuzzlePlayLayoutState {
  const strings = getAppStrings();
  const definition = getPuzzleDefinition(props.route.params.puzzleTypeId);
  const adapter = definition.playAdapter;
  const { dialog, setDialog } = useLeavePuzzleDialog();
  const isFocused = useIsFocused();
  const difficulty = props.route.params.difficulty ?? 'easy';
  const resumeRequested = props.route.params.resume === true;

  const goHome = useCallback(() => {
    returnToHome(props.navigation);
  }, [props.navigation]);
  const goBack = useCallback(() => {
    props.navigation.goBack();
  }, [props.navigation]);

  const adapterInstance = adapter.useAdapter({
    difficulty,
    resumeRequested,
    setDialog,
    goHome,
    goBack,
  });
  const {
    onMissing,
    onFreshMissing,
    onBeforeLoad,
    onCleanup,
    getState,
  } = adapterInstance;

  const handleMissingPuzzle = useCallback(async () => {
    if (onMissing) {
      await onMissing();
      return;
    }
    props.navigation.replace('Home');
  }, [onMissing, props.navigation]);

  const handleBeforeLoad = useCallback(() => {
    setDialog(null);
    onBeforeLoad?.();
  }, [onBeforeLoad, setDialog]);

  const handleCleanup = useCallback(() => {
    setDialog(null);
    onCleanup?.();
  }, [onCleanup, setDialog]);

  const showCompletionScreen = useCallback((params: RootStackParamList['Completion']) => {
    props.navigation.replace('Completion', params);
  }, [props.navigation]);

  const showLossScreen = useCallback((params: RootStackParamList['Loss']) => {
    props.navigation.replace('Loss', params);
  }, [props.navigation]);

  const bootstrap = usePuzzleControllerBootstrap({
    isFocused,
    puzzleTypeId: props.route.params.puzzleTypeId,
    difficulty,
    resumeRequested,
    contract: adapter.contract,
    onMissing: handleMissingPuzzle,
    onFreshMissing: onFreshMissing ?? handleMissingPuzzle,
    saveActivePuzzle: saveActivePuzzleState,
    clearActivePuzzle: clearActivePuzzleState,
    onShowCompletion: showCompletionScreen,
    onShowLoss: showLossScreen,
    onBeforeLoad: handleBeforeLoad,
    onCleanup: handleCleanup,
  });
  const {
    session,
    setSession,
    loading,
    running,
    setRunning,
    sessionRef,
    finalizedRef,
    getCurrentElapsedSeconds,
    pauseTimer,
    finishSolvedSession,
    completeExitToHome,
    loadFreshSession,
    finishLossSession,
  } = bootstrap;

  const handleForfeit = useCallback(() => {
    setDialog({
      title: strings.puzzlePlay.endDialogTitle,
      message: strings.puzzlePlay.endDialogMessage,
      buttons: [
        { text: strings.common.cancel, style: 'cancel', onPress: () => setDialog(null) },
        {
          text: strings.puzzlePlay.endDialogConfirm,
          style: 'destructive',
          onPress: async () => {
            setDialog(null);
            await finishLossSession('forfeit');
          },
        },
      ],
    });
  }, [
    finishLossSession,
    setDialog,
    strings.common.cancel,
    strings.puzzlePlay.endDialogConfirm,
    strings.puzzlePlay.endDialogMessage,
    strings.puzzlePlay.endDialogTitle,
  ]);

  const runImmediateAction = useCallback(async (action: unknown) => {
    const currentSession = sessionRef.current;
    if (!adapterInstance.runImmediateAction || !currentSession) {
      return;
    }
    if (!adapter.contract.isInProgress(currentSession)) {
      return;
    }

    const result = adapterInstance.runImmediateAction.run(currentSession, action);
    if (!result.changed) {
      return;
    }

    sessionRef.current = result.session;
    setSession(result.session);

    const solved = await finishSolvedSession(result.session);
    if (solved) {
      return;
    }

    await adapterInstance.handleEffects?.({
      previousSession: currentSession,
      session: result.session,
      effects: result.effects,
      setSession,
      sessionRef,
      finalizedRef,
      setRunning,
      pauseTimer,
      finishSolvedSession,
      finishLossSession,
      loadFreshSession,
      setDialog,
      goHome,
    });
  }, [
    adapter.contract,
    adapterInstance,
    finalizedRef,
    finishLossSession,
    finishSolvedSession,
    loadFreshSession,
    pauseTimer,
    sessionRef,
    setRunning,
    setSession,
    setDialog,
    goHome,
  ]);

  const renderState: PuzzleRenderState<unknown, unknown> = {
    session,
    setSession,
    loading,
    running,
    setRunning,
    elapsedSeconds: getCurrentElapsedSeconds(),
    sessionRef,
    finalizedRef,
    pauseTimer,
    finishSolvedSession,
    completeExitToHome,
    loadFreshSession,
    runImmediateAction,
    setDialog,
    goHome,
    goBack,
  };

  const adapterState = getState(renderState);

  const exitToHome = adapterState.exitToHome ?? (async () => {
    await completeExitToHome(sessionRef.current);
    goHome();
  });

  return {
    loading: adapterState.loading ?? loading,
    loadingLabel: adapterState.loadingLabel ?? strings.puzzlePlay.loading,
    elapsedSeconds: renderState.elapsedSeconds,
    dialog,
    onDismissDialog: () => setDialog(null),
    exitToHome,
    onForfeit: handleForfeit,
    headerActions: adapterState.headerActions ?? [],
    headerMeta: adapterState.headerMeta ?? [],
    main: adapterState.main,
    footer: adapterState.footer ?? null,
  };
}
