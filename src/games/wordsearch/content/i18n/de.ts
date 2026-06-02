import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Ziehe Wörter in geraden Linien nach und löse das versteckte Bonuswort direkt im Raster.',
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Löse das versteckte Wort im Raster, sobald du bereit bist.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Verstecktes Wort lösen',
        exitMode: 'Modus für verstecktes Wort verlassen',
        progress: (current: number, total: number) => `Verstecktes Wort ${current}/${total}`,
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
    'hidden-word': {
      title: 'In den Modus für das versteckte Wort wechseln',
      body: 'Nutze während des Spiels die Aktion für das versteckte Wort und tippe dann die Buchstaben im Live-Raster der Reihe nach an.',
      summary: 'Das versteckte Wort wird direkt während des Spiels gelöst.',
      continueLabel: 'Puzzle starten',
    },
  },
};

export default locale;
