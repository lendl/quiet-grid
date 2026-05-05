import { useState } from 'react';
import type { DialogConfig } from '../../components/AppDialog';

export function useLeavePuzzleDialog() {
  const [dialog, setDialog] = useState<DialogConfig | null>(null);
  return { dialog, setDialog };
}
