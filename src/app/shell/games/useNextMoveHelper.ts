import { useCallback, useMemo, useState } from 'react';

type NextMoveHintResolver<TSession, THint> = (session: TSession) => THint | null;

export interface NextMoveHelperState<THint> {
  visible: boolean;
  hint: THint | null;
  reset: () => void;
}

export interface NextMoveHelperActions<TSession> {
  toggle: (session: TSession | null | undefined) => void;
}

export function useNextMoveHelper<TSession, THint>(
  getHint: NextMoveHintResolver<TSession, THint>,
): NextMoveHelperState<THint> & NextMoveHelperActions<TSession> {
  const [hint, setHint] = useState<THint | null>(null);
  const [visible, setVisible] = useState(false);

  const reset = useCallback(() => {
    setHint(null);
    setVisible(false);
  }, []);

  const toggle = useCallback((session: TSession | null | undefined) => {
    if (visible) {
      reset();
      return;
    }

    if (!session) {
      return;
    }

    const nextHint = getHint(session);
    if (!nextHint) {
      reset();
      return;
    }

    setHint(nextHint);
    setVisible(true);
  }, [getHint, reset, visible]);

  return useMemo(() => ({
    visible,
    hint: visible ? hint : null,
    reset,
    toggle,
  }), [hint, reset, toggle, visible]);
}
