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
        locked: 'Solve the hidden word from the grid whenever you are ready.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        solvedTitle: 'Hidden word found!',
        enterMode: 'Solve hidden word',
        exitMode: 'Exit hidden-word mode',
        instructions: 'Tap the hidden-word letters in order on the grid.',
        resetOnMistake: 'A wrong hidden-word tap resets your progress.',
        nextLetterTitle: (clue: string) => `Hidden word: ${clue}`,
        nextLetterBody: 'The highlighted cell is the next hidden-word letter in order.',
      },
      nextMove: {
        title: (word: string) => `Find "${word}"`,
        body: 'Start from the highlighted letter and trace the full word in one straight line.',
      },
    },
    tutorial: {
      progressLabel: (step: number, total: number) => `Lesson ${step} of ${total}`,
      exitLabel: {
        skip: 'Skip tutorial',
        end: 'Open Word Search',
      },
      checkpoint: {
        prompt: 'You want to select STAR. After tapping S, what do you tap next?',
        validOption: 'R — the last letter',
        invalidOption: 'T — the next letter',
        correctFeedback: 'Correct — tap the first letter, then the last. The path is traced automatically.',
        wrongFeedback: 'Not quite. Tap the first and last letter only, not each letter in between.',
      },
    },
  },
  howToPlay: {
    goal: 'Find every listed word hidden in the grid, then solve the hidden bonus word to complete the puzzle.',
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
        title: 'No mistakes, no penalties',
        body: 'Invalid selections are ignored, so you can keep scanning without losing the run.',
      },
      {
        num: '3',
        title: 'Solve the hidden word',
        body: 'Use hidden-word mode during play, then tap the hidden-word letters in order on the grid.',
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
  tutorialLessons: {
    'win-condition': {
      title: 'Find the hidden word to win',
      body: 'The listed words are theme clues — finding them helps you identify the hidden bonus word. Finding the hidden word is the only way to win.',
      summary: 'Regular words are clues. The hidden word is the goal.',
      continueLabel: 'Next lesson',
    },
    'selection': {
      title: 'Tap start, then tap end',
      body: 'Tap the first letter of a word, then the last. The game traces the path — horizontal, vertical, diagonal, or a single corner turn.',
      summary: 'Two taps select a word.',
      continueLabel: 'Next lesson',
    },
    'no-penalty': {
      title: 'Wrong guesses just disappear',
      body: 'If no word is found the selection resets — no penalty. Scan freely and try any start and end combination you like.',
      summary: 'Nothing to lose — keep scanning.',
      continueLabel: 'Next lesson',
    },
    'hidden-word': {
      title: 'Find the hidden word',
      body: 'Tap the key icon to enter hidden-word mode. The letters are scattered anywhere in the grid. The footer shows each letter as you tap it — find them all to win.',
      summary: 'Footer progress guides you letter by letter.',
      continueLabel: 'Start puzzle',
    },
  },
};

export default en;
