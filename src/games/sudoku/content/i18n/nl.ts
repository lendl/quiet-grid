const nl = {
  strings: {
    title: 'Sudoku',
    shortTitle: 'Sudoku',
    tagline: 'Plaats de cijfers 1 tot en met 9 zodat elke rij, kolom en box geldig blijft.',
    difficultyLabels: {
      easy: 'Makkelijk',
      medium: 'Gemiddeld',
      hard: 'Moeilijk',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Startroosters bedoeld voor logisch oplossen met eerst singles.',
      medium: 'Gebalanceerde roosters die kunnen uitgroeien tot technieken met notities.',
      hard: 'Schaarse roosters bedoeld voor rijkere zetanalyse en validatie.',
      expert: 'Zeer schaarse roosters bedoeld voor gevorderde ketting- en kleurdeducties.',
    },
    play: {
      metadataLabels: {
        size: 'Grootte',
        difficulty: 'Niveau',
        filled: 'Ingevuld',
      },
      noPuzzlesDialog: {
        title: 'Sudoku niet beschikbaar',
        message: (difficultyLabel: string) => `Er is nog geen Sudoku-puzzel klaar voor ${difficultyLabel}.`,
      },
      cellLabel: 'Cel',
      resetZoom: 'Zoom resetten',
      helperToggle: {
        show: 'Toon volgende zet',
        hide: 'Verberg volgende zet',
      },
      controls: {
        noteModeEnabled: 'Notitiemodus aan',
        noteModeDisabled: 'Notitiemodus uit',
        selectedCellPrompt: 'Selecteer een cel om een cijfer of notitie in te voeren.',
        selectedCellLabel: (cellLabel: string) => `Geselecteerd: ${cellLabel}`,
        digitButtonLabel: (digit: number) => `Gebruik cijfer ${digit} in de geselecteerde cel`,
        noteDigitLabel: (digit: number) => `Schakel notitie ${digit} in de geselecteerde cel`,
      },
      nextMove: {
        invalidConflictTitle: 'Bord moet eerst worden hersteld',
        invalidConflictBody: (houseLabel: string, digit: number) => (
          `Cijfer ${digit} staat meer dan één keer in ${houseLabel}. Los dat conflict op voordat je naar de volgende zet vraagt.`
        ),
        invalidDeadCellTitle: 'Bord moet eerst worden hersteld',
        invalidDeadCellBody: (cellLabel: string) => (
          `${cellLabel} heeft geen geldig cijfer meer over. Corrigeer de omliggende invoer voordat je naar de volgende zet vraagt.`
        ),
        placementTitle: (techniqueLabel: string, digit: number) => `${techniqueLabel}: plaats ${digit}`,
        nakedSingleBody: (digit: number, cellLabel: string) => (
          `Alleen cijfer ${digit} past in ${cellLabel} na controle van rij, kolom en box.`
        ),
        hiddenSingleBody: (digit: number, houseLabel: string, cellLabel: string) => (
          `Cijfer ${digit} past maar in één cel in ${houseLabel}, dus plaats het in ${cellLabel}.`
        ),
        placementBody: (techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string) => (
          `${techniqueLabel} dwingt cijfer ${digit} in ${cellLabel}. Gebruik de gemarkeerde ${houseLabels} om dat te bevestigen.`
        ),
        eliminationTitle: (techniqueLabel: string, digitsLabel: string) => `${techniqueLabel}: schrap ${digitsLabel}`,
        lockedCandidatesBody: (
          digitsLabel: string,
          sourceHouseLabel: string,
          targetHouseLabel: string,
        ) => (
          `De cijfers ${digitsLabel} zitten opgesloten in ${sourceHouseLabel}, dus verwijder ze uit de andere gemarkeerde cellen in ${targetHouseLabel}.`
        ),
        eliminationBody: (
          techniqueLabel: string,
          digitsLabel: string,
          targetLabels: string,
          houseLabels: string,
        ) => (
          `${techniqueLabel} verwijdert ${digitsLabel} uit ${targetLabels}. De gemarkeerde ${houseLabels} laten die kandidaten daar geen geldige plek meer.`
        ),
        unsupportedTitle: 'Nog geen ondersteunde volgende zet',
        unsupportedBody: 'Deze positie kan een diepere techniek nodig hebben dan Sudoku momenteel in Quiet Grid uitlegt.',
      },
    },
    tutorial: {
      exitLabel: {
        skip: 'Tutorial overslaan',
        end: 'Sudoku openen',
      },
      controlLabel: 'Live spel',
      progressLabel: (current: number, total: number) => `Les ${current} van ${total}`,
      status: {
        nextLesson: 'Mooi. Op naar de volgende les…',
        finishing: 'Mooi. Sudoku wordt geopend…',
      },
      lessons: {
        goal: {
          title: 'Vul elke rij, kolom en box',
          body: 'Sudoku is opgelost wanneer elke rij, elke kolom en elke 3×3-box de cijfers 1 tot en met 9 precies één keer bevat. Gegeven cijfers blijven vast staan.',
          summary: 'Begin elke scan met één huis tegelijk: rij, kolom of box.',
          controlHint: 'Live spel: tik op een cel en gebruik daarna de werkbalkcijfers. Tik opnieuw op hetzelfde cijfer om het te wissen.',
          continueLabel: 'Toon de eerste zet',
        },
        'naked-single': {
          title: 'Er staat nu een naked single klaar',
          body: 'De gemarkeerde cel heeft na controle van rij, kolom en box nog maar één geldig cijfer over.',
          summary: 'Wanneer een cel één legaal cijfer heeft, plaats je dat meteen.',
          controlHint: 'Live spel: blijf in cijfermodus, tik op de gemarkeerde cel en tik daarna op cijfer 4 in de werkbalk.',
          prompt: 'Welk cijfer hoort in rij 1, kolom 3?',
          options: {
            '4': '4',
            '8': '8',
          },
          correctOptionKey: '4',
          correctFeedback: 'Correct. Cijfer 4 is de enige kandidaat die nog overblijft voor die cel.',
          wrongFeedback: 'Probeer opnieuw. Controleer rij, kolom en box samen voordat je een cijfer plaatst.',
        },
        'notes-mode': {
          title: 'Gebruik notities voordat je gokt',
          body: 'Deze gemarkeerde cel heeft nog meer dan één geldig kandidaatcijfer en is dus nog niet klaar voor een definitief cijfer.',
          summary: 'Notities zijn eersteklas ondersteuning: markeer kandidaten voordat je een waarde vastlegt.',
          controlHint: 'Live spel: tik op het potlood om naar notitiemodus te gaan en tik daarna op een werkbalkcijfer om die notitie te schakelen.',
          prompt: 'Welke modus moet je nu gebruiken voor rij 6, kolom 2?',
          options: {
            digit: 'Cijfers',
            notes: 'Notities',
          },
          correctOptionKey: 'notes',
          correctFeedback: 'Correct. Deze cel heeft eerst notities nodig voordat hij klaar is voor een definitief cijfer.',
          wrongFeedback: 'Nog niet. In cijfermodus leg je een waarde vast, maar deze cel heeft nog meerdere geldige kandidaten.',
        },
        'hidden-single': {
          title: 'Notities kunnen een hidden single onthullen',
          body: 'De gemarkeerde rij toont meerdere kandidaatsnotities, maar slechts één cel kan nog cijfer 5 bevatten.',
          summary: 'Een hidden single ontstaat wanneer één kandidaat nog maar in één cel binnen een huis voorkomt.',
          controlHint: 'Live spel: nadat je de hidden single hebt gezien, schakel je het potlood uit en tik je op cijfer 5 in de werkbalk.',
          prompt: 'Welk cijfer moet na het lezen van de notities in rij 6, kolom 2 komen?',
          options: {
            '4': '4',
            '5': '5',
          },
          correctOptionKey: '5',
          correctFeedback: 'Correct. Cijfer 5 komt in de notities nog maar één keer voor in de gemarkeerde rij en moet dus daar staan.',
          wrongFeedback: 'Probeer opnieuw. In de rij ontbreekt nog een 5 en dit is de enige cel waar die kan staan.',
        },
      },
    },
    learning: {
      labels: {
        cell: (row: number, col: number) => `rij ${row}, kolom ${col}`,
        row: (index: number) => `rij ${index}`,
        column: (index: number) => `kolom ${index}`,
        box: (index: number) => `box ${index}`,
        joinList: (items: string[]) => {
          if (items.length <= 1) {
            return items[0] ?? '';
          }
          if (items.length === 2) {
            return `${items[0]} en ${items[1]}`;
          }
          return `${items.slice(0, -1).join(', ')} en ${items[items.length - 1]}`;
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
        coloring: 'Kleuren',
        chains: 'Ketens',
      },
      analyzer: {
        legend: {
          evidence: 'Bewijs',
          place: 'Plaats cijfer',
          eliminate: 'Schrap notitie',
        },
      },
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Vul het rooster',
        body: 'Plaats de cijfers 1 tot en met 9 zodat elke rij, kolom en 3×3-box elk cijfer precies één keer gebruikt.',
      },
      {
        num: '2',
        title: 'Respecteer de gegeven cijfers',
        body: 'De startcijfers blijven vast staan en vormen het anker voor elke geldige Sudoku-sessie.',
      },
      {
        num: '3',
        title: 'Gebruik notities als een cel nog niet klaar is',
        body: 'Notities zijn optionele ondersteunende acties, maar ze helpen kandidaten bij te houden voordat je een definitief cijfer plaatst.',
      },
    ],
    tips: [
      {
        key: 'scan-rows',
        title: 'Scan één huis tegelijk',
        body: 'Kies één rij, één kolom of één box en vraag je af welke cijfers nog ontbreken. Kleine, lokale controles blijven makkelijker te vertrouwen dan brede gissingen.',
        example: [
          [5, 3, null],
          [6, 7, 2],
          [1, 9, 8],
        ],
      },
      {
        key: 'notes-first',
        title: 'Notities houden moeilijke cellen eerlijk',
        body: 'Als een cel nog meerdere legale cijfers heeft, schakel dan naar notitiemodus in plaats van te gokken. Notities laten hidden singles en pair-deducties later beter opvallen.',
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
      eyebrow: 'Puzzel beëindigd',
      title: 'Sudoku-sessie beëindigd',
      body: 'Je hebt deze Sudoku-sessie beëindigd voordat het rooster af was.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzel geblokkeerd',
      title: 'Sudoku-bord werd ongeldig',
      body: 'Minstens één rij, kolom of box is nu in strijd met de Sudoku-regels. Los het conflict op voordat je naar de volgende zet vraagt.',
      icon: '⚠️',
    },
  },
} as const;

export default nl;
