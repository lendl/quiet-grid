import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Mots mêlés',
    shortTitle: 'Mots',
    tagline: 'Trace les mots en lignes droites et résous le mot bonus caché directement dans la grille.',
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Mots mêlés non disponible',
        message: (difficultyLabel: string) => `Aucun puzzle de mots mêlés n'est prêt pour ${difficultyLabel} pour l'instant.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Trouvez tous les mots pour débloquer le mot caché.',
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
        body: "Les lettres du mot caché remplissent les cases vides de gauche à droite et de haut en bas. Une fois tous les mots trouvés, touchez chaque lettre dans l'ordre pour gagner.",
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
};

export default locale;
