const fr = {
  strings: {
    title: 'Sudoku',
    shortTitle: 'Sudoku',
    tagline: 'Place les chiffres de 1 à 9 pour que chaque ligne, colonne et boîte reste valide.',
    difficultyLabels: {
      easy: 'Facile',
      medium: 'Moyen',
      hard: 'Difficile',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Grilles d’ouverture pensées pour une résolution humaine centrée sur les singles.',
      medium: 'Grilles équilibrées qui peuvent évoluer vers des techniques appuyées par les notes.',
      hard: 'Grilles clairsemées réservées à une analyse de coups et à une validation plus riches.',
      expert: 'Grilles très clairsemées réservées aux déductions avancées par chaînes et coloriage.',
    },
    play: {
      metadataLabels: {
        size: 'Taille',
        difficulty: 'Difficulté',
        filled: 'Rempli',
      },
      noPuzzlesDialog: {
        title: 'Sudoku indisponible',
        message: (difficultyLabel: string) => `Aucun puzzle Sudoku n’est encore prêt pour ${difficultyLabel}.`,
      },
      cellLabel: 'Case',
      resetZoom: 'Réinitialiser le zoom',
      helperToggle: {
        show: 'Afficher le prochain coup',
        hide: 'Masquer le prochain coup',
      },
      controls: {
        noteModeEnabled: 'Mode notes activé',
        noteModeDisabled: 'Mode notes désactivé',
        selectedCellPrompt: 'Sélectionne une case pour entrer un chiffre ou une note.',
        selectedCellLabel: (cellLabel: string) => `Sélectionnée : ${cellLabel}`,
        digitButtonLabel: (digit: number) => `Utiliser le chiffre ${digit} dans la case sélectionnée`,
        noteDigitLabel: (digit: number) => `Basculer la note ${digit} dans la case sélectionnée`,
      },
      nextMove: {
        invalidConflictTitle: 'Le plateau doit d’abord être corrigé',
        invalidConflictBody: (houseLabel: string, digit: number) => (
          `Le chiffre ${digit} apparaît plus d’une fois dans ${houseLabel}. Corrige ce conflit avant de demander le prochain coup.`
        ),
        invalidDeadCellTitle: 'Le plateau doit d’abord être corrigé',
        invalidDeadCellBody: (cellLabel: string) => (
          `${cellLabel} n’a plus aucun chiffre valide possible. Corrige les entrées autour avant de demander le prochain coup.`
        ),
        placementTitle: (techniqueLabel: string, digit: number) => `${techniqueLabel} : placer ${digit}`,
        nakedSingleBody: (digit: number, cellLabel: string) => (
          `Seul le chiffre ${digit} convient à ${cellLabel} après vérification de sa ligne, de sa colonne et de sa boîte.`
        ),
        hiddenSingleBody: (digit: number, houseLabel: string, cellLabel: string) => (
          `Le chiffre ${digit} ne peut aller que dans une seule case de ${houseLabel}, donc place-le dans ${cellLabel}.`
        ),
        placementBody: (techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string) => (
          `${techniqueLabel} force le chiffre ${digit} dans ${cellLabel}. Utilise les ${houseLabels} mises en évidence pour le confirmer.`
        ),
        eliminationTitle: (techniqueLabel: string, digitsLabel: string) => `${techniqueLabel} : retirer ${digitsLabel}`,
        lockedCandidatesBody: (
          digitsLabel: string,
          sourceHouseLabel: string,
          targetHouseLabel: string,
        ) => (
          `Les chiffres ${digitsLabel} sont verrouillés dans ${sourceHouseLabel}, retire-les donc des autres cases mises en évidence dans ${targetHouseLabel}.`
        ),
        eliminationBody: (
          techniqueLabel: string,
          digitsLabel: string,
          targetLabels: string,
          houseLabels: string,
        ) => (
          `${techniqueLabel} retire ${digitsLabel} de ${targetLabels}. Les ${houseLabels} mises en évidence ne laissent plus de place valide à ces candidats ici.`
        ),
        unsupportedTitle: 'Pas encore de prochain coup pris en charge',
        unsupportedBody: 'Cette position peut demander une technique plus profonde que celles que Sudoku enseigne actuellement dans Quiet Grid.',
        explainButton: 'Expliquer ce coup',
      },
      techniqueLesson: {
        backButton: 'Retour au jeu',
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
        skip: 'Passer le tutoriel',
        end: 'Ouvrir Sudoku',
      },
      controlLabel: 'Jeu en direct',
      progressLabel: (current: number, total: number) => `Leçon ${current} sur ${total}`,
      status: {
        nextLesson: 'Bien. Passage à la leçon suivante…',
        finishing: 'Bien. Ouverture de Sudoku…',
      },
      lessons: {
        goal: {
          title: 'Remplis chaque ligne, colonne et boîte',
          body: 'Le Sudoku est résolu quand chaque ligne, chaque colonne et chaque boîte 3×3 utilisent les chiffres de 1 à 9 exactement une fois. Les chiffres donnés restent fixes.',
          summary: 'Commence chaque lecture par une seule maison à la fois : ligne, colonne ou boîte.',
          controlHint: 'Jeu en direct : touche une case, puis utilise les chiffres de la barre d’outils. Touche à nouveau le même chiffre pour l’effacer.',
          continueLabel: 'Afficher le premier coup',
        },
        'naked-single': {
          title: 'Un naked single est prêt maintenant',
          body: 'La case mise en évidence n’a déjà plus qu’un seul chiffre valide après vérification de sa ligne, de sa colonne et de sa boîte.',
          summary: 'Quand une case n’a qu’un seul chiffre légal, place-le immédiatement.',
          controlHint: 'Jeu en direct : reste en mode chiffres, touche la case mise en évidence, puis le chiffre 4 dans la barre d’outils.',
          prompt: 'Quel chiffre appartient à la ligne 1, colonne 3 ?',
          options: {
            '4': '4',
            '8': '8',
          },
          correctOptionKey: '4',
          correctFeedback: 'Correct. Le chiffre 4 est le seul candidat qui reste pour cette case.',
          wrongFeedback: 'Réessaie. Vérifie la ligne, la colonne et la boîte ensemble avant de placer un chiffre.',
        },
        'notes-mode': {
          title: 'Utilise les notes avant de deviner',
          body: 'Cette case mise en évidence a encore plus d’un candidat valide, elle n’est donc pas encore prête pour un chiffre définitif.',
          summary: 'Les notes sont un vrai outil de support : marque les candidats avant de valider une valeur.',
          controlHint: 'Jeu en direct : touche le crayon pour passer en mode notes, puis touche un chiffre de la barre pour activer ou retirer cette note.',
          prompt: 'Quel mode dois-tu utiliser maintenant pour la ligne 6, colonne 2 ?',
          options: {
            digit: 'Chiffres',
            notes: 'Notes',
          },
          correctOptionKey: 'notes',
          correctFeedback: 'Correct. Cette case a d’abord besoin de notes avant d’être prête pour un chiffre final.',
          wrongFeedback: 'Pas encore. Le mode chiffres valide une valeur, mais cette case a encore plusieurs candidats valides.',
        },
        'hidden-single': {
          title: 'Les notes peuvent révéler un hidden single',
          body: 'La ligne mise en évidence montre plusieurs notes candidates, mais une seule case peut encore accueillir le chiffre 5.',
          summary: 'Un hidden single apparaît quand un candidat n’existe plus que dans une seule case d’une maison.',
          controlHint: 'Jeu en direct : après avoir repéré le hidden single, coupe le crayon puis touche le chiffre 5 dans la barre d’outils.',
          prompt: 'Après avoir lu les notes, quel chiffre doit aller en ligne 6, colonne 2 ?',
          options: {
            '4': '4',
            '5': '5',
          },
          correctOptionKey: '5',
          correctFeedback: 'Correct. Le chiffre 5 n’apparaît qu’une seule fois dans les notes de la ligne mise en évidence, il doit donc aller là.',
          wrongFeedback: 'Réessaie. La ligne a encore besoin d’un 5, et c’est la seule case qui peut le prendre.',
        },
      },
    },
    learning: {
      labels: {
        cell: (row: number, col: number) => `ligne ${row}, colonne ${col}`,
        row: (index: number) => `ligne ${index}`,
        column: (index: number) => `colonne ${index}`,
        box: (index: number) => `boîte ${index}`,
        joinList: (items: string[]) => {
          if (items.length <= 1) {
            return items[0] ?? '';
          }
          if (items.length === 2) {
            return `${items[0]} et ${items[1]}`;
          }
          return `${items.slice(0, -1).join(', ')} et ${items[items.length - 1]}`;
        },
      },
      techniqueLabels: {
        'naked-single': 'Naked single',
        'hidden-single': 'Hidden single',
        'naked-pair': 'Naked pair',
        'hidden-pair': 'Hidden pair',
        'pointing-pair-triple': 'Pointing pair/triple',
        'box-line-reduction': 'Réduction boîte-ligne',
        'x-wing': 'X-Wing',
        'swordfish': 'Swordfish',
        'xy-wing': 'XY-Wing',
        'xyz-wing': 'XYZ-Wing',
        coloring: 'Coloriage',
        chains: 'Chaînes',
      },
      analyzer: {
        legend: {
          evidence: 'Preuve',
          place: 'Placer le chiffre',
          eliminate: 'Retirer la note',
        },
      },
    },
  },
  howToPlay: {
    goal: "Place chaque chiffre de 1 à 9 exactement une fois dans chaque ligne, colonne et boîte 3×3.",
    controls: "Appuie sur une case pour la sélectionner, puis sur un chiffre pour le placer. Active le mode notes pour saisir des candidats à la place.",
    wrongMove: "Un chiffre en double dans la même ligne, colonne ou boîte est mis en évidence comme conflit.",
    rules: [
      {
        num: "1",
        title: "Remplis la grille",
        body: "Aucun chiffre ne peut apparaître deux fois dans la même ligne, colonne ou boîte.",
      },
      {
        num: "2",
        title: "Respecte les chiffres donnés",
        body: "Les chiffres pré-remplis sont fixes — tu ne peux pas les modifier.",
      },
      {
        num: "3",
        title: "Utilise les notes quand une case n’est pas prête",
        body: "Active le mode notes pour noter les chiffres possibles d’une case et les barrer au fil que le puzzle se resserre.",
      },
    ],
    techniques: [
      {
        key: "naked-single",
        title: "Single nu",
        body: "Quand un seul chiffre est autorisé dans une case après avoir exclu tous ceux déjà présents dans sa ligne, colonne et boîte, place-le.",
      },
      {
        key: "hidden-single",
        title: "Single caché",
        body: "Quand un chiffre ne peut aller que dans une seule case d’une ligne, colonne ou boîte, place-le là même si d’autres candidats sont encore visibles.",
      },
      {
        key: "notes-mode",
        title: "Mode notes",
        body: "Écris tous les chiffres possibles en notes, puis barre-les au fur et à mesure que les lignes, colonnes et boîtes environnantes se remplissent — jusqu’à ce qu’il n’en reste qu’un.",
      },
    ],
    tips: [
      {
        key: 'scan-rows',
        title: 'Inspecte une seule maison à la fois',
        body: 'Choisis une ligne, une colonne ou une boîte et demande-toi quels chiffres manquent encore. Les vérifications petites et locales restent plus fiables que les suppositions larges.',
        example: [
          [5, 3, null],
          [6, 7, 2],
          [1, 9, 8],
        ],
      },
      {
        key: 'notes-first',
        title: 'Les notes gardent les cases difficiles honnêtes',
        body: 'Si une case a encore plusieurs chiffres valides, passe en mode notes au lieu de deviner. Les candidats notés rendent les hidden singles et les autres placements forcés bien plus faciles à repérer.',
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
      eyebrow: 'Puzzle terminé',
      title: 'Session Sudoku terminée',
      body: 'Tu as arrêté cette session de Sudoku avant de finir la grille.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle bloqué',
      title: 'La grille de Sudoku est devenue invalide',
      body: 'Au moins une ligne, une colonne ou une boîte est maintenant en conflit avec les règles du Sudoku. Corrige le conflit avant de demander le prochain coup.',
      icon: '⚠️',
    },
  },
} as const;

export default fr;
