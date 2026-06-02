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
        enterMode: 'Solve hidden word',
        exitMode: 'Exit hidden-word mode',
        progress: (current: number, total: number) => `Hidden word ${current}/${total}`,
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
        prompt: 'Which path is valid for a Word Search selection?',
        validOption: 'Straight path',
        invalidOption: 'Bent path',
        correctFeedback: 'Correct — Word Search only accepts straight-line paths.',
        wrongFeedback: 'Not quite. Bent paths are ignored in Word Search.',
      },
    },
  },
  howToPlay: {
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
    'scan-lines': {
      title: 'Scan rows, columns, and diagonals',
      body: 'Word Search words are always straight. Look for the first letter, then test each legal direction.',
      summary: 'Straight-line scanning beats random tapping.',
      continueLabel: 'Next lesson',
    },
    'drag-selection': {
      title: 'Drag one clean selection',
      body: 'Begin on one letter and drag to the endpoint. Invalid spans are ignored without penalties.',
      summary: 'New selections replace old temporary spans.',
      continueLabel: 'Next lesson',
    },
    'hidden-word': {
      title: 'Switch into hidden-word mode',
      body: 'Use the hidden-word action during play, then tap the hidden-word letters in order on the live grid.',
      summary: 'The hidden word is solved directly during play.',
      continueLabel: 'Start puzzle',
    },
  },
};

export default en;
