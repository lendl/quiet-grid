import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getTakuzuHowToPlayRules, getTakuzuHowToPlayTips } from './i18n';

export function getTakuzuHowToPlay(): HowToPlayContent {
  return {
    rules: getTakuzuHowToPlayRules(),
    tips: getTakuzuHowToPlayTips(),
  };
}
