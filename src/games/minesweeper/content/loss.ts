import type { LossContent } from '../../../app/shell/games/lossContent';
import { getCurrentLanguage } from '../../../app/i18n';

export function getMinesweeperLossContent(): LossContent {
  if (getCurrentLanguage() === 'nl') {
    return {
      forfeit: {
        eyebrow: 'Puzzel beëindigd',
        title: 'Puzzel niet af',
        body: 'Je hebt deze puzzel beëindigd voordat hij was opgelost.',
        icon: '🏁',
      },
      'rule-based': {
        eyebrow: 'Puzzel verloren',
        title: 'Puzzel verloren',
        body: 'Deze puzzel eindigde toen een mijn werd geopend. Er staat een nieuwe puzzel klaar wanneer jij wilt.',
        icon: '💣',
      },
    };
  }

  return {
    forfeit: {
      eyebrow: 'Puzzle ended',
      title: 'Puzzle unfinished',
      body: 'You ended this puzzle before it was solved.',
      icon: '🏁',
    },
    'rule-based': {
      eyebrow: 'Puzzle lost',
      title: 'Puzzle lost',
      body: 'This puzzle ended when a mine was opened. A fresh puzzle is ready when you want one.',
      icon: '💣',
    },
  };
}
