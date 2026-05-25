const en = {
  strings: {
    title: 'Nonogram',
    shortTitle: 'Nonogram',
    tagline: 'Fill cells to match the row and column clues.',
    difficultyLabels: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Small clue patterns with clear openings.',
      medium: 'Balanced grids that need steady deduction.',
      hard: 'Tighter clue sets that require deeper overlap checks.',
      expert: 'Dense grids with less obvious forced moves.',
    },
    play: {
      metadataLabels: {
        size: 'Size',
        difficulty: 'Difficulty',
      },
      helperToggle: {
        show: 'Show next move',
        hide: 'Hide next move',
      },
      noPuzzlesDialog: {
        title: 'No puzzles available',
        message: (difficultyLabel: string) => `No puzzles found in the ${difficultyLabel} catalog.`,
      },
      cellLabel: 'Cell',
      tutorial: {
        progressLabel: (step: number) => `Lesson ${step}`,
        introNote: 'Tap a cell to cycle blank → filled → empty. Swipe across a run to paint the same state across every visited cell.',
        exitLabel: {
          end: 'End tutorial',
          skip: 'Skip tutorial',
        },
        status: {
          finishing: 'Tutorial finishing…',
          nextLesson: 'Next lesson starting…',
          nextStep: 'Next step starting…',
        },
        answerPrompt: 'Should the highlighted target be filled or empty?',
        answerRetry: 'Not quite. Re-check the clues and try again.',
        selectAnswerLabel: (value: 0 | 1) => (value === 1 ? 'Filled' : 'Empty'),
      },
      analysis: {
        invalidBoard: {
          title: 'Invalid line',
          body: (lineLabel: string) => `${lineLabel} can no longer match its clues.`,
        },
        overlapFill: {
          title: 'Overlap fill',
          body: (lineLabel: string, targetCount: number) => `Fill the ${targetCount} overlapping cell${targetCount === 1 ? '' : 's'} in ${lineLabel}.`,
        },
        forcedEmpty: {
          title: 'Forced empty',
          body: (lineLabel: string, targetCount: number) => `Mark the ${targetCount} cell${targetCount === 1 ? '' : 's'} in ${lineLabel} as empty.`,
        },
        completeLine: {
          title: 'Complete line',
          body: (lineLabel: string, targetCount: number) => `Finish ${lineLabel} by marking the ${targetCount} remaining cell${targetCount === 1 ? '' : 's'} empty.`,
        },
      },
    },
  },
  tutorialLessons: {
    'tap-swipe': {
      title: 'Tap and swipe',
      body: 'Solve by filling every cell that must be shaded from the row and column clues. Tap cycles blank → filled → empty, and swipe paints the same state across a path.',
      summary: 'Your goal is to place all required filled cells; empty marks are optional helpers.',
      continueLabel: 'Continue',
    },
    'overlap-fill': {
      title: 'Overlap fill',
      body: 'This 5-cell line has clue 3. Every valid placement overlaps the middle cell, so that target must be filled.',
      summary: 'Use overlap to lock guaranteed filled cells first.',
      continueLabel: 'Check answer',
    },
    'forced-empty': {
      title: 'Forced empty',
      body: 'This line already has a filled cell on the left. The highlighted target cannot fit any valid placement, so the correct choice is empty.',
      summary: 'When a target cannot fit the clues, choose empty.',
      continueLabel: 'Check answer',
    },
    'complete-line': {
      title: 'Complete line',
      body: 'This line already satisfies all clue blocks. The remaining highlighted targets should be empty.',
      summary: 'After all clue blocks are placed, leftover targets are empty.',
      continueLabel: 'Finish tutorial',
    },
  },
  howToPlayRules: [
    {
      num: '1',
      title: 'Read the clues',
      body: 'Each row and column clue tells you the lengths of the filled blocks in order.',
    },
    {
      num: '2',
      title: 'Use overlap',
      body: 'If every valid placement for a block covers the same cell, that cell must be filled.',
    },
    {
      num: '3',
      title: 'Force empties',
      body: 'Cells that cannot belong to any valid block placement must stay empty.',
    },
    {
      num: '4',
      title: 'Finish each line',
      body: 'Once a line is complete, the remaining cells in that line must stay empty.',
    },
  ],
  howToPlayTips: [
    {
      key: 'overlap-fill',
      title: 'Overlap fill',
      body: 'When a block can only fit in one shared area, fill those overlapping cells.',
    },
    {
      key: 'forced-empty',
      title: 'Forced empty',
      body: 'If a cell cannot belong to any clue placement, mark it empty.',
    },
    {
      key: 'complete-line',
      title: 'Complete line',
      body: 'If a row or column already matches its clue pattern, the rest of that line is empty.',
    },
  ],
  loss: {
    abandoned: {
      eyebrow: 'Puzzle ended',
      title: 'Puzzle unfinished',
      body: 'You ended this puzzle before it was solved.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle lost',
      title: 'Puzzle lost',
      body: 'This puzzle ended before it could be solved.',
      icon: '⚠️',
    },
  },
} as const;

export default en;
