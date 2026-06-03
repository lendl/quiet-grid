import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Trace les mots en lignes droites et résous le mot bonus caché directement dans la grille.',
    tutorial: {
      ...en.strings.tutorial,
      checkpoint: {
        prompt: "Vous souhaitez sélectionner STAR. Après avoir touché S, que touchez-vous ensuite ?",
        validOption: "R — la dernière lettre",
        invalidOption: "T — la lettre suivante",
        correctFeedback: "Correct — touchez la première lettre, puis la dernière. Le chemin est tracé automatiquement.",
        wrongFeedback: "Pas tout à fait. Touchez uniquement la première et la dernière lettre, pas chaque lettre entre les deux.",
      },
    },
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Résolvez le mot caché dans la grille dès que vous le souhaitez.',
        revealed: (clue: string, word: string) => `${clue} : ${word}`,
        enterMode: 'Résoudre le mot caché',
        exitMode: 'Quitter le mode mot caché',
        solvedTitle: "Mot caché trouvé !",
        instructions: "Touchez les lettres du mot caché dans l’ordre sur la grille.",
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
        body: "Activez le mode mot caché pendant la partie, puis touchez ses lettres dans l’ordre sur la grille.",
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      body: "Vous avez quitté cette partie de mots mêlés avant d’avoir résolu tous les mots et le mot caché.",
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    "win-condition": {
      title: "Trouvez le mot cache pour gagner",
      body: "Les mots listes sont des indices thematiques — ils vous aident a identifier le mot bonus cache. Trouver le mot cache est la seule facon de gagner.",
      summary: "Les mots normaux sont des indices. Le mot cache est l’objectif.",
      continueLabel: "Lecon suivante",
    },
    "selection": {
      title: "Touchez le debut, puis la fin",
      body: "Touchez la premiere lettre d’un mot, puis la derniere. Le jeu trace le chemin — horizontal, vertical, diagonal ou avec un seul angle.",
      summary: "Deux touches selectionnent un mot.",
      continueLabel: "Lecon suivante",
    },
    "no-penalty": {
      title: "Les mauvaises tentatives disparaissent",
      body: "Si aucun mot n’est trouve, la selection se reinitialise sans penalite. Scannez librement et essayez toutes les combinaisons de debut et de fin.",
      summary: "Rien a perdre — continuez a scanner.",
      continueLabel: "Lecon suivante",
    },
    "hidden-word": {
      title: "Trouvez le mot cache",
      body: "Touchez l’icone de cle pour passer en mode mot cache. Les lettres sont dispersees partout dans la grille. La barre inferieure montre chaque lettre au fil des touches — trouvez-les toutes pour gagner.",
      summary: "La progression en bas vous guide lettre par lettre.",
      continueLabel: "Commencer la grille",
    },
  },
};

export default locale;
