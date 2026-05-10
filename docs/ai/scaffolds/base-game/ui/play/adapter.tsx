import type { ReactNode } from 'react';

export interface PlayAdapterStateTemplate {
  headerMeta: readonly { key: string; label: string; value: string }[];
  main: ReactNode;
  footer: ReactNode;
}

export const playAdapterTemplate = {
  getState(): PlayAdapterStateTemplate {
    return {
      headerMeta: [
        { key: 'difficulty', label: 'Difficulty', value: '__DIFFICULTY_LABEL__' },
      ],
      main: null,
      footer: null,
    };
  },
  onMissing: '__SHOW_MISSING_PUZZLE_DIALOG__',
  onFreshMissing: '__SHOW_EMPTY_CATALOG_DIALOG__',
} as const;
