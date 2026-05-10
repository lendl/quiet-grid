import type { NonogramI18n } from './index';

const en: NonogramI18n = {
  strings: {
    title: 'Nonogram',
    shortTitle: 'Nonogram',
    tagline: 'Paint the picture with pure logic.',
    difficultyLabels: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Large clues and immediate forced moves.',
      medium: 'Needs overlap and cross-line deduction.',
      hard: 'Not used in Nonogram yet.',
      expert: 'Not used in Nonogram yet.',
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
        title: 'No Nonogram puzzles yet',
        message: 'Generate some Nonogram puzzles for this difficulty first.',
      },
    },
    tutorial: {
      progressLabel: (step, total) => `Lesson ${step} of ${total}`,
      exitLabel: {
        end: 'End',
        skip: 'Skip',
      },
      status: {
        finishing: 'Wrapping up...',
        nextLesson: 'Next lesson...',
      },
      actionLabels: {
        filled: 'Fill',
        marked: 'Mark with X',
      },
    },
    analyzer: {
      legendEvidence: 'Evidence',
      legendTarget: 'Next move',
      noAnalysisTitle: 'No guided continuation',
      noAnalysisBody: 'This board has no simple logical step from the current state.',
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Read the clues',
        body: 'Each row and column clue tells you the lengths of filled blocks in that line.',
      },
      {
        num: '2',
        title: 'Keep blocks separate',
        body: 'Different clue blocks must have at least one empty cell between them.',
      },
      {
        num: '3',
        title: 'Use fills and X marks',
        body: 'Fill cells that must be part of the picture and mark cells that cannot be filled.',
      },
    ],
    tips: [
      {
        key: 'overlap',
        title: 'Use overlap',
        body: 'If a block can start in only a small range, the middle cells may be forced.',
      },
      {
        key: 'empty-lines',
        title: 'Mark impossible cells',
        body: 'When every valid placement leaves a cell empty, mark it with an X.',
      },
      {
        key: 'cross-check',
        title: 'Cross-check rows and columns',
        body: 'A new fill in one row often unlocks forced moves in a column, and the other way around.',
      },
    ],
  },
  loss: {
    forfeit: {
      eyebrow: 'Nonogram',
      title: 'Puzzle forfeited',
      body: 'Study the next logical steps and see how the picture comes together.',
      icon: 'flag-outline',
    },
    'rule-based': {
      eyebrow: 'Nonogram',
      title: 'Puzzle ended',
      body: 'Review the board and follow the logical continuation from here.',
      icon: 'help-circle-outline',
    },
  },
  tutorialLessons: {
    'read-clues': {
      title: 'Read the line clue',
      body: 'A clue of 3 means these three cells must be one filled block.',
      prompt: 'Tap the cells that must be filled.',
      retry: 'Not quite. The 3-cell block still has one forced position.',
      success: 'Exactly. Those cells belong to the only 3-cell block.',
    },
    'forced-fill': {
      title: 'Find the forced fill',
      body: 'This row clue is so large that the center cells must be filled.',
      prompt: 'Fill the forced cells.',
      retry: 'Try the cells that every valid placement shares.',
      success: 'Right. Those middle cells overlap in every placement.',
    },
    'forced-mark': {
      title: 'Mark impossible cells',
      body: 'The clue is already satisfied, so the rest of the line must stay empty.',
      prompt: 'Mark the impossible cells with X.',
      retry: 'These cells cannot be part of any valid block.',
      success: 'Correct. X marks keep the line clean.',
    },
    'combine-lines': {
      title: 'Use both directions',
      body: 'A filled cell in the row narrows the matching column too.',
      prompt: 'Fill the next forced cell.',
      retry: 'Check where the row and column clues agree.',
      success: 'Nice. Cross-checking lines found the next move.',
    },
    'tap-cycle': {
      title: 'Use the tap cycle',
      body: 'Tap once to fill, again to mark with X, and once more to clear.',
      prompt: 'Mark the cell with X.',
      retry: 'This move needs an X, not a filled square.',
      success: 'Perfect. You can switch between fill, X, and clear.',
    },
  },
  analysis: {
    legendEvidence: 'Evidence',
    legendTarget: 'Next move',
    pausedNextMove: {
      title: 'Next move is paused',
      body: 'Turn the helper back on to see one guaranteed logical move.',
    },
    overlapFill: (lineLabel, clueLabel, cellCount) => ({
      title: 'Overlap forces a fill',
      body: `${lineLabel} with clue ${clueLabel} always covers ${cellCount === 1 ? 'this cell' : 'these cells'}, so fill them.`,
    }),
    forcedEmpty: (lineLabel, clueLabel, cellCount) => ({
      title: 'These cells must stay empty',
      body: `${lineLabel} with clue ${clueLabel} cannot place a block on ${cellCount === 1 ? 'this cell' : 'these cells'}, so mark them with X.`,
    }),
    completeLine: (lineLabel, clueLabel) => ({
      title: 'Only one placement fits',
      body: `${lineLabel} with clue ${clueLabel} has a single valid placement from the current board.`,
    }),
    groupedStep: (kind, lineLabel, clueLabel, cellCount) => ({
      title: kind === 'filled' ? 'Apply the forced fills' : 'Mark the forced X cells',
      body: `${lineLabel} with clue ${clueLabel} gives ${cellCount === 1 ? 'one forced cell' : `${cellCount} forced cells`}.`,
    }),
    lineLabel: (axis, index) => `${axis === 'row' ? 'Row' : 'Column'} ${index + 1}`,
    clueLabel: (clues) => clues.length === 1 && clues[0] === 0 ? '0' : clues.join(' '),
  },
};

export default en;
