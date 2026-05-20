import { useEffect, useRef } from 'react';

interface UseActiveSessionPersistenceArgs<T> {
  enabled: boolean;
  value: T | null;
  changeKey?: string | null | (() => string | null);
  save: (value: T) => Promise<void>;
}

export function useActiveSessionPersistence<T>({
  enabled,
  value,
  changeKey,
  save,
}: UseActiveSessionPersistenceArgs<T>) {
  const previousSerializedRef = useRef<string | null>(null);

  useEffect(() => {
    if (!enabled || value === null) return;

    const serialized = typeof changeKey === 'function'
      ? changeKey()
      : changeKey ?? JSON.stringify(value);
    if (previousSerializedRef.current === serialized) return;
    previousSerializedRef.current = serialized;
    void save(value);
  }, [changeKey, enabled, save, value]);
}
