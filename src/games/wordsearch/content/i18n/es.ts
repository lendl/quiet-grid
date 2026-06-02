import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Traza palabras en líneas rectas y resuelve la palabra oculta de bonificación directamente en la cuadrícula.',
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Resuelve la palabra oculta desde la cuadrícula cuando quieras.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Resolver palabra oculta',
        exitMode: 'Salir del modo de palabra oculta',
        progress: (current: number, total: number) => `Palabra oculta ${current}/${total}`,
        instructions: 'Toca las letras de la palabra oculta en orden sobre la cuadrícula.',
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
        body: 'Activa el modo de palabra oculta durante la partida y luego toca sus letras en orden sobre la cuadrícula.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      body: 'Terminaste esta sesión de Sopa de Letras antes de resolver todas las palabras y la palabra oculta.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    'hidden-word': {
      title: 'Cambia al modo de palabra oculta',
      body: 'Usa la acción de palabra oculta durante la partida y luego toca sus letras en orden sobre la cuadrícula activa.',
      summary: 'La palabra oculta se resuelve directamente durante la partida.',
      continueLabel: 'Empezar puzzle',
    },
  },
};

export default locale;
