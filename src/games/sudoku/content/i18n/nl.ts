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
        explainButton: 'Leg deze zet uit',
      },
      techniqueLesson: {
        backButton: 'Terug naar het spel',
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
    goal: 'Plaats elk cijfer 1–9 precies één keer in elke rij, kolom en 3×3-box.',
    controls: 'Tik op een cel om hem te selecteren, tik dan op een cijferknop om te plaatsen. Schakel naar notitiemodus om kandidaten in te vullen.',
    wrongMove: 'Een dubbel cijfer in dezelfde rij, kolom of box wordt gemarkeerd als conflict.',
    rules: [
      {
        num: '1',
        title: 'Vul het rooster',
        body: 'Geen cijfer mag twee keer voorkomen in dezelfde rij, kolom of box.',
      },
      {
        num: '2',
        title: 'Respecteer de gegeven cijfers',
        body: 'De vooraf ingevulde cijfers staan vast — je kunt ze niet veranderen.',
      },
      {
        num: '3',
        title: 'Gebruik notities als een cel nog niet klaar is',
        body: 'Schakel naar notitiemodus om mogelijke cijfers in een cel te noteren en ze te schrappen naarmate de puzzel zich toespitst.',
      },
    ],
    techniques: [
      {
        key: 'naked-single',
        title: 'Naakt enkelvoud',
        body: 'Als na het uitsluiten van elk cijfer in de rij, kolom en box slechts één cijfer overblijft in een cel, plaats het.',
      },
      {
        key: 'hidden-single',
        title: 'Verborgen enkelvoud',
        body: 'Als een cijfer slechts in één cel past binnen een rij, kolom of box, plaats het daar, ook als andere kandidaten nog zichtbaar zijn.',
      },
      {
        key: 'notes-mode',
        title: 'Notitiemodus',
        body: 'Schrijf alle mogelijke cijfers als notities en schrap ze terwijl omliggende rijen, kolommen en boxen worden ingevuld — totdat er nog maar één overblijft.',
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
        body: 'Als een cel nog meerdere geldige cijfers heeft, schakel dan naar notitiemodus in plaats van te gokken. Genoteerde kandidaten maken hidden singles en andere gedwongen plaatsingen veel gemakkelijker te herkennen.',
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
