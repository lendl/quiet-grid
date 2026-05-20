import { useCallback, useState } from 'react';
import { useFocusEffect } from '@react-navigation/native';
import type { AppStats } from '../../types';
import { loadStats } from '../../utils/statsStorage';

export interface StatsSnapshotResult {
  stats: AppStats | null;
  reload: () => Promise<void>;
}

export function useStatsSnapshot(): StatsSnapshotResult {
  const [stats, setStats] = useState<AppStats | null>(null);

  const reload = useCallback(async () => {
    const nextStats = await loadStats();
    setStats(nextStats);
  }, []);

  useFocusEffect(useCallback(() => {
    void reload();
    return undefined;
  }, [reload]));

  return { stats, reload };
}
