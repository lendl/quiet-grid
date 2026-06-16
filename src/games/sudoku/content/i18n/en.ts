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
        explainButton: 'Explain this move',
      },
      techniqueLesson: {
        backButton: 'Back to game',
        explanations: {
          'hidden-single': 'A hidden single means only one cell in a house can legally hold a particular digit, even though that cell might still show other candidates.\n\nScan the highlighted house for the target digit. Every other cell is blocked because that digit already appears somewhere in each cell\'s row, column, or box. That leaves exactly one valid home — so place the digit here.\n\nHidden singles are easier to spot once you write notes: look for a digit that appears in only one cell\'s candidate list within the highlighted house.',
          'naked-pair': 'A naked pair is two cells in the same house that together hold exactly the same two candidates and nothing else. Because those two digits must fill those two cells, no other cell in that house can use either of them.\n\nFind the two highlighted cells. Both carry exactly two candidates and those candidates are identical. Whichever digit lands in one cell, the other takes the second. Every other cell in the highlighted house can therefore lose both of those candidates — those are the eliminations shown.',
          'hidden-pair': 'A hidden pair is two digits that can only go in exactly two cells within the same house, even though those cells appear to carry other candidates too.\n\nLook at the highlighted house. The two target digits appear as candidates in exactly two cells and nowhere else in that house. Since those two cells must claim both digits between them, any other candidates those cells carry are now impossible and can be removed.',
          'pointing-pair-triple': 'A pointing pair or triple occurs when a digit\'s only valid placements within a box are all confined to the same row or column. That alignment means the digit cannot appear anywhere else in that row or column outside the box.\n\nLook at the highlighted box. The target digit can only go in the highlighted row or column inside it. Any cell in that same row or column that sits outside the box can safely lose that digit — those are the eliminations shown.',
          'box-line-reduction': 'Box-line reduction is the reverse of a pointing pair. When a digit\'s only valid placements in a row or column all fall inside a single box, no other cell in that box can hold that digit.\n\nLook at the highlighted row or column. All remaining positions for the target digit fall within one box. Every other cell in that box that still carries the digit as a candidate can safely have it removed.',
          'x-wing': 'An X-Wing forms when a digit appears as a candidate in exactly two cells in each of two rows, and both pairs align in the same two columns.\n\nThose four cells form a rectangle. The digit must be placed in one pair of diagonally opposite corners. Either way, every other cell in those two columns is eliminated. Look at the highlighted rows and columns to see the rectangle and the cells that lose the candidate.',
          'swordfish': 'A Swordfish extends the X-Wing idea to three rows and three columns. A digit appears as a candidate in two or three cells in each of three rows, and all those cells fall within the same three columns.\n\nBecause the digit must be placed exactly once in each of those three rows, and every placement is confined to the same three columns, no other cell in those three columns can hold that digit. The highlighted rows and columns mark the full pattern.',
          'xy-wing': 'An XY-Wing uses three cells that form a chain of two-candidate cells. A pivot cell shares one candidate with each of two wing cells, and the two wing cells share a candidate with each other that the pivot does not carry.\n\nNo matter how the pivot resolves, the shared digit between the two wings must land in one of them. Any cell that both wings can see can therefore safely lose that shared candidate. The highlighted cells show the pivot, the wings, and the targets.',
          'xyz-wing': 'An XYZ-Wing is a tighter version of an XY-Wing where the pivot holds three candidates instead of two. The pivot shares a pair with each wing, and all three cells together restrict where the shared digit can go.\n\nBecause the shared digit must land in one of the three highlighted cells — the pivot or either wing — any cell that all three can see can safely lose that candidate.',
          'coloring': 'Coloring assigns two alternating colors to all occurrences of a single candidate across the board, following conjugate pairs — houses where the digit has exactly two possible cells.\n\nOnce both colors are mapped, any cell that can see two cells of the same color can eliminate the candidate. If one color is correct, two same-color cells in the same house would be a contradiction. The highlighted cells show the chain and the conflict that forces the elimination.',
          'chains': 'A chain is a sequence of logical inferences linking candidates together. Each link is either a strong inference (if one end is false the other must be true) or a weak inference (both cannot be true at once).\n\nFollowing the chain, if a cell can see both endpoints and those endpoints carry the same candidate with a strong link between them, that candidate can be eliminated from the viewing cell. Chains are the most general technique and can resolve positions that no pattern-based rule can handle alone.',
        },
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
    goal: 'Place each digit 1–9 exactly once in every row, column, and 3×3 box.',
    controls: 'Tap a cell to select it, then tap a digit button to place it. Toggle Notes mode to pencil in candidates instead.',
    wrongMove: 'A duplicate digit in the same row, column, or box is highlighted as a conflict.',
    rules: [
      {
        num: '1',
        title: 'Fill the grid',
        body: 'No digit may appear twice in the same row, column, or box.',
      },
      {
        num: '2',
        title: 'Respect the givens',
        body: 'The pre-filled digits are fixed — you cannot change them.',
      },
      {
        num: '3',
        title: 'Use notes when a cell is not ready',
        body: 'Toggle Notes mode to pencil in possible digits for a cell and cross them out as the puzzle narrows down.',
      },
    ],
    techniques: [
      {
        key: 'naked-single',
        title: 'Naked single',
        body: 'When only one digit is allowed in a cell after ruling out every digit already in its row, column, and box, place it.',
      },
      {
        key: 'hidden-single',
        title: 'Hidden single',
        body: 'When a digit can only fit one cell within a row, column, or box, place it there even if other candidates are still visible in that cell.',
      },
      {
        key: 'notes-mode',
        title: 'Notes mode',
        body: 'Write all possible digits as notes, then cross them out as surrounding rows, columns, and boxes fill in — until only one is left.',
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
        body: 'If a cell still has multiple valid digits, switch to Notes mode instead of guessing. Pencilled candidates make hidden singles and other forced placements much easier to spot.',
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
      body: 'You ended this Sudoku session before solving the grid.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle blocked',
      title: 'Sudoku grid became invalid',
      body: 'At least one row, column, or box now conflicts with the Sudoku rules. Correct the conflict before asking for the next move.',
      icon: '⚠️',
    },
  },
} as const;

export default en;
