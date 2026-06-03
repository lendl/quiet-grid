import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Ziehe Wörter in geraden Linien nach und löse das versteckte Bonuswort direkt im Raster.',
    tutorial: {
      ...en.strings.tutorial,
      checkpoint: {
        prompt: 'Du möchtest STAR auswählen. Was tippst du nach dem S als Nächstes?',
        validOption: 'R — den letzten Buchstaben',
        invalidOption: 'T — den nächsten Buchstaben',
        correctFeedback: 'Richtig — tippe den ersten Buchstaben, dann den letzten. Der Pfad wird automatisch gezogen.',
        wrongFeedback: 'Nicht ganz. Tippe nur den ersten und letzten Buchstaben — nicht jeden Buchstaben dazwischen.',
      },
    },
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Löse das versteckte Wort im Raster, sobald du bereit bist.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Verstecktes Wort lösen',
        exitMode: 'Modus für verstecktes Wort verlassen',
        solvedTitle: 'Verstecktes Wort gefunden!',
        instructions: 'Tippe die Buchstaben des versteckten Worts in der richtigen Reihenfolge im Raster an.',
        resetOnMistake: 'Ein falscher Tipp auf das versteckte Wort setzt deinen Fortschritt zurück.',
        nextLetterTitle: (clue: string) => `Verstecktes Wort: ${clue}`,
        nextLetterBody: 'Die markierte Zelle ist der nächste Buchstabe des versteckten Worts.',
      },
    },
  },
  howToPlay: {
    ...en.howToPlay,
    rules: [
      en.howToPlay.rules[0],
      en.howToPlay.rules[1],
      {
        num: '3',
        title: 'Löse das versteckte Wort',
        body: 'Nutze während des Spiels den Modus für das versteckte Wort und tippe dann die Buchstaben im Raster der Reihe nach an.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      body: 'Du hast diese Wortsuche beendet, bevor du alle Wörter und das versteckte Wort gelöst hast.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    'win-condition': {
      title: 'Finde das versteckte Wort, um zu gewinnen',
      body: 'Die aufgelisteten Wörter sind Themen-Hinweise — sie helfen dir, das versteckte Bonuswort zu identifizieren. Das versteckte Wort zu finden ist die einzige Möglichkeit zu gewinnen.',
      summary: 'Normale Wörter sind Hinweise. Das versteckte Wort ist das Ziel.',
      continueLabel: 'Nächste Lektion',
    },
    'selection': {
      title: 'Tippe Anfang, dann Ende',
      body: 'Tippe den ersten Buchstaben eines Worts, dann den letzten. Das Spiel zeichnet den Pfad — waagerecht, senkrecht, diagonal oder mit einer Kurve.',
      summary: 'Zwei Tipps wählen ein Wort aus.',
      continueLabel: 'Nächste Lektion',
    },
    'no-penalty': {
      title: 'Falsche Versuche verschwinden einfach',
      body: 'Wird kein Wort gefunden, wird die Auswahl zurückgesetzt — ohne Strafe. Scanne frei und probiere jede Kombination aus Anfang und Ende.',
      summary: 'Nichts zu verlieren — weiter scannen.',
      continueLabel: 'Nächste Lektion',
    },
    'hidden-word': {
      title: 'Finde das versteckte Wort',
      body: 'Tippe auf das Schlüssel-Symbol, um den Modus für das versteckte Wort zu aktivieren. Die Buchstaben sind überall im Raster verteilt. Die untere Leiste zeigt jeden Buchstaben beim Tippen — finde alle, um zu gewinnen.',
      summary: 'Der Fortschritt unten führt dich Buchstabe für Buchstabe.',
      continueLabel: 'Puzzle starten',
    },
  },
};

export default locale;
