const en = {
  strings: {
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
        introNote: 'Goal: fill the whole grid so each row and column stays balanced, unique, and free of triples. In live play, tap the highlighted cell to cycle empty, 0, 1, then empty again. In this tutorial, use the 0 and 1 buttons below.',
        exitLabel: {
          end: 'End tutorial',
          skip: 'Skip tutorial',
        },
        status: {
          finishing: 'Tutorial finishing…',
          nextLesson: 'Next lesson starting…',
          nextStep: 'Next step starting…',
        },
        selectAnswerLabel: (value: 0 | 1) => `Select ${value}`,
      },
    },
  },
  howToPlayGoal: 'Fill every cell with a 0 or 1 following three rules.',
  howToPlayControls: 'Tap any cell to cycle through empty → 0 → 1 → empty.',
  howToPlayWrongMove: 'Completing a row or column that breaks a rule flashes it and costs 500 points.',
  howToPlayRules: [
    {
      num: '1',
      title: 'No three in a row',
      body: 'Three identical digits in a row or column are not allowed.',
    },
    {
      num: '2',
      title: 'Equal halves',
      body: 'Every row and column must contain the same number of 0s and 1s.',
    },
    {
      num: '3',
      title: 'All lines are unique',
      body: 'Every row is different from every other row, and every column from every other column.',
    },
  ],
  howToPlayTechniques: [
    {
      key: 'find-pairs',
      title: 'Find pairs',
      body: 'Two matching digits next to each other force the cells on both sides to be the opposite digit.',
    },
    {
      key: 'avoid-trios',
      title: 'Avoid trios',
      body: 'When the same digit appears on both sides of an empty cell, that empty cell must be the opposite.',
    },
    {
      key: 'complete-lines',
      title: 'Complete rows and columns',
      body: 'Once a row or column has as many 0s (or 1s) as it can hold, all remaining cells must be the other digit.',
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Eliminate based on filled lines',
      body: 'A row or column cannot duplicate a completed one. If it would, use the opposite value.',
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Eliminate impossible combinations',
      body: 'If one value would eventually force three in a row, the other value must go here.',
    },
  ],
  howToPlayScoring: 'Starts at 10,000 and drops while the timer runs. Each incorrectly completed line costs 500 points. Score drops more slowly on harder difficulties.',
  howToPlayTips: [
    {
      key: 'watch-for-flashes',
      title: 'Watch for flashes',
      body: 'When you correctly complete a row or column, all its cells briefly flash as confirmation.',
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
  tutorialLessons: {
    'find-pairs': {
      title: 'Lesson 1: Find pairs',
      body: 'Two adjacent identical digits mean the cells on either side must be the opposite digit.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. If the highlighted cell were 1, the row would start with three 1s in a row.',
      success: 'Correct. Two 1s already sit together, so the highlighted cell should be 0.',
    },
    'avoid-trios': {
      title: 'Lesson 2: Avoid trios',
      body: 'If the same digit appears with one empty cell between them, that middle cell must be the opposite.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. The highlighted cell sits between two 1s, so it cannot also be 1.',
      success: 'Correct. The middle cell should be 0 so the row does not form three 1s in a row.',
    },
    'complete-lines': {
      title: 'Lesson 3: Complete rows and columns',
      body: 'Once the maximum count of one digit is reached in a line, all remaining empty cells must be the other digit.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. This row already has all the 0s it can hold, so the remaining cell should be 1.',
      success: 'Correct. The row already has three 0s, so the remaining cell should be 1.',
    },
    'eliminate-filled-lines': {
      title: 'Lesson 4: Eliminate based on filled lines',
      body: 'If filling a row or column would make it identical to an already-complete one, those values must be swapped.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. That choice would make the lower row match the completed row above it.',
      success: 'Correct. Swapping this value keeps the lower row different from the completed row.',
    },
    'eliminate-impossible-combinations': {
      title: 'Lesson 5: Eliminate impossible combinations',
      body: 'Look at the highlighted cell and the remaining blanks in the row. Use the pattern to work out which value fits there.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. If the highlighted cell were 1, the remaining blanks would force a trio.',
      success: 'Correct. Choosing 0 avoids the trio that a 1 would force later in the row.',
    },
  },
  learningCenter: {
    pausedNextMove: {
      title: 'No clear next move yet',
      body: 'This part of the puzzle does not offer a strong next move right now. Try another row or column, then ask again.',
    },
    findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Next move in ${lineLabel}`,
        body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: two ${repeatedValue}s already sit together in ${lineLabel}, so another ${repeatedValue} would create three in a row.`,
      };
    },
    avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Next move in ${lineLabel}`,
        body: `Place ${targetValue} in the highlighted cell. Why: ${lineLabel} already shows ${repeatedValue} _ ${repeatedValue}, so the open cell between them must be ${targetValue} to avoid three in a row.`,
      };
    },
    completeLines(lineLabel: string, filledValue: 0 | 1, filledCount: number, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Next move in ${lineLabel}`,
        body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: ${lineLabel} already has ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, so the remaining open ${cellLabel} must be ${targetValue} to keep the line balanced.`,
      };
    },
    eliminateFilledLines(lineLabel: string, matchingLineLabel: string, targetValue: 0 | 1, cellLabel: string, lineKindLabel: string) {
      return {
        title: `Next move in ${lineLabel}`,
        body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: if ${lineLabel} matched ${matchingLineLabel}, the completed ${lineKindLabel} would stop being unique.`,
      };
    },
    eliminateImpossible(
      lineLabel: string,
      validCompletionCount: number,
      blockedValue: 0 | 1,
      targetValue: 0 | 1,
      cellLabel: string,
      contradictionKind: 'triple' | 'balance' | 'duplicate-line',
      contradictionLineLabel: string,
      proofRuleLabel: string,
    ) {
      const contradictionLabel =
        contradictionKind === 'triple'
          ? 'a three-in-a-row contradiction'
          : contradictionKind === 'balance'
            ? 'a balance contradiction'
            : 'a duplicate completed line contradiction';

      return {
        title: `Next move in ${lineLabel}`,
        body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: if this ${cellLabel} were ${blockedValue}, following ${proofRuleLabel} would force ${contradictionLabel} in ${contradictionLineLabel}, so ${targetValue} is forced here. ${lineLabel} still has ${validCompletionCount} valid line completion${validCompletionCount === 1 ? '' : 's'} to compare, but only this value avoids that contradiction.`,
      };
    },
    avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1) {
      return {
        title: `Next move to repair ${lineLabel}`,
        body: `Change one highlighted cell in ${lineLabel}. Why: three ${repeatedValue}s in a row break the no-trios rule.`,
      };
    },
    completeLinesRepair(lineLabel: string, filledValue: 0 | 1, filledCount: number, limit: number) {
      return {
        title: `Next move to rebalance ${lineLabel}`,
        body: `Change one highlighted cell in ${lineLabel}. Why: ${lineLabel} already contains ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, but limit is ${limit}.`,
      };
    },
    eliminateFilledLinesRepair(firstLineLabel: string, secondLineLabel: string, lineLabel: string) {
      return {
        title: `Next move to separate matching ${lineLabel}`,
        body: `Change one highlighted cell. Why: ${firstLineLabel} and ${secondLineLabel} match, but completed ${lineLabel} must stay unique.`,
      };
    },
    lineLabel(lineKind: 'row' | 'column', index: number) {
      return `${lineKind} ${index + 1}`;
    },
    cellLabel(count: number) {
      return count === 1 ? 'cell' : 'cells';
    },
    lineKindLabel(lineKind: 'row' | 'column', count: number) {
      return count === 1 ? lineKind : `${lineKind}s`;
    },
    ruleLabel(rule: 'find-pairs' | 'avoid-trios' | 'complete-lines' | 'eliminate-filled-lines' | null) {
      switch (rule) {
        case 'find-pairs': return 'the find pairs rule';
        case 'avoid-trios': return 'the avoid trios rule';
        case 'complete-lines': return 'the complete lines rule';
        case 'eliminate-filled-lines': return 'the eliminate filled lines rule';
        default: return 'the rules';
      }
    },
  },
} as const;

export default en;
