import { getTakuzuHowToPlayTips as resolveTakuzuHowToPlayTips } from '../i18n';

export type HowToPlayCellValue = 0 | 1 | null | 'a0' | 'a1';

export type HowToPlayTipKey =
  | 'find-pairs'
  | 'avoid-trios'
  | 'complete-lines'
  | 'eliminate-filled-lines'
  | 'eliminate-impossible-combinations'
  | 'score-matters'
  | 'watch-for-flashes';

export interface HowToPlayTip {
  key: HowToPlayTipKey;
  title: string;
  body: string;
  example?: readonly (readonly HowToPlayCellValue[])[];
}

export function getTakuzuHowToPlayTips(): HowToPlayTip[] {
  return resolveTakuzuHowToPlayTips();
}
