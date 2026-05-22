const fr = {
  strings: {
    title: 'Takuzu',
    shortTitle: 'Takuzu',
    tagline: 'Remplis la grille avec des 0 et des 1 par la logique.',
    difficultyLabels: {
      easy: 'Facile',
      medium: 'Moyen',
      hard: 'Difficile',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Plus de cases de départ et des déductions plus douces.',
      medium: 'Ouvertures équilibrées qui demandent de lire la grille un peu plus loin.',
      hard: 'Configurations serrées avec moins d\'informations libres et un travail de motifs plus poussé.',
      expert: 'Ouvertures clairsemées avec une pression de déduction soutenue sur tout le puzzle.',
    },
    play: {
      metadataLabels: {
        size: 'Taille',
        difficulty: 'Difficulté',
      },
      helperToggle: {
        show: 'Afficher le prochain coup',
        hide: 'Masquer le prochain coup',
      },
      noPuzzlesDialog: {
        title: 'Aucun puzzle disponible',
        message: (difficultyLabel: string) => `Aucun puzzle trouvé dans le catalogue ${difficultyLabel}.`,
      },
      cellLabel: 'Cellule',
      tutorial: {
        progressLabel: (step: number) => `Leçon ${step}`,
        introNote: 'Objectif : remplis toute la grille de façon que chaque ligne et colonne reste équilibrée, unique et sans triplets. En jeu réel, appuie sur la case mise en évidence pour cycler entre vide, 0, 1 puis vide à nouveau. Dans ce tutoriel, utilise les boutons 0 et 1 ci-dessous.',
        exitLabel: {
          end: 'Terminer le tutoriel',
          skip: 'Passer le tutoriel',
        },
        status: {
          finishing: 'Tutoriel en cours de fin…',
          nextLesson: 'Prochaine leçon en cours…',
          nextStep: 'Prochaine étape en cours…',
        },
        selectAnswerLabel: (value: 0 | 1) => `Sélectionner ${value}`,
      },
    },
  },
  howToPlayRules: [
    {
      num: '1',
      title: 'Remplir chaque case',
      body: 'Appuie sur une case pour cycler : vide → 0 → 1 → vide. Remplis toute la grille.',
    },
    {
      num: '2',
      title: 'Pas trois à la suite',
      body: 'Évite de placer trois chiffres identiques côte à côte dans une ligne ou une colonne.',
    },
    {
      num: '3',
      title: 'Moitiés égales',
      body: 'Chaque ligne et chaque colonne doit contenir exactement le même nombre de 0 et de 1.',
    },
    {
      num: '4',
      title: 'Toutes les lignes sont uniques',
      body: 'Deux lignes ne peuvent pas être identiques, et deux colonnes non plus.',
    },
  ],
  howToPlayTips: [
    {
      key: 'find-pairs',
      title: 'Trouver les paires',
      body: 'Deux chiffres identiques adjacents signifient que les cases de chaque côté doivent être le chiffre opposé.',
      example: [[0, 0, 'a1']],
    },
    {
      key: 'avoid-trios',
      title: 'Éviter les triplets',
      body: 'Si le même chiffre apparaît avec une case vide entre eux, cette case centrale doit être le chiffre opposé.',
      example: [[1, 'a0', 1]],
    },
    {
      key: 'complete-lines',
      title: 'Compléter les lignes et colonnes',
      body: 'Une fois le nombre maximal d\'un chiffre atteint dans une ligne, toutes les cases vides restantes doivent être l\'autre chiffre.',
      example: [[0, 1, 0, 1, 0, 'a1']],
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Éliminer par les lignes remplies',
      body: 'Si remplir une ligne ou colonne la rendrait identique à une ligne déjà complète, ces valeurs doivent être échangées.',
      example: [
        [1, 0, 1, 0, 1, 0],
        [1, 0, 'a0', 'a1', 1, 0],
      ],
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Éliminer les combinaisons impossibles',
      body: 'Si la valeur mise en évidence était un, les cases vides restantes forceraient un triplet. Comme ce n\'est pas permis, la valeur mise en évidence doit être zéro.',
      example: [[1, 1, 0, null, null, 'a0']],
    },
    {
      key: 'score-matters',
      title: 'Comment fonctionne le score',
      body: 'Ton score commence à 10 000 et diminue pendant que le chronomètre tourne. Chaque ligne complétée qui ne correspond pas à la solution soustrait 500 points. Les difficultés plus élevées perdent des points plus lentement.',
    },
    {
      key: 'watch-for-flashes',
      title: 'Surveiller les éclairs',
      body: 'Quand tu complètes correctement une ligne ou une colonne, toutes ses cases clignotent brièvement en confirmation.',
    },
  ],
  loss: {
    abandoned: {
      eyebrow: 'Puzzle terminé',
      title: 'Puzzle inachevé',
      body: 'Tu as terminé ce puzzle avant de le résoudre.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle perdu',
      title: 'Puzzle perdu',
      body: 'Ce puzzle s\'est terminé avant de pouvoir être résolu.',
      icon: '⚠️',
    },
  },
  tutorialLessons: {
    'find-pairs': {
      title: 'Leçon 1 : Trouver les paires',
      body: 'Deux chiffres identiques adjacents signifient que les cases de chaque côté doivent être le chiffre opposé.',
      prompt: 'La case mise en évidence doit-elle être un 1 ou un 0 ?',
      retry: 'Pas celle-là. Si la case mise en évidence était 1, la ligne commencerait par trois 1 à la suite.',
      success: 'Correct. Deux 1 sont déjà côte à côte, donc la case mise en évidence doit être 0.',
    },
    'avoid-trios': {
      title: 'Leçon 2 : Éviter les triplets',
      body: 'Si le même chiffre apparaît avec une case vide entre eux, cette case centrale doit être le chiffre opposé.',
      prompt: 'La case mise en évidence doit-elle être un 1 ou un 0 ?',
      retry: 'Pas celle-là. La case mise en évidence se trouve entre deux 1, elle ne peut donc pas être 1 aussi.',
      success: 'Correct. La case centrale doit être 0 pour que la ligne ne forme pas trois 1 à la suite.',
    },
    'complete-lines': {
      title: 'Leçon 3 : Compléter les lignes et colonnes',
      body: 'Une fois le nombre maximal d\'un chiffre atteint dans une ligne, toutes les cases vides restantes doivent être l\'autre chiffre.',
      prompt: 'La case mise en évidence doit-elle être un 1 ou un 0 ?',
      retry: 'Pas celle-là. Cette ligne a déjà tous les 0 qu\'elle peut contenir, donc la case restante doit être 1.',
      success: 'Correct. La ligne a déjà trois 0, donc la case restante doit être 1.',
    },
    'eliminate-filled-lines': {
      title: 'Leçon 4 : Éliminer par les lignes remplies',
      body: 'Si remplir une ligne ou colonne la rendrait identique à une ligne déjà complète, ces valeurs doivent être échangées.',
      prompt: 'La case mise en évidence doit-elle être un 1 ou un 0 ?',
      retry: 'Pas celle-là. Ce choix rendrait la ligne du bas identique à la ligne complète au-dessus.',
      success: 'Correct. Échanger cette valeur garde la ligne du bas différente de la ligne complète.',
    },
    'eliminate-impossible-combinations': {
      title: 'Leçon 5 : Éliminer les combinaisons impossibles',
      body: 'Regarde la case mise en évidence et les cases vides restantes dans la ligne. Utilise le motif pour déterminer quelle valeur y convient.',
      prompt: 'La case mise en évidence doit-elle être un 1 ou un 0 ?',
      retry: 'Pas celle-là. Si la case mise en évidence était 1, les cases vides restantes forceraient un triplet.',
      success: 'Correct. Choisir 0 évite le triplet qu\'un 1 forcerait plus loin dans la ligne.',
    },
  },
  learningCenter: {
    pausedNextMove: {
      title: 'Pas de prochain coup évident',
      body: 'Cette partie du puzzle n\'offre pas de prochain coup fort pour l\'instant. Essaie une autre ligne ou colonne, puis redemande.',
    },
    findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Prochain coup dans ${lineLabel}`,
        body: `Place ${targetValue} dans la ${cellLabel} mise en évidence. Pourquoi : deux ${repeatedValue} sont déjà côte à côte dans ${lineLabel}, donc un autre ${repeatedValue} créerait trois à la suite.`,
      };
    },
    avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Prochain coup dans ${lineLabel}`,
        body: `Place ${targetValue} dans la case mise en évidence. Pourquoi : ${lineLabel} montre déjà ${repeatedValue} _ ${repeatedValue}, donc la case ouverte entre eux doit être ${targetValue} pour éviter trois à la suite.`,
      };
    },
    completeLines(lineLabel: string, filledValue: 0 | 1, filledCount: number, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Prochain coup dans ${lineLabel}`,
        body: `Place ${targetValue} dans la ${cellLabel} mise en évidence. Pourquoi : ${lineLabel} a déjà ${filledCount} ${filledValue}, donc la ${cellLabel} ouverte restante doit être ${targetValue} pour garder la ligne équilibrée.`,
      };
    },
    eliminateFilledLines(lineLabel: string, matchingLineLabel: string, targetValue: 0 | 1, cellLabel: string, lineKindLabel: string) {
      return {
        title: `Prochain coup dans ${lineLabel}`,
        body: `Place ${targetValue} dans la ${cellLabel} mise en évidence. Pourquoi : si ${lineLabel} correspondait à ${matchingLineLabel}, les ${lineKindLabel} complètes ne seraient plus uniques.`,
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
          ? 'une contradiction de trio'
          : contradictionKind === 'balance'
            ? 'une contradiction d’équilibre'
            : 'une contradiction de ligne complète dupliquée';

      return {
        title: `Prochain coup dans ${lineLabel}`,
        body: `Place ${targetValue} dans la ${cellLabel} mise en évidence. Pourquoi : si cette ${cellLabel} valait ${blockedValue}, suivre ${proofRuleLabel} provoquerait ${contradictionLabel} dans ${contradictionLineLabel}, donc ${targetValue} est forcé ici. ${lineLabel} a encore ${validCompletionCount} complétion${validCompletionCount === 1 ? '' : 's'} de ligne valide${validCompletionCount === 1 ? '' : 's'} à comparer, mais seule cette valeur évite cette contradiction.`,
      };
    },
    avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1) {
      return {
        title: `Prochain coup pour réparer ${lineLabel}`,
        body: `Change une case mise en évidence dans ${lineLabel}. Pourquoi : trois ${repeatedValue} à la suite enfreignent la règle sans triplets.`,
      };
    },
    completeLinesRepair(lineLabel: string, filledValue: 0 | 1, filledCount: number, limit: number) {
      return {
        title: `Prochain coup pour rééquilibrer ${lineLabel}`,
        body: `Change une case mise en évidence dans ${lineLabel}. Pourquoi : ${lineLabel} contient déjà ${filledCount} ${filledValue}, mais la limite est ${limit}.`,
      };
    },
    eliminateFilledLinesRepair(firstLineLabel: string, secondLineLabel: string, lineLabel: string) {
      return {
        title: `Prochain coup pour séparer les ${lineLabel} identiques`,
        body: `Change une case mise en évidence. Pourquoi : ${firstLineLabel} et ${secondLineLabel} sont identiques, mais les ${lineLabel} complètes doivent rester uniques.`,
      };
    },
  },
} as const;

export default fr;
