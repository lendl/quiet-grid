import { getCurrentLanguage } from '../../../app/i18n';
import type { Difficulty } from '../../../app/types';

interface TakuzuStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
}

const EN_STRINGS: TakuzuStrings = {
  title: 'Takuzu',
  shortTitle: 'Takuzu',
  tagline: 'Fill grid with 0s and 1s using logic.',
  difficultyLabels: {
    easy: 'Easy',
    medium: 'Medium',
    hard: 'Hard',
    expert: 'Expert',
  },
  difficultyDescriptions: {
    easy: 'More starting cells and gentler deductions.',
    medium: 'Balanced openings that ask you to read the grid a bit further ahead.',
    hard: 'Tighter setups with less free information and stronger pattern work.',
    expert: 'Sparse openings with sustained deduction pressure across the whole puzzle.',
  },
};

const NL_STRINGS: TakuzuStrings = {
  title: 'Takuzu',
  shortTitle: 'Takuzu',
  tagline: 'Vul het rooster met 0 en 1 via logica.',
  difficultyLabels: {
    easy: 'Makkelijk',
    medium: 'Gemiddeld',
    hard: 'Moeilijk',
    expert: 'Expert',
  },
  difficultyDescriptions: {
    easy: 'Meer startcellen en mildere deducties.',
    medium: 'Gebalanceerde openingen die je vragen iets verder vooruit te lezen.',
    hard: 'Strakkere opzetten met minder vrije informatie en sterkere patroonherkenning.',
    expert: 'Schaarse openingen met aanhoudende deductiedruk over het hele bord.',
  },
};

export function getTakuzuStrings(): TakuzuStrings {
  return getCurrentLanguage() === 'nl' ? NL_STRINGS : EN_STRINGS;
}
