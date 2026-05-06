const en = {
  strings: {
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
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Reveal safe cells',
        body: 'Open cells that you believe are safe. A revealed mine ends the active puzzle.',
      },
      {
        num: '2',
        title: 'Read the numbers',
        body: 'Each revealed number shows how many mines touch that cell, including diagonals.',
      },
      {
        num: '3',
        title: 'Flag likely mines',
        body: 'Long-press a hidden cell to place or remove a flag when you are confident a mine is there.',
      },
      {
        num: '4',
        title: 'Clear every safe cell',
        body: 'The puzzle is solved when every non-mine cell is revealed.',
      },
    ],
    tips: [
      {
        key: 'start-from-openings',
        title: 'Start from openings',
        body: 'Large empty openings reveal several safe cells at once and often expose the first strong clues.',
      },
      {
        key: 'count-neighbors',
        title: 'Count shared neighbors',
        body: 'When two revealed numbers touch some of the same hidden cells, compare their remaining mine counts before placing flags.',
      },
      {
        key: 'Use flags carefully',
        title: 'Use flags carefully',
        body: 'Flags help you track likely mines, but they do not prove a cell is dangerous unless the surrounding clues support it.',
      },
      {
        key: 'pace-matters',
        title: 'How scoring works',
        body: 'Your score starts at 10,000 and drops while the puzzle timer runs. Faster safe solves keep more score.',
      },
    ],
  },
  loss: {
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
  },
  tutorialText: {
    'forced-flag': {
      title: 'Flag tile that must hide a mine',
      body: 'That 1 still needs one mine, and the highlighted tile is its only hidden neighbor.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'Look at the 1 beside the highlighted tile. It still needs one mine, and no other hidden neighbor can supply it.',
      success: 'Right. That clue still needed one mine, so the highlighted tile had to be flagged.',
    },
    'safe-reveal': {
      title: 'Reveal tile that must be safe',
      body: 'This 1 already touches its flagged mine, so the highlighted tile cannot hide another one.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'That clue is already satisfied by the flagged mine. The highlighted tile is the remaining hidden neighbor, so it is safe.',
      success: 'Right. Once that clue already has its mine, the highlighted tile can be revealed safely.',
    },
    'diagonals-count': {
      title: 'Diagonal neighbors count too',
      body: 'The visible clues pin down the flagged mine first. After that, the corner 1 matters because it counts diagonal neighbors too.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'The visible clues already force the flagged tile to be a mine. Once you include that diagonal mine in the corner 1, the highlighted tile is safe.',
      success: 'Right. The flagged tile is a known mine, and the corner clue counts it diagonally, so the highlighted tile can be revealed.',
    },
    'compare-clues': {
      title: 'Compare two clues together',
      body: 'These two 1 clues share hidden tiles. Once the shared pair takes one mine, the extra tile by the right 1 must be safe.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'Read both 1 clues together. The shared hidden pair can contain only one mine, so the extra tile by the right clue is safe.',
      success: 'Right. Comparing both clues shows the highlighted tile cannot hide a mine.',
    },
    'guess-moments': {
      title: 'Sometimes next move is a guess',
      body: 'Not every puzzle offers a fully proved next move. On this board, the hidden top edge still supports more than one possible mine layout.',
      prompt: 'When clues do not prove one move, make the calmest guess you can.',
      summary: 'More than one mine pattern can still fit the hidden top edge, so no single move is proved there yet.',
      continueLabel: 'Continue',
    },
  },
  learningCenter: {
    safeReveal(clueLabel: string, tileLabel: string, mineCount: number, mineLabel: string) {
      return {
        title: `Safe next move near ${clueLabel}`,
        body: `Reveal highlighted ${tileLabel}. Why: around ${clueLabel}, shaded tiles already account for all ${mineCount} ${mineLabel}, so remaining hidden ${tileLabel} are safe.`,
      };
    },
    guess: {
      title: 'No certain next move yet',
      body: 'No clue points to a certain safe reveal right now. This spot may need a guess, so trust your best read of the board and ask again after the next reveal.',
    },
  },
} as const;

export default en;
