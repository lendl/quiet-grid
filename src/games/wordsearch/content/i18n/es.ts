import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Sopa de Letras',
    shortTitle: 'Letras',
    tagline: 'Traza palabras en líneas rectas y resuelve la palabra oculta de bonificación directamente en la cuadrícula.',
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Sopa de Letras no disponible',
        message: (difficultyLabel: string) => `Todavía no hay ningún puzzle de Sopa de Letras listo para ${difficultyLabel}.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Encuentra todas las palabras para desbloquear la palabra oculta.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Resolver palabra oculta',
        exitMode: 'Salir del modo de palabra oculta',
        solvedTitle: '¡Palabra oculta encontrada!',
        instructions: 'Las letras de la palabra oculta están en las casillas vacías, de izquierda a derecha y de arriba a abajo. Tócalas en orden.',
        resetOnMistake: 'Una pulsación incorrecta en la palabra oculta reinicia tu progreso.',
        nextLetterTitle: (clue: string) => `Palabra oculta: ${clue}`,
        nextLetterBody: 'La casilla resaltada es la siguiente letra de la palabra oculta.',
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
        title: 'Resuelve la palabra oculta',
        body: 'Las letras de la palabra oculta llenan las casillas vacías de izquierda a derecha y de arriba a abajo. Una vez encontradas todas las palabras, toca cada letra en orden para ganar.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      title: 'Sesión de Sopa de Letras terminada',
      body: 'Terminaste esta sesión de Sopa de Letras antes de resolver todas las palabras y la palabra oculta.',
    },
    'rule-failure': {
      ...en.loss['rule-failure'],
      title: 'Sopa de Letras no tiene derrota por reglas',
      body: 'Las selecciones no válidas se ignoran en la Sopa de Letras. Sigue escaneando y continúa el puzzle.',
    },
  },
};

export default locale;
