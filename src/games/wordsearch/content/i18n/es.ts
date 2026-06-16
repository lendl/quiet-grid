import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Sopa de Letras',
    shortTitle: 'Letras',
    tagline: 'Traza palabras en líneas rectas y resuelve la palabra oculta de bonificación directamente en la cuadrícula.',
    tutorial: {
      ...en.strings.tutorial,
      exitLabel: {
        skip: 'Saltar tutorial',
        end: 'Abrir Sopa de Letras',
      },
      checkpoint: {
        prompt: '¿Quieres seleccionar STAR. Después de tocar S, ¿qué tocas a continuación?',
        validOption: 'R — la última letra',
        invalidOption: 'T — la siguiente letra',
        correctFeedback: 'Correcto — toca la primera letra, luego la última. El camino se traza automáticamente.',
        wrongFeedback: 'No del todo. Toca solo la primera y la última letra, no cada letra entre ellas.',
      },
    },
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Sopa de Letras no disponible',
        message: (difficultyLabel: string) => `Todavía no hay ningún puzzle de Sopa de Letras listo para ${difficultyLabel}.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Resuelve la palabra oculta desde la cuadrícula cuando quieras.',
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
        body: 'Las letras de la palabra oculta llenan las casillas vacías de izquierda a derecha y de arriba a abajo. Activa el modo de palabra oculta y toca cada letra en orden.',
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
  tutorialLessons: {
    ...en.tutorialLessons,
    'win-condition': {
      title: 'Encuentra la palabra oculta para ganar',
      body: 'Las palabras de la lista son pistas temáticas — te ayudan a identificar la palabra oculta de bonificación. Encontrar la palabra oculta es la única forma de ganar.',
      summary: 'Las palabras normales son pistas. La palabra oculta es el objetivo.',
      continueLabel: 'Siguiente lección',
    },
    'selection': {
      title: 'Toca el inicio, luego el final',
      body: 'Toca la primera letra de una palabra, luego la última. El juego traza el camino — horizontal, vertical, diagonal o con un giro.',
      summary: 'Dos toques seleccionan una palabra.',
      continueLabel: 'Siguiente lección',
    },
    'no-penalty': {
      title: 'Los intentos fallidos desaparecen',
      body: 'Si no se encuentra ninguna palabra, la selección se reinicia sin penalización. Escanea libremente y prueba cualquier combinación de inicio y final.',
      summary: 'Nada que perder — sigue escaneando.',
      continueLabel: 'Siguiente lección',
    },
    'hidden-word': {
      title: 'Encuentra la palabra oculta',
      body: 'Toca el icono de llave para entrar en el modo de palabra oculta. Las letras están en las casillas vacías, de izquierda a derecha y de arriba a abajo. La barra inferior sigue tu progreso — toca cada letra en orden para ganar.',
      summary: 'Las casillas vacías deletrean la palabra oculta en orden de lectura.',
      continueLabel: 'Empezar puzzle',
    },
  },
};

export default locale;
