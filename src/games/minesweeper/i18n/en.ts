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
  analysis: {
    lossSummary({ safeCount, mineCount }: { safeCount: number; mineCount: number }) {
      if (safeCount > 0 && mineCount > 0) {
        return {
          title: 'Logical moves were available',
          body: `This board already had ${safeCount} safe ${safeCount === 1 ? 'tile' : 'tiles'} to reveal and ${mineCount} certain ${mineCount === 1 ? 'mine' : 'mines'} to flag.`,
        };
      }

      if (safeCount > 0) {
        return {
          title: 'Safe reveals were available',
          body: `This board already had ${safeCount} safe ${safeCount === 1 ? 'tile' : 'tiles'} to reveal from the shown clues.`,
        };
      }

      return {
        title: 'Certain flags were available',
        body: `This board already had ${mineCount} certain ${mineCount === 1 ? 'mine' : 'mines'} to flag from the shown clues.`,
      };
    },
    groupedFlagStep({ mineCount }: { mineCount: number }) {
      return {
        title: 'Certain mines on this board',
        body: `You can flag the highlighted ${mineCount === 1 ? 'tile' : 'tiles'} now. Current clues already prove these ${mineCount === 1 ? 'it is a mine' : 'tiles are mines'}.`,
      };
    },
    legendEvidence: 'Evidence',
    legendSafe: 'Safe reveal',
    legendMine: 'Flag mine',
  },
  tutorialText: {
    'goal-and-stakes': {
      title: 'Clear every safe tile',
      body: 'Reveal every tile that does not hide a mine. If you reveal a mine, the run ends immediately.',
      prompt: 'Flags help you track danger, but only safe reveals win the board.',
      summary: 'Win by revealing every safe tile. Lose by opening one mine.',
      continueLabel: 'Continue',
    },
    'core-actions': {
      title: 'You have two actions',
      body: 'In a real puzzle, tap a hidden tile to reveal it. Press and hold a hidden tile to place or remove a flag.',
      prompt: 'In this tutorial you choose Reveal or Flag with buttons below. In a live board, tap reveals and press-hold flags.',
      summary: 'Reveal opens a tile. Press-hold toggles a flag. Flags help you track mines but do not solve the board by themselves.',
      continueLabel: 'Continue',
    },
    'reading-clues': {
      title: 'Numbers count all touching mines',
      body: 'Each number shows how many mines touch that tile in any of the eight neighboring spaces.',
      prompt: 'Diagonal neighbors count too.',
      summary: 'A clue tells you how many mines touch it, not exactly where they are yet.',
      continueLabel: 'Continue',
    },
    'forced-flag': {
      title: 'Flag tile that must hide a mine',
      body: 'This clue still needs one mine, and the highlighted tile is the only hidden place left for it.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'That clue still needs one mine, and no other hidden neighbor can supply it.',
      success: 'Right. The highlighted tile had to be a mine, so Flag is correct.',
    },
    'safe-reveal': {
      title: 'Reveal tile that must be safe',
      body: 'This clue already has its mine, so the highlighted neighbor cannot hide another one.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'The clue is already satisfied by the flagged mine, so the remaining hidden neighbor is safe.',
      success: 'Right. Once the clue already has its mine, the highlighted tile is safe to reveal.',
    },
    'compare-clues': {
      title: 'Compare clues together',
      body: 'These clues share hidden tiles. Once the shared group accounts for the mine count, the extra tile becomes safe.',
      prompt: 'What should you do with the highlighted tile?',
      retry: 'Read both clues together. The shared hidden tiles absorb the mine count, so the extra tile is safe.',
      success: 'Right. Comparing both clues proves the highlighted tile cannot be a mine.',
    },
    'advanced-patterns': {
      title: 'Some patterns use diagonals and overlap',
      body: 'Here the corner 1 counts the flagged diagonal mine, and the 2 and 1 beside it both read the same hidden top strip.',
      prompt: 'A corner clue still counts diagonals, and nearby clues can overlap the same hidden tiles.',
      summary: 'Do not read one clue alone when a diagonal mine or a shared hidden strip changes what nearby clues mean.',
      continueLabel: 'Continue',
    },
    'guess-and-help': {
      title: 'Sometimes logic runs out',
      body: 'This top edge still fits more than one mine layout. The row of 1s does not tell you which hidden tiles are the mines.',
      prompt: 'When that happens, make the calmest guess you can and use help tools when needed.',
      summary: 'Different mine placements can fit the same clues. Hints can help, undo fixes mis-taps, and you can replay this tutorial later.',
      continueLabel: 'Finish',
    },
  },
  learningCenter: {
    nextMovePattern({
      patternKey,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterPatternParams) {
      switch (patternKey) {
        case 'single-mine-logic':
          return {
            title: `Safe next move near ${clueLabel ?? 'this clue'}`,
            body: `Reveal highlighted ${tileLabel}. This local clue pattern still leaves one mine slot, which makes the other hidden ${tileLabel} safe.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Single-Mine Logic',
              explanationTitle: 'Explanation',
              explanation: `This local clue pattern still needs exactly one ${mineLabel}. Once that single mine slot is pinned down, the other touching hidden ${tileLabel} must be safe.`,
            },
          };
        case 'all-mines-accounted-for':
          return {
            title: `Safe next move near ${clueLabel ?? 'this clue'}`,
            body: `Reveal highlighted ${tileLabel}. Around ${clueLabel ?? 'this clue'}, known mine positions already account for all ${mineCount} ${mineLabel}.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'All Mines Accounted For',
              explanationTitle: 'Explanation',
              explanation: `This clue already has all ${mineCount} ${mineLabel} it needs, so every other hidden ${tileLabel} touching it must be safe.`,
            },
          };
        case 'only-one-possible-mine':
          return {
            title: `Safe next move from comparing clues`,
            body: `Reveal highlighted ${tileLabel}. Reading ${clueLabel ?? 'one clue'} together with ${secondaryClueLabel ?? 'another clue'} leaves only one legal place for the remaining mine.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Only One Possible Mine',
              explanationTitle: 'Explanation',
              explanation: `Comparing these clues leaves exactly one legal location for the remaining ${mineLabel}, so the extra hidden ${tileLabel} outside that mine slot must be safe.`,
            },
          };
        case 'guaranteed-safe-tile':
          return {
            title: `Safe next move near ${clueLabel ?? 'this clue'}`,
            body: `Reveal highlighted ${tileLabel}. If this tile were a mine, nearby clue counts would be too high.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Guaranteed Safe Tile',
              explanationTitle: 'Explanation',
              explanation: `If the highlighted ${tileLabel} were a mine, at least one nearby clue would have too many ${mineLabel}. Since that would break the clue, the tile must be safe.`,
            },
          };
        case 'full-clue-resolution':
          return {
            title: `Safe next move near ${clueLabel ?? 'this clue'}`,
            body: `Reveal highlighted ${tileLabel}. This clue's mine requirement is fully resolved, so its remaining hidden ${tileLabel} are safe.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Full Clue Resolution',
              explanationTitle: 'Explanation',
              explanation: `This clue's mine requirement is fully resolved by nearby forced mine positions, so the remaining hidden ${tileLabel} touching it must be safe.`,
            },
          };
        default:
          throw new Error(`Unhandled next move pattern: ${patternKey satisfies never}`);
      }
    },
    flagMovePattern({
      reason,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterMineFlagParams) {
      switch (reason) {
        case 'direct-local':
          return {
            title: `Certain mine near ${clueLabel ?? 'this clue'}`,
            body: `Flag highlighted ${tileLabel}. ${clueLabel ?? 'This clue'} still needs ${mineCount} ${mineLabel}, and the highlighted hidden ${tileLabel} are the only places left for them.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Direct Local Mine',
              explanationTitle: 'Explanation',
              explanation: `This clue still needs ${mineCount} ${mineLabel}. Because only the highlighted hidden ${tileLabel} remain around it, each highlighted tile must be a mine.`,
            },
          };
        case 'subset-difference':
          return {
            title: 'Certain mine from comparing clues',
            body: `Flag highlighted ${tileLabel}. Comparing ${clueLabel ?? 'one clue'} with ${secondaryClueLabel ?? 'another clue'} shows the extra hidden ${tileLabel} must contain the remaining ${mineLabel}.`,
            teaching: {
              patternTitle: 'Pattern',
              patternLabel: 'Subset Mine Difference',
              explanationTitle: 'Explanation',
              explanation: `The smaller clue's hidden tiles fit inside the larger clue's hidden tiles. After accounting for the shared mine slots, the extra hidden ${tileLabel} must contain the remaining ${mineLabel}.`,
            },
          };
        default:
          throw new Error(`Unhandled flag move pattern: ${reason satisfies never}`);
      }
    },
    guess: {
      title: 'No certain next move yet',
      body: 'No clue points to a certain safe reveal right now. This spot may need a guess, so trust your best read of the board and ask again after the next reveal.',
    },
  },
} as const;

export default en;
import type { LearningCenterMineFlagParams, LearningCenterPatternParams } from './index';
