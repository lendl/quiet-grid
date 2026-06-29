import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Wortsuche',
    shortTitle: 'Wörter',
    tagline: 'Ziehe Wörter in geraden Linien nach und löse das versteckte Bonuswort direkt im Raster.',
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Wortsuche nicht verfügbar',
        message: (difficultyLabel: string) => `Noch kein Wortsuche-Rätsel für ${difficultyLabel} verfügbar.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Finde alle Wörter, um das versteckte Wort freizuschalten.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Verstecktes Wort lösen',
        exitMode: 'Modus für verstecktes Wort verlassen',
        solvedTitle: 'Verstecktes Wort gefunden!',
        instructions: 'Die Buchstaben des versteckten Worts befinden sich in den leeren Feldern, von links nach rechts und von oben nach unten. Tippe sie der Reihe nach an.',
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
        body: 'Die Buchstaben des versteckten Worts füllen die leeren Felder von links nach rechts und von oben nach unten. Sobald alle Wörter gefunden sind, tippe die Buchstaben der Reihe nach an, um zu gewinnen.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      title: 'Wortsuche-Sitzung beendet',
      body: 'Du hast diese Wortsuche beendet, bevor du alle Wörter und das versteckte Wort gelöst hast.',
    },
    'rule-failure': {
      ...en.loss['rule-failure'],
      title: 'Wortsuche hat keinen Regelverlust',
      body: 'Ungültige Auswahlen werden in der Wortsuche ignoriert. Weiter scannen und das Rätsel fortsetzen.',
    },
  },
};

export default locale;
