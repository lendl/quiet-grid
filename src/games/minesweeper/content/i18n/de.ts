const de = {
  strings: {
    title: 'Minesweeper',
    shortTitle: 'Minesweeper',
    tagline: 'Räume das Raster frei, ohne eine Mine zu öffnen.',
    difficultyLabels: {
      easy: 'Leicht',
      medium: 'Mittel',
      hard: 'Schwer',
      expert: 'Experte',
    },
    difficultyDescriptions: {
      easy: 'Mehr Spielraum zum frühen Scannen und ruhigen Hinweislesen.',
      medium: 'Ein ausgeglichenes Feld mit mehr Minen und weniger sicheren Öffnungen.',
      hard: 'Engere Bereiche, die sorgfältiges Markieren und Verfolgen von Hinweisen belohnen.',
      expert: 'Dichte Minenfelder mit von Anfang an sehr wenig Spielraum.',
    },
    play: {
      metadataLabels: {
        size: 'Größe',
        difficulty: 'Schwierigkeit',
        minesLeft: 'Minen',
      },
      helperToggle: {
        show: 'Nächsten Zug anzeigen',
        hide: 'Nächsten Zug verbergen',
      },
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Sichere Felder aufdecken',
        body: 'Öffne Felder, die du für sicher hältst. Eine aufgedeckte Mine beendet das aktive Rätsel.',
      },
      {
        num: '2',
        title: 'Die Zahlen lesen',
        body: 'Jede aufgedeckte Zahl zeigt, wie viele Minen dieses Feld berühren, auch diagonal.',
      },
      {
        num: '3',
        title: 'Wahrscheinliche Minen markieren',
        body: 'Halte ein verstecktes Feld gedrückt, um eine Flagge zu setzen oder zu entfernen, wenn du sicher bist, dass sich dort eine Mine befindet.',
      },
      {
        num: '4',
        title: 'Jedes sichere Feld räumen',
        body: 'Das Rätsel ist gelöst, wenn jedes Feld ohne Mine aufgedeckt ist.',
      },
    ],
    tips: [
      {
        key: 'start-from-openings',
        title: 'Bei Öffnungen beginnen',
        body: 'Große leere Öffnungen decken mehrere sichere Felder auf einmal auf und liefern oft die ersten starken Hinweise.',
      },
      {
        key: 'count-neighbors',
        title: 'Gemeinsame Nachbarn zählen',
        body: 'Wenn zwei aufgedeckte Zahlen einige der gleichen versteckten Felder berühren, vergleiche ihre verbleibenden Minenzahlen, bevor du Flaggen setzt.',
      },
      {
        key: 'Use flags carefully',
        title: 'Flaggen sorgfältig verwenden',
        body: 'Flaggen helfen dir, wahrscheinliche Minen zu verfolgen, beweisen aber nicht, dass ein Feld gefährlich ist, es sei denn, die umliegenden Hinweise unterstützen dies.',
      },
      {
        key: 'pace-matters',
        title: 'So funktioniert die Punktewertung',
        body: 'Dein Punktestand beginnt bei 10.000 und sinkt, während der Timer läuft. Schnelleres sicheres Lösen erhält mehr Punkte.',
      },
    ],
  },
  loss: {
    abandoned: {
      eyebrow: 'Rätsel beendet',
      title: 'Rätsel unvollendet',
      body: 'Du hast dieses Rätsel beendet, bevor es gelöst war.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Rätsel verloren',
      title: 'Rätsel verloren',
      body: 'Dieses Rätsel endete, als eine Mine aufgedeckt wurde. Ein neues Rätsel wartet, wenn du bereit bist.',
      icon: '💣',
    },
  },
  analysis: {
    lossSummary({ safeCount, mineCount }: { safeCount: number; mineCount: number }) {
      if (safeCount > 0 && mineCount > 0) {
        return {
          title: 'Logische Züge waren verfügbar',
          body: `Dieses Feld hatte bereits ${safeCount} sichere ${safeCount === 1 ? 'Feld' : 'Felder'} zum Aufdecken und ${mineCount} sichere ${mineCount === 1 ? 'Mine' : 'Minen'} zum Markieren.`,
        };
      }

      if (safeCount > 0) {
        return {
          title: 'Sichere Aufdeckungen waren verfügbar',
          body: `Dieses Feld hatte bereits ${safeCount} sichere ${safeCount === 1 ? 'Feld' : 'Felder'} zum Aufdecken aus den gezeigten Hinweisen.`,
        };
      }

      return {
        title: 'Sichere Markierungen waren verfügbar',
        body: `Dieses Feld hatte bereits ${mineCount} sichere ${mineCount === 1 ? 'Mine' : 'Minen'} zum Markieren aus den gezeigten Hinweisen.`,
      };
    },
    groupedFlagStep({ mineCount }: { mineCount: number }) {
      return {
        title: 'Sichere Minen auf diesem Feld',
        body: `Du kannst die hervorgehobenen ${mineCount === 1 ? 'Feld' : 'Felder'} jetzt markieren. Die aktuellen Hinweise beweisen bereits, dass ${mineCount === 1 ? 'es eine Mine ist' : 'diese Felder Minen sind'}.`,
      };
    },
    groupedSafeStep({ targetCount, reasonCount }: { targetCount: number; reasonCount: number }) {
      return {
        title: 'Mehrere Hinweise stützen diese sichere Aufdeckung',
        body: `Decke die hervorgehobenen ${targetCount === 1 ? 'Feld' : 'Felder'} auf. ${reasonCount} Hinweismuster führen unabhängig zum selben sicheren Zug, also ist diese Aufdeckung aus mehr als einer Richtung bestätigt.`,
      };
    },
    legendEvidence: 'Beweis',
    legendSafe: 'Sicher aufdecken',
    legendMine: 'Mine markieren',
  },
  tutorialText: {
    'goal-and-stakes': {
      title: 'Jedes sichere Feld aufdecken',
      body: 'Decke jedes Feld auf, das keine Mine verbirgt. Wenn du eine Mine aufdeckst, endet der Durchgang sofort.',
      prompt: 'Flaggen helfen beim Verfolgen von Gefahren, aber nur sichere Aufdeckungen gewinnen das Feld.',
      summary: 'Gewinne, indem du jedes sichere Feld aufdeckst. Verliere, indem du eine Mine öffnest.',
      continueLabel: 'Weiter',
    },
    'core-actions': {
      title: 'Du hast zwei Aktionen',
      body: 'In einem echten Rätsel tippst du auf ein verstecktes Feld, um es aufzudecken. Halte ein verstecktes Feld gedrückt, um eine Flagge zu setzen oder zu entfernen.',
      prompt: 'In diesem Tutorial wählst du Aufdecken oder Markieren mit Schaltflächen unten. Auf einem echten Feld deckt Tippen auf und langes Drücken markiert.',
      summary: 'Aufdecken öffnet ein Feld. Gedrückt halten schaltet eine Flagge um. Flaggen helfen beim Verfolgen von Minen, lösen das Feld aber nicht von selbst.',
      continueLabel: 'Weiter',
    },
    'reading-clues': {
      title: 'Zahlen zählen alle berührenden Minen',
      body: 'Jede Zahl zeigt, wie viele Minen dieses Feld in allen acht benachbarten Positionen berühren.',
      prompt: 'Diagonale Nachbarn zählen auch.',
      summary: 'Ein Hinweis sagt dir, wie viele Minen es berühren, nicht genau wo sie sind.',
      continueLabel: 'Weiter',
    },
    'forced-flag': {
      title: 'Feld markieren, das eine Mine verbergen muss',
      body: 'Dieser Hinweis braucht noch eine Mine, und das hervorgehobene Feld ist der einzige verborgene Platz, der übrig bleibt.',
      prompt: 'Was solltest du mit dem hervorgehobenen Feld tun?',
      retry: 'Dieser Hinweis braucht noch eine Mine, und kein anderer verborgener Nachbar kann sie liefern.',
      success: 'Richtig. Das hervorgehobene Feld musste eine Mine sein, also ist Markieren korrekt.',
    },
    'safe-reveal': {
      title: 'Feld aufdecken, das sicher sein muss',
      body: 'Dieser Hinweis hat bereits seine Mine, also kann der hervorgehobene Nachbar keine weitere Mine verbergen.',
      prompt: 'Was solltest du mit dem hervorgehobenen Feld tun?',
      retry: 'Der Hinweis ist bereits durch die markierte Mine erfüllt, also ist der verbleibende verborgene Nachbar sicher.',
      success: 'Richtig. Sobald der Hinweis bereits seine Mine hat, ist das hervorgehobene Feld sicher aufzudecken.',
    },
    'compare-clues': {
      title: 'Hinweise zusammen vergleichen',
      body: 'Diese Hinweise teilen verborgene Felder. Sobald die gemeinsame Gruppe die Minenanzahl abdeckt, wird das zusätzliche Feld sicher.',
      prompt: 'Was solltest du mit dem hervorgehobenen Feld tun?',
      retry: 'Lese beide Hinweise zusammen. Die gemeinsamen verborgenen Felder absorbieren die Minenanzahl, also ist das zusätzliche Feld sicher.',
      success: 'Richtig. Durch den Vergleich beider Hinweise beweist du, dass das hervorgehobene Feld keine Mine sein kann.',
    },
    'advanced-patterns': {
      title: 'Einige Muster verwenden Diagonalen und Überlappungen',
      body: 'Hier zählt die Ecke-1 die markierte diagonale Mine, und die 2 und 1 daneben lesen beide denselben verborgenen oberen Streifen.',
      prompt: 'Ein Eckhinweis zählt Diagonalen, und benachbarte Hinweise können dieselben verborgenen Felder überlappen.',
      summary: 'Lese nicht nur einen Hinweis, wenn eine diagonale Mine oder ein gemeinsamer verborgener Streifen ändert, was benachbarte Hinweise bedeuten.',
      continueLabel: 'Weiter',
    },
    'guess-and-help': {
      title: 'Manchmal erschöpft die Logik sich',
      body: 'Dieser obere Rand passt noch zu mehr als einer Minenanordnung. Die Reihe der Einsen sagt nicht, welche verborgenen Felder die Minen sind.',
      prompt: 'Wenn das passiert, mache die ruhigste Vermutung, die du kannst, und nutze Hilfetools, wenn nötig.',
      summary: 'Verschiedene Minenanordnungen können zu denselben Hinweisen passen. Tipps helfen, Rückgängig behebt Fehltipps, und du kannst dieses Tutorial später wiederholen.',
      continueLabel: 'Beenden',
    },
  },
  learningCenter: {
    formatCellLabel({ row, col }: { row: number; col: number }) {
      return `Reihe ${row + 1}, Spalte ${col + 1}`;
    },
    tileLabel(count: number) {
      return count === 1 ? 'Feld' : 'Felder';
    },
    mineLabel(count: number) {
      return count === 1 ? 'Mine' : 'Minen';
    },
    nextMovePattern({
      patternKey,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterPatternParams) {
      switch (patternKey) {
        case 'single-mine-logic':
          return {
            title: `Sicherer nächster Zug nahe ${clueLabel ?? 'diesem Hinweis'}`,
            body: `Decke die hervorgehobenen ${tileLabel} auf. Dieses lokale Hinweismuster lässt noch einen Minenplatz offen, was die anderen verborgenen ${tileLabel} sicher macht.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Single-Mine-Logik',
              explanationTitle: 'Erklärung',
              explanation: `Dieses lokale Hinweismuster braucht noch genau eine ${mineLabel}. Sobald dieser einzelne Minenplatz feststeht, müssen die anderen berührenden verborgenen ${tileLabel} sicher sein.`,
            },
          };
        case 'all-mines-accounted-for':
          return {
            title: `Sicherer nächster Zug nahe ${clueLabel ?? 'diesem Hinweis'}`,
            body: `Decke die hervorgehobenen ${tileLabel} auf. Um ${clueLabel ?? 'diesen Hinweis'} sind alle ${mineCount} ${mineLabel} bereits bekannt.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Alle Minen erfasst',
              explanationTitle: 'Erklärung',
              explanation: `Dieser Hinweis hat bereits alle ${mineCount} ${mineLabel}, die er braucht, also müssen alle anderen verborgenen ${tileLabel} daneben sicher sein.`,
            },
          };
        case 'only-one-possible-mine':
          return {
            title: 'Sicherer nächster Zug durch Hinweisvergleich',
            body: `Decke die hervorgehobenen ${tileLabel} auf. ${clueLabel ?? 'Einen Hinweis'} zusammen mit ${secondaryClueLabel ?? 'einem anderen Hinweis'} zu lesen lässt nur einen legalen Platz für die verbleibende Mine übrig.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Nur eine mögliche Mine',
              explanationTitle: 'Erklärung',
              explanation: `Der Vergleich dieser Hinweise lässt genau einen legalen Ort für die verbleibende ${mineLabel} übrig, also müssen die extra verborgenen ${tileLabel} außerhalb dieses Minenplatzes sicher sein.`,
            },
          };
        case 'guaranteed-safe-tile':
          return {
            title: `Sicherer nächster Zug nahe ${clueLabel ?? 'diesem Hinweis'}`,
            body: `Decke die hervorgehobenen ${tileLabel} auf. Wenn dieses Feld eine Mine wäre, hätten nahegelegene Hinweise zu viele Minen.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Garantiert sicheres Feld',
              explanationTitle: 'Erklärung',
              explanation: `Wenn das hervorgehobene ${tileLabel} eine Mine wäre, hätte mindestens ein nahegelegener Hinweis zu viele ${mineLabel}. Da das nicht möglich ist, muss das Feld sicher sein.`,
            },
          };
        case 'full-clue-resolution':
          return {
            title: `Sicherer nächster Zug nahe ${clueLabel ?? 'diesem Hinweis'}`,
            body: `Decke die hervorgehobenen ${tileLabel} auf. Die Minenanforderung dieses Hinweises ist vollständig gelöst, also sind die verbleibenden verborgenen ${tileLabel} sicher.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Vollständige Hinweisauflösung',
              explanationTitle: 'Erklärung',
              explanation: `Die Minenanforderung dieses Hinweises ist vollständig durch nahegelegene erzwungene Minenfelder gelöst, also müssen die verbleibenden verborgenen ${tileLabel} daneben sicher sein.`,
            },
          };
        default:
          throw new Error(`Unhandled next move pattern: ${patternKey satisfies never}`);
      }
    },
    flagMovePattern({
      reason,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterMineFlagParams) {
      switch (reason) {
        case 'direct-local':
          return {
            title: `Sichere Mine nahe ${clueLabel ?? 'diesem Hinweis'}`,
            body: `Markiere die hervorgehobenen ${tileLabel}. ${clueLabel ?? 'Dieser Hinweis'} braucht noch ${mineCount} ${mineLabel}, und die hervorgehobenen verborgenen ${tileLabel} sind die einzigen verbliebenen Plätze.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Direkt lokale Mine',
              explanationTitle: 'Erklärung',
              explanation: `Dieser Hinweis braucht noch ${mineCount} ${mineLabel}. Da nur die hervorgehobenen verborgenen ${tileLabel} um ihn herum übrig bleiben, muss jedes hervorgehobene Feld eine Mine sein.`,
            },
          };
        case 'subset-difference':
          return {
            title: 'Sichere Mine durch Hinweisvergleich',
            body: `Markiere die hervorgehobenen ${tileLabel}. Der Vergleich von ${clueLabel ?? 'einem Hinweis'} mit ${secondaryClueLabel ?? 'einem anderen Hinweis'} zeigt, dass die extra verborgenen ${tileLabel} die verbleibenden ${mineLabel} enthalten müssen.`,
            teaching: {
              patternTitle: 'Muster',
              patternLabel: 'Subset-Minenunterschied',
              explanationTitle: 'Erklärung',
              explanation: `Die verborgenen Felder des kleineren Hinweises passen in die des größeren Hinweises. Nach Berücksichtigung der gemeinsamen Minenplätze müssen die extra verborgenen ${tileLabel} die verbleibenden ${mineLabel} enthalten.`,
            },
          };
        default:
          throw new Error(`Unhandled flag move pattern: ${reason satisfies never}`);
      }
    },
    guess: {
      title: 'Noch kein sicherer nächster Zug',
      body: 'Kein Hinweis zeigt jetzt auf eine sichere Aufdeckung. Diese Stelle braucht möglicherweise eine Vermutung, also vertraue deiner besten Einschätzung des Feldes und frage nach der nächsten Aufdeckung erneut.',
    },
  },
  tutorialUi: {
    progressLabel: (step: number) => `Lektion ${step}`,
    exitLabel: {
      end: 'Tutorial beenden',
      skip: 'Tutorial überspringen',
    },
    status: {
      finishing: 'Tutorial wird abgeschlossen…',
      nextLesson: 'Nächste Lektion startet…',
    },
    highlightedTile: 'Markiertes Tutorial-Feld',
  },
} as const;

export default de;
import type { LearningCenterMineFlagParams, LearningCenterPatternParams } from './index';
