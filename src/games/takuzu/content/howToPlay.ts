import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getCurrentLanguage } from '../../../app/i18n';
import { getTakuzuHowToPlayTips } from './howToPlayTips';

export function getTakuzuHowToPlay(): HowToPlayContent {
  if (getCurrentLanguage() === 'nl') {
    return {
      rules: [
        {
          num: '1',
          title: 'Vul elke cel',
          body: 'Tik op een cel om te wisselen: leeg -> 0 -> 1 -> leeg. Vul het volledige rooster.',
        },
        {
          num: '2',
          title: 'Geen drie op rij',
          body: 'Vermijd drie gelijke cijfers naast elkaar in een rij of kolom.',
        },
        {
          num: '3',
          title: 'Gelijke helften',
          body: 'Elke rij en elke kolom moet exact evenveel 0\'en en 1\'en bevatten.',
        },
        {
          num: '4',
          title: 'Alle lijnen zijn uniek',
          body: 'Geen twee rijen mogen identiek zijn, en geen twee kolommen mogen identiek zijn.',
        },
      ],
      tips: getTakuzuHowToPlayTips(),
    };
  }

  return {
    rules: [
      {
        num: '1',
        title: 'Fill every cell',
        body: 'Tap a cell to cycle: empty -> 0 -> 1 -> empty. Fill the entire grid.',
      },
      {
        num: '2',
        title: 'No three in a row',
        body: 'Avoid placing three identical digits next to each other in any row or column.',
      },
      {
        num: '3',
        title: 'Equal halves',
        body: 'Every row and every column must contain exactly the same number of 0s and 1s.',
      },
      {
        num: '4',
        title: 'All lines are unique',
        body: 'No two rows may be identical, and no two columns may be identical.',
      },
    ],
    tips: getTakuzuHowToPlayTips(),
  };
}
