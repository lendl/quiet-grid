import type { WordSearchI18n } from './index';

const en: WordSearchI18n = {
  strings: {
    title: 'Word Search',
    shortTitle: 'Words',
    tagline: 'Trace listed words in straight lines and solve the hidden bonus word from the grid.',
    difficultyLabels: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Horizontal and vertical forward words with generous spacing.',
      medium: 'Adds diagonal forward words and tighter clusters.',
      hard: 'All directions including backwards with denser overlaps.',
      expert: 'Largest grids with full-direction words and heavy overlap.',
    },
    play: {
      metadataLabels: {
        size: 'Size',
        difficulty: 'Difficulty',
        theme: 'Theme',
        found: 'Found',
      },
      noPuzzlesDialog: {
        title: 'Word Search unavailable',
        message: (difficultyLabel: string) => `No Word Search puzzle is ready for ${difficultyLabel} yet.`,
      },
      helperToggle: {
        show: 'Show next word hint',
        hide: 'Hide next word hint',
      },
      hiddenWord: {
        locked: 'Find all the words to unlock the hidden word.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        solvedTitle: 'Hidden word found!',
        enterMode: 'Solve hidden word',
        exitMode: 'Exit hidden-word mode',
        instructions: 'The hidden-word letters sit in the empty cells, left to right then top to bottom. Tap them in order.',
        resetOnMistake: 'A wrong hidden-word tap resets your progress.',
        nextLetterTitle: (clue: string) => `Hidden word: ${clue}`,
        nextLetterBody: 'The highlighted cell is the next hidden-word letter in order.',
      },
      nextMove: {
        title: (word: string) => `Find "${word}"`,
        body: 'Start from the highlighted letter and trace the full word in one straight line.',
      },
    },
  },
  howToPlay: {
    goal: 'Find every listed word hidden in the grid, then solve the hidden bonus word to solve the puzzle.',
    controls: 'Tap the first letter of a word, then tap the last letter to select. Words run in straight lines: horizontal, vertical, or diagonal.',
    wrongMove: 'Invalid selections are silently ignored — there is no penalty for tracing wrong paths.',
    rules: [
      {
        num: '1',
        title: 'Trace straight lines',
        body: 'Every listed word appears in one straight line: horizontal, vertical, or diagonal depending on difficulty.',
      },
      {
        num: '2',
        title: 'Invalid selections, no penalties',
        body: 'Invalid selections are ignored, so you can keep scanning without losing the run.',
      },
      {
        num: '3',
        title: 'Solve the hidden word',
        body: 'The hidden-word letters fill the empty cells in reading order — left to right, top to bottom. Once all words are found, tap each letter in sequence to win.',
      },
    ],
    techniques: [],
    tips: [
      {
        key: 'scan-first-letter',
        title: 'Start with unique first letters',
        body: 'Find uncommon initials in the grid first, then expand in all allowed directions.',
      },
      {
        key: 'follow-line',
        title: 'Commit one line at a time',
        body: 'Begin a selection, drag to the endpoint, and commit. Starting a new selection replaces the temporary one.',
      },
    ],
  },
  loss: {
    abandoned: {
      eyebrow: 'Puzzle ended',
      title: 'Word Search session ended',
      body: 'You ended this Word Search session before solving every listed word and the hidden word.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'No loss condition',
      title: 'Word Search has no rule-failure loss',
      body: 'Invalid selections are no-ops in Word Search. Keep scanning and continue the puzzle.',
      icon: '🔎',
    },
  },
};

export default en;
