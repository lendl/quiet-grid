const de = {
  strings: {
    title: 'Takuzu',
    shortTitle: 'Takuzu',
    tagline: 'Fülle das Raster mit 0en und 1en durch Logik.',
    difficultyLabels: {
      easy: 'Leicht',
      medium: 'Mittel',
      hard: 'Schwer',
      expert: 'Experte',
    },
    difficultyDescriptions: {
      easy: 'Mehr Startzellen und sanftere Schlussfolgerungen.',
      medium: 'Ausgewogene Eröffnungen, die etwas weiter vorausdenken lassen.',
      hard: 'Engere Aufstellungen mit weniger freien Informationen und stärkerer Mustererkennung.',
      expert: 'Spärliche Eröffnungen mit anhaltendem Deduktionsdruck über das gesamte Rätsel.',
    },
    play: {
      metadataLabels: {
        size: 'Größe',
        difficulty: 'Schwierigkeit',
      },
      helperToggle: {
        show: 'Nächsten Zug anzeigen',
        hide: 'Nächsten Zug verbergen',
      },
      noPuzzlesDialog: {
        title: 'Keine Rätsel verfügbar',
        message: (difficultyLabel: string) => `Keine Rätsel im Katalog „${difficultyLabel}" gefunden.`,
      },
      cellLabel: 'Zelle',
      tutorial: {
        progressLabel: (step: number) => `Lektion ${step}`,
        introNote: 'Ziel: Fülle das gesamte Raster, sodass jede Zeile und Spalte ausgeglichen, einzigartig und frei von Drillingsfolgen bleibt. Im echten Spiel tippst du auf die markierte Zelle, um zwischen leer, 0, 1 und wieder leer zu wechseln. In diesem Tutorial verwendest du die Schaltflächen 0 und 1 unten.',
        exitLabel: {
          end: 'Tutorial beenden',
          skip: 'Tutorial überspringen',
        },
        status: {
          finishing: 'Tutorial wird abgeschlossen…',
          nextLesson: 'Nächste Lektion startet…',
          nextStep: 'Nächster Schritt startet…',
        },
        selectAnswerLabel: (value: 0 | 1) => `${value} wählen`,
      },
    },
  },
  howToPlayRules: [
    {
      num: '1',
      title: 'Jede Zelle ausfüllen',
      body: 'Tippe auf eine Zelle, um zu wechseln: leer → 0 → 1 → leer. Fülle das gesamte Raster.',
    },
    {
      num: '2',
      title: 'Keine drei in einer Reihe',
      body: 'Vermeide drei gleiche Ziffern nebeneinander in einer Zeile oder Spalte.',
    },
    {
      num: '3',
      title: 'Gleiche Hälften',
      body: 'Jede Zeile und jede Spalte muss genau gleich viele 0en und 1en enthalten.',
    },
    {
      num: '4',
      title: 'Alle Linien sind einzigartig',
      body: 'Keine zwei Zeilen dürfen identisch sein, und keine zwei Spalten dürfen identisch sein.',
    },
  ],
  howToPlayTips: [
    {
      key: 'find-pairs',
      title: 'Paare finden',
      body: 'Zwei benachbarte gleiche Ziffern bedeuten, dass die Zellen auf beiden Seiten die entgegengesetzte Ziffer sein müssen.',
      example: [[0, 0, 'a1']],
    },
    {
      key: 'avoid-trios',
      title: 'Drillinge vermeiden',
      body: 'Wenn dieselbe Ziffer mit einer leeren Zelle dazwischen erscheint, muss diese mittlere Zelle die entgegengesetzte Ziffer sein.',
      example: [[1, 'a0', 1]],
    },
    {
      key: 'complete-lines',
      title: 'Zeilen und Spalten vervollständigen',
      body: 'Sobald die maximale Anzahl einer Ziffer in einer Linie erreicht ist, müssen alle verbleibenden leeren Zellen die andere Ziffer sein.',
      example: [[0, 1, 0, 1, 0, 'a1']],
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Anhand gefüllter Linien ausschließen',
      body: 'Würde das Ausfüllen einer Zeile oder Spalte sie identisch mit einer bereits vollständigen machen, müssen diese Werte getauscht werden.',
      example: [
        [1, 0, 1, 0, 1, 0],
        [1, 0, 'a0', 'a1', 1, 0],
      ],
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Unmögliche Kombinationen ausschließen',
      body: 'Wäre der markierte Wert eine Eins, würden die verbleibenden leeren Felder einen Drilling erzwingen. Da das nicht erlaubt ist, muss der markierte Wert null sein.',
      example: [[1, 1, 0, null, null, 'a0']],
    },
    {
      key: 'score-matters',
      title: 'So funktioniert die Bewertung',
      body: 'Dein Punktestand beginnt bei 10.000 und sinkt, während der Rätseltimer läuft. Jede abgeschlossene Linie, die nicht der Lösung entspricht, zieht 500 Punkte ab. Höhere Schwierigkeitsgrade verlieren Punkte langsamer.',
    },
    {
      key: 'watch-for-flashes',
      title: 'Auf Aufblitzen achten',
      body: 'Wenn du eine Zeile oder Spalte korrekt abschließt, leuchten alle ihre Zellen kurz als Bestätigung auf.',
    },
  ],
  loss: {
    forfeit: {
      eyebrow: 'Rätsel beendet',
      title: 'Rätsel unfertig',
      body: 'Du hast dieses Rätsel beendet, bevor es gelöst war.',
      icon: '🏁',
    },
    'rule-based': {
      eyebrow: 'Rätsel verloren',
      title: 'Rätsel verloren',
      body: 'Dieses Rätsel endete, bevor es gelöst werden konnte.',
      icon: '⚠️',
    },
  },
  tutorialLessons: {
    'find-pairs': {
      title: 'Lektion 1: Paare finden',
      body: 'Zwei benachbarte gleiche Ziffern bedeuten, dass die Zellen auf beiden Seiten die entgegengesetzte Ziffer sein müssen.',
      prompt: 'Soll die markierte Zelle eine 1 oder eine 0 sein?',
      retry: 'Nicht diese. Wäre die markierte Zelle eine 1, würde die Zeile mit drei 1en in einer Reihe beginnen.',
      success: 'Richtig. Zwei 1en sitzen bereits nebeneinander, also muss die markierte Zelle 0 sein.',
    },
    'avoid-trios': {
      title: 'Lektion 2: Drillinge vermeiden',
      body: 'Wenn dieselbe Ziffer mit einer leeren Zelle dazwischen erscheint, muss diese mittlere Zelle die entgegengesetzte sein.',
      prompt: 'Soll die markierte Zelle eine 1 oder eine 0 sein?',
      retry: 'Nicht diese. Die markierte Zelle sitzt zwischen zwei 1en, also kann sie nicht auch 1 sein.',
      success: 'Richtig. Die mittlere Zelle muss 0 sein, damit die Zeile keine drei 1en in einer Reihe bildet.',
    },
    'complete-lines': {
      title: 'Lektion 3: Zeilen und Spalten vervollständigen',
      body: 'Sobald die maximale Anzahl einer Ziffer in einer Linie erreicht ist, müssen alle verbleibenden leeren Zellen die andere Ziffer sein.',
      prompt: 'Soll die markierte Zelle eine 1 oder eine 0 sein?',
      retry: 'Nicht diese. Diese Zeile hat bereits alle 0en, die sie enthalten kann, also muss die verbleibende Zelle 1 sein.',
      success: 'Richtig. Die Zeile hat bereits drei 0en, also muss die verbleibende Zelle 1 sein.',
    },
    'eliminate-filled-lines': {
      title: 'Lektion 4: Anhand gefüllter Linien ausschließen',
      body: 'Würde das Ausfüllen einer Zeile oder Spalte sie identisch mit einer bereits vollständigen machen, müssen diese Werte getauscht werden.',
      prompt: 'Soll die markierte Zelle eine 1 oder eine 0 sein?',
      retry: 'Nicht diese. Diese Wahl würde die untere Zeile identisch mit der darüber abgeschlossenen machen.',
      success: 'Richtig. Das Tauschen dieses Werts hält die untere Zeile anders als die abgeschlossene Zeile.',
    },
    'eliminate-impossible-combinations': {
      title: 'Lektion 5: Unmögliche Kombinationen ausschließen',
      body: 'Schau auf die markierte Zelle und die verbleibenden leeren Felder in der Zeile. Nutze das Muster, um herauszufinden, welcher Wert dort passt.',
      prompt: 'Soll die markierte Zelle eine 1 oder eine 0 sein?',
      retry: 'Nicht diese. Wäre die markierte Zelle eine 1, würden die verbleibenden leeren Felder einen Drilling erzwingen.',
      success: 'Richtig. Eine 0 zu wählen vermeidet den Drilling, den eine 1 später in der Zeile erzwingen würde.',
    },
  },
  learningCenter: {
    pausedNextMove: {
      title: 'Noch kein klarer nächster Zug',
      body: 'Dieser Teil des Rätsels bietet gerade keinen starken nächsten Zug. Versuche eine andere Zeile oder Spalte und frage dann erneut.',
    },
    findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Nächster Zug in ${lineLabel}`,
        body: `Platziere ${targetValue} in der markierten ${cellLabel}. Warum: Zwei ${repeatedValue}en sitzen bereits nebeneinander in ${lineLabel}, eine weitere ${repeatedValue} würde drei in einer Reihe erzeugen.`,
      };
    },
    avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Nächster Zug in ${lineLabel}`,
        body: `Platziere ${targetValue} in der markierten Zelle. Warum: ${lineLabel} zeigt bereits ${repeatedValue} _ ${repeatedValue}, also muss die offene Zelle dazwischen ${targetValue} sein, um einen Drilling zu vermeiden.`,
      };
    },
    completeLines(lineLabel: string, filledValue: 0 | 1, filledCount: number, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Nächster Zug in ${lineLabel}`,
        body: `Platziere ${targetValue} in der markierten ${cellLabel}. Warum: ${lineLabel} hat bereits ${filledCount} ${filledValue}${filledCount === 1 ? '' : 'en'}, also muss die verbleibende offene ${cellLabel} ${targetValue} sein, um die Linie ausgeglichen zu halten.`,
      };
    },
    eliminateFilledLines(lineLabel: string, matchingLineLabel: string, targetValue: 0 | 1, cellLabel: string, lineKindLabel: string) {
      return {
        title: `Nächster Zug in ${lineLabel}`,
        body: `Platziere ${targetValue} in der markierten ${cellLabel}. Warum: Wenn ${lineLabel} mit ${matchingLineLabel} übereinstimmte, wären die abgeschlossenen ${lineKindLabel} nicht mehr einzigartig.`,
      };
    },
    eliminateImpossible(lineLabel: string, blockedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Nächster Zug in ${lineLabel}`,
        body: `Platziere ${targetValue} in der markierten Zelle. Warum: Wäre diese Zelle ${blockedValue}, würde ${lineLabel} später einen ungültigen Drilling erzwingen, also ist ${targetValue} der einzige Wert, der die Linie lösbar hält.`,
      };
    },
    avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1) {
      return {
        title: `Nächster Zug zur Reparatur von ${lineLabel}`,
        body: `Ändere eine markierte Zelle in ${lineLabel}. Warum: Drei ${repeatedValue}en in einer Reihe verstoßen gegen die Keine-Drillinge-Regel.`,
      };
    },
    completeLinesRepair(lineLabel: string, filledValue: 0 | 1, filledCount: number, limit: number) {
      return {
        title: `Nächster Zug zur Neuausrichtung von ${lineLabel}`,
        body: `Ändere eine markierte Zelle in ${lineLabel}. Warum: ${lineLabel} enthält bereits ${filledCount} ${filledValue}${filledCount === 1 ? '' : 'en'}, aber das Limit ist ${limit}.`,
      };
    },
    eliminateFilledLinesRepair(firstLineLabel: string, secondLineLabel: string, lineLabel: string) {
      return {
        title: `Nächster Zug zum Trennen gleicher ${lineLabel}`,
        body: `Ändere eine markierte Zelle. Warum: ${firstLineLabel} und ${secondLineLabel} sind identisch, aber abgeschlossene ${lineLabel} müssen einzigartig bleiben.`,
      };
    },
  },
} as const;

export default de;
