const en = {
  strings: {
    title: 'Sudoku',
    shortTitle: 'Sudoku',
    tagline: 'Place digits 1 through 9 so every row, column, and box stays valid.',
    difficultyLabels: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Starter grids meant for singles-first human solving.',
      medium: 'Balanced grids that can grow into note-aware technique checks.',
      hard: 'Sparse grids reserved for richer move analysis and validation work.',
      expert: 'Very sparse grids reserved for advanced chain and coloring deductions.',
    },
    play: {
      metadataLabels: {
        size: 'Size',
        difficulty: 'Difficulty',
        filled: 'Filled',
      },
      noPuzzlesDialog: {
        title: 'Sudoku unavailable',
        message: (difficultyLabel: string) => `No Sudoku puzzle is ready for ${difficultyLabel} yet.`,
      },
      cellLabel: 'Cell',
      resetZoom: 'Reset zoom',
      helperToggle: {
        show: 'Show next move',
        hide: 'Hide next move',
      },
      controls: {
        noteModeEnabled: 'Notes mode on',
        noteModeDisabled: 'Notes mode off',
        selectedCellPrompt: 'Select a cell to enter a digit or note.',
        selectedCellLabel: (cellLabel: string) => `Selected: ${cellLabel}`,
        digitButtonLabel: (digit: number) => `Use digit ${digit} in the selected cell`,
        noteDigitLabel: (digit: number) => `Toggle note ${digit} in the selected cell`,
      },
      nextMove: {
        invalidConflictTitle: 'Board needs a fix first',
        invalidConflictBody: (houseLabel: string, digit: number) => (
          `Digit ${digit} appears more than once in ${houseLabel}. Correct that conflict before asking for the next move.`
        ),
        invalidDeadCellTitle: 'Board needs a fix first',
        invalidDeadCellBody: (cellLabel: string) => (
          `${cellLabel} has no valid digit left. Correct the surrounding entries before asking for the next move.`
        ),
        placementTitle: (techniqueLabel: string, digit: number) => `${techniqueLabel}: place ${digit}`,
        nakedSingleBody: (digit: number, cellLabel: string) => (
          `Only digit ${digit} fits ${cellLabel} after checking its row, column, and box.`
        ),
        hiddenSingleBody: (digit: number, houseLabel: string, cellLabel: string) => (
          `Digit ${digit} only fits one cell in ${houseLabel}, so place it in ${cellLabel}.`
        ),
        placementBody: (techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string) => (
          `${techniqueLabel} forces digit ${digit} into ${cellLabel}. Use the highlighted ${houseLabels} to confirm it.`
        ),
        eliminationTitle: (techniqueLabel: string, digitsLabel: string) => `${techniqueLabel}: clear ${digitsLabel}`,
        lockedCandidatesBody: (
          digitsLabel: string,
          sourceHouseLabel: string,
          targetHouseLabel: string,
        ) => (
          `Digits ${digitsLabel} are locked inside ${sourceHouseLabel}, so remove them from the other highlighted cells in ${targetHouseLabel}.`
        ),
        eliminationBody: (
          techniqueLabel: string,
          digitsLabel: string,
          targetLabels: string,
          houseLabels: string,
        ) => (
          `${techniqueLabel} removes ${digitsLabel} from ${targetLabels}. The highlighted ${houseLabels} leave those candidates no valid home there.`
        ),
        unsupportedTitle: 'No supported next move yet',
        unsupportedBody: 'This position may need a deeper technique than Sudoku currently teaches in Quiet Grid.',
      },
    },
    tutorial: {
      exitLabel: {
        skip: 'Skip tutorial',
        end: 'Open Sudoku',
      },
      controlLabel: 'Live play',
      progressLabel: (current: number, total: number) => `Lesson ${current} of ${total}`,
      status: {
        nextLesson: 'Nice. Moving to the next lesson…',
        finishing: 'Nice. Opening Sudoku…',
      },
      lessons: {
        goal: {
          title: 'Fill every row, column, and box',
          body: 'Sudoku is solved when each row, each column, and each 3×3 box uses digits 1 through 9 exactly once. Given digits stay fixed.',
          summary: 'Start every scan by checking one house at a time: row, column, or box.',
          controlHint: 'Live play: tap a cell, then use the toolbar digits. Tap the same digit again to clear it.',
          continueLabel: 'Show the first move',
        },
        'naked-single': {
          title: 'A naked single is ready now',
          body: 'The highlighted cell already has only one valid digit left after checking its row, column, and box.',
          summary: 'When a cell has one legal digit, place it immediately.',
          controlHint: 'Live play: stay in digit mode, tap the highlighted cell, then tap digit 4 in the toolbar.',
          prompt: 'Which digit belongs in row 1, column 3?',
          options: {
            '4': '4',
            '8': '8',
          },
          correctOptionKey: '4',
          correctFeedback: 'Correct. Digit 4 is the only candidate left for that cell.',
          wrongFeedback: 'Try again. Check the row, column, and box together before placing a digit.',
        },
        'notes-mode': {
          title: 'Use notes before you guess',
          body: 'This highlighted cell still has more than one legal candidate, so it is not ready for a committed digit yet.',
          summary: 'Notes are first-class: mark candidates before you commit to a value.',
          controlHint: 'Live play: tap the pencil to switch to note mode, then tap a toolbar digit to toggle that note.',
          prompt: 'Which mode should you use for row 6, column 2 right now?',
          options: {
            digit: 'Digits',
            notes: 'Notes',
          },
          correctOptionKey: 'notes',
          correctFeedback: 'Correct. This cell still needs notes before it is ready for a final digit.',
          wrongFeedback: 'Not yet. Digits mode commits a value, but this cell still has multiple valid candidates.',
        },
        'hidden-single': {
          title: 'Notes can reveal a hidden single',
          body: 'The highlighted row shows several candidate notes, but only one cell can still take digit 5.',
          summary: 'A hidden single happens when one candidate appears in only one cell inside a house.',
          controlHint: 'Live play: after spotting the hidden single, switch off the pencil and tap digit 5 in the toolbar.',
          prompt: 'After reading the notes, which digit must go in row 6, column 2?',
          options: {
            '4': '4',
            '5': '5',
          },
          correctOptionKey: '5',
          correctFeedback: 'Correct. Digit 5 appears in notes only once in the highlighted row, so it must go there.',
          wrongFeedback: 'Try again. The row still needs a 5, and this is the only cell that can take it.',
        },
      },
    },
    learning: {
      labels: {
        cell: (row: number, col: number) => `row ${row}, column ${col}`,
        row: (index: number) => `row ${index}`,
        column: (index: number) => `column ${index}`,
        box: (index: number) => `box ${index}`,
        joinList: (items: string[]) => {
          if (items.length <= 1) {
            return items[0] ?? '';
          }
          if (items.length === 2) {
            return `${items[0]} and ${items[1]}`;
          }
          return `${items.slice(0, -1).join(', ')}, and ${items[items.length - 1]}`;
        },
      },
      techniqueLabels: {
        'naked-single': 'Naked single',
        'hidden-single': 'Hidden single',
        'naked-pair': 'Naked pair',
        'hidden-pair': 'Hidden pair',
        'pointing-pair-triple': 'Pointing pair/triple',
        'box-line-reduction': 'Box-line reduction',
        'x-wing': 'X-Wing',
        'swordfish': 'Swordfish',
        'xy-wing': 'XY-Wing',
        'xyz-wing': 'XYZ-Wing',
        coloring: 'Coloring',
        chains: 'Chains',
      },
      analyzer: {
        legend: {
          evidence: 'Evidence',
          place: 'Place digit',
          eliminate: 'Clear note',
        },
      },
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Fill the grid',
        body: 'Place digits 1 through 9 so each row, column, and 3×3 box uses every digit once.',
      },
      {
        num: '2',
        title: 'Respect the givens',
        body: 'The starting givens stay fixed and anchor every valid Sudoku session.',
      },
      {
        num: '3',
        title: 'Use notes when a cell is not ready',
        body: 'Notes are optional support actions, but they help track candidates before you commit to a final digit.',
      },
    ],
    tips: [
      {
        key: 'scan-rows',
        title: 'Scan one house at a time',
        body: 'Pick one row, one column, or one box and ask which digits are still missing. Small, local checks stay easier to trust than broad guesses.',
        example: [
          [5, 3, null],
          [6, 7, 2],
          [1, 9, 8],
        ],
      },
      {
        key: 'notes-first',
        title: 'Notes keep hard cells honest',
        body: 'If a cell still has multiple legal digits, switch to Notes mode instead of guessing. Notes help hidden singles and pair-based deductions stand out later.',
        example: [
          [null, '4·5', 3],
          ['1·4·7', '4·7', 6],
          ['1·7', 8, 2],
        ],
      },
    ],
  },
  loss: {
    abandoned: {
      eyebrow: 'Puzzle ended',
      title: 'Sudoku session ended',
      body: 'You ended this Sudoku session before finishing the grid.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle blocked',
      title: 'Sudoku board became invalid',
      body: 'At least one row, column, or box now conflicts with the Sudoku rules. Correct the conflict before asking for the next move.',
      icon: '⚠️',
    },
  },
} as const;

export default en;
