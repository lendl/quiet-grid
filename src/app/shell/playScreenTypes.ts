import type { ReactNode } from 'react';
import type { DialogConfig } from '../components/AppDialog';
import type { PuzzleMetaItem } from './games/playAdapter';

export interface PuzzlePlayLayoutState {
  loading: boolean;
  loadingLabel?: string;
  elapsedSeconds: number;
  dialog: DialogConfig | null;
  onDismissDialog: () => void;
  showHelperToggle: boolean;
  helperToggleLabel?: string;
  helperVisible: boolean;
  onToggleHelper?: () => void;
  exitToHome: () => Promise<void>;
  onForfeit: () => void;
  metadata: PuzzleMetaItem[];
  grid: ReactNode;
  footer: ReactNode;
}
