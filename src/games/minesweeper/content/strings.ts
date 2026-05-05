import { getCurrentLanguage } from '../../../app/i18n';
import type { Difficulty } from '../../../app/types';

interface MinesweeperStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
}

const EN_STRINGS: MinesweeperStrings = {
  title: 'Minesweeper',
  shortTitle: 'Minesweeper',
  tagline: 'Clear grid without opening mine.',
  difficultyLabels: {
    easy: 'Easy',
    medium: 'Medium',
    hard: 'Hard',
    expert: 'Expert',
  },
  difficultyDescriptions: {
    easy: 'More breathing room for early scanning and steady clue reading.',
    medium: 'A balanced board with more mines and fewer safe openings.',
    hard: 'Tighter spaces that reward careful flagging and clue tracking.',
    expert: 'Dense minefields with very little breathing room from the start.',
  },
};

const NL_STRINGS: MinesweeperStrings = {
  title: 'Minesweeper',
  shortTitle: 'Minesweeper',
  tagline: 'Maak het rooster vrij zonder een mijn te openen.',
  difficultyLabels: {
    easy: 'Makkelijk',
    medium: 'Gemiddeld',
    hard: 'Moeilijk',
    expert: 'Expert',
  },
  difficultyDescriptions: {
    easy: 'Meer ademruimte voor vroeg scannen en rustig aanwijzingen lezen.',
    medium: 'Een gebalanceerd bord met meer mijnen en minder veilige openingen.',
    hard: 'Strakkere ruimtes die zorgvuldig markeren en aanwijzingen volgen belonen.',
    expert: 'Dichte mijnenvelden met vanaf het begin heel weinig ademruimte.',
  },
};

export function getMinesweeperStrings(): MinesweeperStrings {
  return getCurrentLanguage() === 'nl' ? NL_STRINGS : EN_STRINGS;
}
