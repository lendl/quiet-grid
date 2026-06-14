const de = {
  strings: {
    title: 'Sudoku',
    shortTitle: 'Sudoku',
    tagline: 'Setze die Ziffern 1 bis 9 so ein, dass jede Zeile, Spalte und Box gültig bleibt.',
    difficultyLabels: {
      easy: 'Leicht',
      medium: 'Mittel',
      hard: 'Schwer',
      expert: 'Experte',
    },
    difficultyDescriptions: {
      easy: 'Startgitter für logisch-menschliches Lösen mit Fokus auf Singles.',
      medium: 'Ausgewogene Gitter, die in notizgestützte Techniken hineinwachsen können.',
      hard: 'Spärliche Gitter für reichere Zuganalyse und Validierung.',
      expert: 'Sehr spärliche Gitter für fortgeschrittene Ketten- und Coloring-Deduktionen.',
    },
    play: {
      metadataLabels: {
        size: 'Größe',
        difficulty: 'Schwierigkeit',
        filled: 'Gefüllt',
      },
      noPuzzlesDialog: {
        title: 'Sudoku nicht verfügbar',
        message: (difficultyLabel: string) => `Noch kein Sudoku-Rätsel für ${difficultyLabel} bereit.`,
      },
      cellLabel: 'Zelle',
      resetZoom: 'Zoom zurücksetzen',
      helperToggle: {
        show: 'Nächsten Zug anzeigen',
        hide: 'Nächsten Zug verbergen',
      },
      controls: {
        noteModeEnabled: 'Notizmodus an',
        noteModeDisabled: 'Notizmodus aus',
        selectedCellPrompt: 'Wähle eine Zelle, um eine Ziffer oder Notiz einzugeben.',
        selectedCellLabel: (cellLabel: string) => `Ausgewählt: ${cellLabel}`,
        digitButtonLabel: (digit: number) => `Ziffer ${digit} in der ausgewählten Zelle verwenden`,
        noteDigitLabel: (digit: number) => `Notiz ${digit} in der ausgewählten Zelle umschalten`,
      },
      nextMove: {
        invalidConflictTitle: 'Brett muss zuerst korrigiert werden',
        invalidConflictBody: (houseLabel: string, digit: number) => (
          `Die Ziffer ${digit} erscheint mehr als einmal in ${houseLabel}. Korrigiere diesen Konflikt, bevor du nach dem nächsten Zug fragst.`
        ),
        invalidDeadCellTitle: 'Brett muss zuerst korrigiert werden',
        invalidDeadCellBody: (cellLabel: string) => (
          `${cellLabel} hat keine gültige Ziffer mehr übrig. Korrigiere die umgebenden Einträge, bevor du nach dem nächsten Zug fragst.`
        ),
        placementTitle: (techniqueLabel: string, digit: number) => `${techniqueLabel}: ${digit} setzen`,
        nakedSingleBody: (digit: number, cellLabel: string) => (
          `Nur die Ziffer ${digit} passt in ${cellLabel}, nachdem Zeile, Spalte und Box geprüft wurden.`
        ),
        hiddenSingleBody: (digit: number, houseLabel: string, cellLabel: string) => (
          `Die Ziffer ${digit} passt in ${houseLabel} nur in eine Zelle, also setze sie in ${cellLabel}.`
        ),
        placementBody: (techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string) => (
          `${techniqueLabel} erzwingt die Ziffer ${digit} in ${cellLabel}. Nutze die markierten ${houseLabels}, um das zu bestätigen.`
        ),
        eliminationTitle: (techniqueLabel: string, digitsLabel: string) => `${techniqueLabel}: ${digitsLabel} streichen`,
        lockedCandidatesBody: (
          digitsLabel: string,
          sourceHouseLabel: string,
          targetHouseLabel: string,
        ) => (
          `Die Ziffern ${digitsLabel} sind in ${sourceHouseLabel} eingeschlossen, also entferne sie aus den anderen markierten Zellen in ${targetHouseLabel}.`
        ),
        eliminationBody: (
          techniqueLabel: string,
          digitsLabel: string,
          targetLabels: string,
          houseLabels: string,
        ) => (
          `${techniqueLabel} entfernt ${digitsLabel} aus ${targetLabels}. Die markierten ${houseLabels} lassen diesen Kandidaten dort kein gültiges Zuhause mehr.`
        ),
        unsupportedTitle: 'Noch kein unterstützter nächster Zug',
        unsupportedBody: 'Diese Stellung könnte eine tiefere Technik benötigen, als Sudoku derzeit in Quiet Grid vermittelt.',
        explainButton: 'Diesen Zug erklären',
      },
      techniqueLesson: {
        backButton: 'Zurück zum Spiel',
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
        skip: 'Tutorial überspringen',
        end: 'Sudoku öffnen',
      },
      controlLabel: 'Live-Spiel',
      progressLabel: (current: number, total: number) => `Lektion ${current} von ${total}`,
      status: {
        nextLesson: 'Gut. Weiter zur nächsten Lektion…',
        finishing: 'Gut. Sudoku wird geöffnet…',
      },
      lessons: {
        goal: {
          title: 'Fülle jede Zeile, Spalte und Box',
          body: 'Sudoku ist gelöst, wenn jede Zeile, jede Spalte und jede 3×3-Box die Ziffern 1 bis 9 genau einmal enthält. Vorgegebene Ziffern bleiben fest.',
          summary: 'Beginne jeden Scan mit genau einem Haus: Zeile, Spalte oder Box.',
          controlHint: 'Live-Spiel: Tippe auf eine Zelle und nutze dann die Ziffern in der Leiste. Tippe dieselbe Ziffer erneut an, um sie zu löschen.',
          continueLabel: 'Ersten Zug zeigen',
        },
        'naked-single': {
          title: 'Ein Naked Single ist jetzt bereit',
          body: 'Die markierte Zelle hat nach Prüfung von Zeile, Spalte und Box nur noch eine gültige Ziffer übrig.',
          summary: 'Wenn eine Zelle nur eine legale Ziffer hat, setze sie sofort.',
          controlHint: 'Live-Spiel: Bleibe im Ziffernmodus, tippe auf die markierte Zelle und dann auf die Ziffer 4 in der Leiste.',
          prompt: 'Welche Ziffer gehört in Zeile 1, Spalte 3?',
          options: {
            '4': '4',
            '8': '8',
          },
          correctOptionKey: '4',
          correctFeedback: 'Richtig. Die Ziffer 4 ist der einzige Kandidat, der für diese Zelle übrig bleibt.',
          wrongFeedback: 'Versuche es erneut. Prüfe Zeile, Spalte und Box zusammen, bevor du eine Ziffer setzt.',
        },
        'notes-mode': {
          title: 'Nutze Notizen, bevor du rätst',
          body: 'Diese markierte Zelle hat noch mehr als einen gültigen Kandidaten und ist deshalb noch nicht bereit für eine feste Ziffer.',
          summary: 'Notizen sind erstklassige Unterstützung: Markiere Kandidaten, bevor du einen Wert festlegst.',
          controlHint: 'Live-Spiel: Tippe auf den Stift, um in den Notizmodus zu wechseln, und dann auf eine Ziffer in der Leiste, um diese Notiz umzuschalten.',
          prompt: 'Welchen Modus solltest du jetzt für Zeile 6, Spalte 2 verwenden?',
          options: {
            digit: 'Ziffern',
            notes: 'Notizen',
          },
          correctOptionKey: 'notes',
          correctFeedback: 'Richtig. Diese Zelle braucht zuerst Notizen, bevor sie für eine endgültige Ziffer bereit ist.',
          wrongFeedback: 'Noch nicht. Im Ziffernmodus legst du einen Wert fest, aber diese Zelle hat noch mehrere gültige Kandidaten.',
        },
        'hidden-single': {
          title: 'Notizen können ein Hidden Single aufdecken',
          body: 'Die markierte Zeile zeigt mehrere Kandidaten-Notizen, aber nur eine Zelle kann noch die Ziffer 5 aufnehmen.',
          summary: 'Ein Hidden Single entsteht, wenn ein Kandidat in einem Haus nur noch in einer Zelle vorkommt.',
          controlHint: 'Live-Spiel: Sobald du das Hidden Single erkannt hast, schalte den Stift aus und tippe auf die Ziffer 5 in der Leiste.',
          prompt: 'Welche Ziffer muss nach dem Lesen der Notizen in Zeile 6, Spalte 2 stehen?',
          options: {
            '4': '4',
            '5': '5',
          },
          correctOptionKey: '5',
          correctFeedback: 'Richtig. Die Ziffer 5 taucht in den Notizen der markierten Zeile nur einmal auf und muss daher dort stehen.',
          wrongFeedback: 'Versuche es erneut. In der Zeile fehlt noch eine 5, und das ist die einzige Zelle, die sie aufnehmen kann.',
        },
      },
    },
    learning: {
      labels: {
        cell: (row: number, col: number) => `Zeile ${row}, Spalte ${col}`,
        row: (index: number) => `Zeile ${index}`,
        column: (index: number) => `Spalte ${index}`,
        box: (index: number) => `Box ${index}`,
        joinList: (items: string[]) => {
          if (items.length <= 1) {
            return items[0] ?? '';
          }
          if (items.length === 2) {
            return `${items[0]} und ${items[1]}`;
          }
          return `${items.slice(0, -1).join(', ')} und ${items[items.length - 1]}`;
        },
      },
      techniqueLabels: {
        'naked-single': 'Naked Single',
        'hidden-single': 'Hidden Single',
        'naked-pair': 'Naked Pair',
        'hidden-pair': 'Hidden Pair',
        'pointing-pair-triple': 'Pointing Pair/Triple',
        'box-line-reduction': 'Box-Line-Reduction',
        'x-wing': 'X-Wing',
        'swordfish': 'Swordfish',
        'xy-wing': 'XY-Wing',
        'xyz-wing': 'XYZ-Wing',
        coloring: 'Coloring',
        chains: 'Ketten',
      },
      analyzer: {
        legend: {
          evidence: 'Hinweis',
          place: 'Ziffer setzen',
          eliminate: 'Notiz streichen',
        },
      },
    },
  },
  howToPlay: {
    goal: 'Platziere jede Ziffer 1–9 genau einmal in jeder Zeile, Spalte und 3×3-Box.',
    controls: 'Tippe auf eine Zelle, um sie auszuwählen, dann auf eine Zifferntaste, um sie zu setzen. Schalte in den Notizmodus, um Kandidaten einzutragen.',
    wrongMove: 'Eine doppelte Ziffer in der gleichen Zeile, Spalte oder Box wird als Konflikt hervorgehoben.',
    rules: [
      {
        num: '1',
        title: 'Fülle das Gitter',
        body: 'Keine Ziffer darf in derselben Zeile, Spalte oder Box zweimal vorkommen.',
      },
      {
        num: '2',
        title: 'Respektiere die Vorgaben',
        body: 'Die vorausgefüllten Ziffern sind fest — du kannst sie nicht ändern.',
      },
      {
        num: '3',
        title: 'Nutze Notizen, wenn eine Zelle noch nicht bereit ist',
        body: 'Schalte in den Notizmodus, um mögliche Ziffern für eine Zelle einzutragen und sie zu streichen, während das Rätsel sich eingrenzt.',
      },
    ],
    techniques: [
      {
        key: 'naked-single',
        title: 'Nacktes Einzelnes',
        body: 'Wenn nach dem Ausschließen aller Ziffern in der Zeile, Spalte und Box nur eine Ziffer in einer Zelle übrig bleibt, setze sie.',
      },
      {
        key: 'hidden-single',
        title: 'Verstecktes Einzelnes',
        body: 'Wenn eine Ziffer in einer Zeile, Spalte oder Box nur in eine Zelle passt, setze sie dort, auch wenn andere Kandidaten noch sichtbar sind.',
      },
      {
        key: 'notes-mode',
        title: 'Notizmodus',
        body: 'Schreibe alle möglichen Ziffern als Notizen und streiche sie durch, während umliegende Zeilen, Spalten und Boxen sich füllen — bis nur noch eine übrig ist.',
      },
    ],
    tips: [
      {
        key: 'scan-rows',
        title: 'Scanne immer nur ein Haus',
        body: 'Wähle eine Zeile, eine Spalte oder eine Box und frage dich, welche Ziffern noch fehlen. Kleine, lokale Prüfungen sind leichter zu vertrauen als große Vermutungen.',
        example: [
          [5, 3, null],
          [6, 7, 2],
          [1, 9, 8],
        ],
      },
      {
        key: 'notes-first',
        title: 'Notizen halten schwierige Zellen ehrlich',
        body: 'Wenn eine Zelle noch mehrere gültige Ziffern hat, wechsle in den Notizmodus statt zu raten. Eingetragene Kandidaten machen Hidden Singles und andere erzwungene Placements viel leichter erkennbar.',
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
      eyebrow: 'Rätsel beendet',
      title: 'Sudoku-Sitzung beendet',
      body: 'Du hast diese Sudoku-Sitzung beendet, bevor du das Gitter fertiggestellt hast.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Rätsel blockiert',
      title: 'Sudoku-Brett wurde ungültig',
      body: 'Mindestens eine Zeile, Spalte oder Box verstößt jetzt gegen die Sudoku-Regeln. Korrigiere den Konflikt, bevor du nach dem nächsten Zug fragst.',
      icon: '⚠️',
    },
  },
} as const;

export default de;
