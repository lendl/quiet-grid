const en = {
  strings: {
    title: 'Chimp Test',
    shortTitle: 'Chimp',
    tagline: 'Tap the numbers in order before they disappear.',
    difficultyLabels: {
      easy: 'Easy',
      medium: 'Medium',
      hard: 'Hard',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: '4×4 grid. Remember up to 7 numbers.',
      medium: '5×5 grid. Remember up to 9 numbers.',
      hard: '6×6 grid. Remember up to 11 numbers.',
      expert: '7×7 grid. Remember up to 13 numbers.',
    },
    play: {
      metadataLabels: {
        round: 'Round',
        difficulty: 'Difficulty',
      },
    },
  },
  howToPlayGoal: 'Tap every number in ascending order before the earlier ones disappear from view.',
  howToPlayControls: 'Tap the next number in sequence. Each correctly tapped number disappears immediately, so you must remember where the remaining ones are.',
  howToPlayWrongMove: 'Tapping any cell that is not the next number in sequence ends the puzzle immediately.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Study the grid',
      body: 'Numbers appear at random positions each round. Scan the whole grid before tapping.',
    },
    {
      num: '2',
      title: 'Tap in order',
      body: 'Always tap 1 first, then 2, 3, and so on. There is no time pressure while the numbers are visible.',
    },
    {
      num: '3',
      title: 'Numbers vanish as you tap',
      body: 'Each correct tap removes that number from the grid. You must recall the positions of the remaining numbers from memory.',
    },
    {
      num: '4',
      title: 'Rounds grow',
      body: 'Each successful round adds one more number. Reach the maximum count to solve the puzzle.',
    },
  ],
  howToPlayTechniques: [],
  howToPlayTips: [
    {
      key: 'scan-first',
      title: 'Scan before you tap',
      body: 'Take a moment to trace a path through all numbers before tapping 1. A quick mental route is faster than searching mid-sequence.',
    },
    {
      key: 'group',
      title: 'Group nearby numbers',
      body: 'Notice clusters of consecutive numbers. Tapping neighbours one after another is faster than jumping across the grid.',
    },
  ],
  howToPlayScoring: 'Score is based on total time across all rounds. Faster tapping earns a higher score.',
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
      body: 'You tapped the wrong cell.',
      icon: '⚠️',
    },
  },
} as const;

export default en;
