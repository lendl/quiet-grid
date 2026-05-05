import type { LossContent } from '../../../app/shell/games/lossContent';
import { getCurrentLanguage } from '../../../app/i18n';

export function getBinaryLossContent(): LossContent {
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
        body: 'Deze puzzel eindigde voordat hij kon worden opgelost.',
        icon: '⚠️',
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
      body: 'This puzzle ended before it could be solved.',
      icon: '⚠️',
    },
  };
}
