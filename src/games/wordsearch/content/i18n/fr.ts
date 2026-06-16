import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Mots mêlés',
    shortTitle: 'Mots',
    tagline: 'Trace les mots en lignes droites et résous le mot bonus caché directement dans la grille.',
    tutorial: {
      ...en.strings.tutorial,
      exitLabel: {
        skip: 'Passer le tutoriel',
        end: 'Ouvrir Mots mêlés',
      },
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
      noPuzzlesDialog: {
        title: 'Mots mêlés non disponible',
        message: (difficultyLabel: string) => `Aucun puzzle de mots mêlés n'est prêt pour ${difficultyLabel} pour l'instant.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Résolvez le mot caché dans la grille dès que vous le souhaitez.',
        revealed: (clue: string, word: string) => `${clue} : ${word}`,
        enterMode: 'Résoudre le mot caché',
        exitMode: 'Quitter le mode mot caché',
        solvedTitle: "Mot caché trouvé !",
        instructions: "Les lettres du mot caché se trouvent dans les cases vides, de gauche à droite puis de haut en bas. Touchez-les dans l'ordre.",
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
        body: "Les lettres du mot caché remplissent les cases vides de gauche à droite et de haut en bas. Activez le mode mot caché et touchez chaque lettre dans l'ordre.",
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      title: 'Partie de mots mêlés terminée',
      body: "Vous avez quitté cette partie de mots mêlés avant d'avoir résolu tous les mots et le mot caché.",
    },
    'rule-failure': {
      ...en.loss['rule-failure'],
      title: 'Les mots mêlés n\'ont pas de défaite par règle',
      body: 'Les sélections invalides sont ignorées dans les mots mêlés. Continuez à scanner et poursuivez le puzzle.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    "win-condition": {
      title: "Trouvez le mot cache pour gagner",
      body: "Les mots listes sont des indices thematiques — ils vous aident a identifier le mot bonus cache. Trouver le mot cache est la seule facon de gagner.",
      summary: "Les mots normaux sont des indices. Le mot cache est l'objectif.",
      continueLabel: "Lecon suivante",
    },
    "selection": {
      title: "Touchez le debut, puis la fin",
      body: "Touchez la premiere lettre d'un mot, puis la derniere. Le jeu trace le chemin — horizontal, vertical, diagonal ou avec un seul angle.",
      summary: "Deux touches selectionnent un mot.",
      continueLabel: "Lecon suivante",
    },
    "no-penalty": {
      title: "Les mauvaises tentatives disparaissent",
      body: "Si aucun mot n'est trouve, la selection se reinitialise sans penalite. Scannez librement et essayez toutes les combinaisons de debut et de fin.",
      summary: "Rien a perdre — continuez a scanner.",
      continueLabel: "Lecon suivante",
    },
    "hidden-word": {
      title: "Trouvez le mot caché",
      body: "Touchez l'icône de clé pour passer en mode mot caché. Les lettres se trouvent dans les cases vides, de gauche à droite puis de haut en bas. La barre inférieure suit votre progression — touchez chaque lettre dans l'ordre pour gagner.",
      summary: "Les cases vides épèlent le mot caché dans l'ordre de lecture.",
      continueLabel: "Commencer la grille",
    },
  },
};

export default locale;
