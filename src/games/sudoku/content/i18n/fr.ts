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
    rules: [
      {
        num: '1',
        title: 'Remplis la grille',
        body: 'Place les chiffres de 1 à 9 pour que chaque ligne, chaque colonne et chaque boîte 3×3 utilisent chaque chiffre une seule fois.',
      },
      {
        num: '2',
        title: 'Respecte les chiffres donnés',
        body: 'Les chiffres de départ restent fixes et servent d’ancrage à toute session de Sudoku valide.',
      },
      {
        num: '3',
        title: 'Utilise les notes quand une case n’est pas prête',
        body: 'Les notes sont des actions de support facultatives, mais elles aident à suivre les candidats avant de valider un chiffre final.',
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
        body: 'Si une case a encore plusieurs chiffres légaux, passe en mode notes au lieu de deviner. Les notes aident les hidden singles et les déductions par paires à ressortir plus tard.',
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
