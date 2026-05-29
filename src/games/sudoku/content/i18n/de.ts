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
    rules: [
      {
        num: '1',
        title: 'Fülle das Gitter',
        body: 'Setze die Ziffern 1 bis 9 so ein, dass jede Zeile, Spalte und 3×3-Box jede Ziffer genau einmal enthält.',
      },
      {
        num: '2',
        title: 'Respektiere die Vorgaben',
        body: 'Die Startvorgaben bleiben fest und verankern jede gültige Sudoku-Sitzung.',
      },
      {
        num: '3',
        title: 'Nutze Notizen, wenn eine Zelle noch nicht bereit ist',
        body: 'Notizen sind optionale Unterstützungsaktionen, helfen dir aber dabei, Kandidaten zu verfolgen, bevor du eine endgültige Ziffer setzt.',
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
        body: 'Wenn eine Zelle noch mehrere legale Ziffern hat, wechsle in den Notizmodus statt zu raten. Notizen lassen Hidden Singles und Paar-Deduktionen später klarer hervortreten.',
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
