import type { ReactNode } from 'react';
import type { DialogConfig } from '../components/AppDialog';
import type { PuzzleHeaderAction, PuzzleMetaItem } from './games/playAdapter';

export interface PuzzlePlayLayoutState {
  loading: boolean;
  loadingLabel?: string;
  elapsedSeconds: number;
  dialog: DialogConfig | null;
  onDismissDialog: () => void;
  exitToHome: () => Promise<void>;
  onForfeit: () => void;
  headerActions: readonly PuzzleHeaderAction[];
  headerMeta: readonly PuzzleMetaItem[];
  main: ReactNode;
  footer: ReactNode;
}
