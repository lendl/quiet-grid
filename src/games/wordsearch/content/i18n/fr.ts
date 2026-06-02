import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Trace les mots en lignes droites et résous le mot bonus caché directement dans la grille.',
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Résolvez le mot caché dans la grille dès que vous le souhaitez.',
        revealed: (clue: string, word: string) => `${clue} : ${word}`,
        enterMode: 'Résoudre le mot caché',
        exitMode: 'Quitter le mode mot caché',
        progress: (current: number, total: number) => `Mot caché ${current}/${total}`,
        instructions: 'Touchez les lettres du mot caché dans l’ordre sur la grille.',
        resetOnMistake: 'Une mauvaise case du mot caché réinitialise votre progression.',
        nextLetterTitle: (clue: string) => `Mot caché : ${clue}`,
        nextLetterBody: 'La case mise en évidence est la prochaine lettre du mot caché.',
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
        title: 'Résoudre le mot caché',
        body: 'Activez le mode mot caché pendant la partie, puis touchez ses lettres dans l’ordre sur la grille.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      body: 'Vous avez quitté cette partie de mots mêlés avant d’avoir résolu tous les mots et le mot caché.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    'hidden-word': {
      title: 'Passer en mode mot caché',
      body: 'Utilisez l’action du mot caché pendant la partie, puis touchez ses lettres dans l’ordre sur la grille active.',
      summary: 'Le mot caché se résout directement pendant la partie.',
      continueLabel: 'Commencer la grille',
    },
  },
};

export default locale;
