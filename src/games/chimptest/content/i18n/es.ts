import type { ChimpTestI18n } from './index';

const es: ChimpTestI18n = {
  strings: {
    title: 'Chimp Test',
    shortTitle: 'Chimp',
    tagline: 'Toca los numeros en orden antes de que desaparezcan.',
    difficultyLabels: {
      easy: 'Facil',
      medium: 'Medio',
      hard: 'Dificil',
      expert: 'Experto',
    },
    difficultyDescriptions: {
      easy: 'Cuadricula 4×4. Recuerda hasta 7 numeros.',
      medium: 'Cuadricula 5×5. Recuerda hasta 9 numeros.',
      hard: 'Cuadricula 6×6. Recuerda hasta 11 numeros.',
      expert: 'Cuadricula 7×7. Recuerda hasta 13 numeros.',
    },
    play: {
      metadataLabels: {
        round: 'Ronda',
        difficulty: 'Dificultad',
      },
    },
  },
  howToPlayGoal: 'Toca cada numero en orden ascendente antes de que los anteriores desaparezcan de la vista.',
  howToPlayControls: 'Toca el siguiente numero en la secuencia. Cada numero tocado correctamente desaparece de inmediato, por lo que debes recordar donde estan los restantes.',
  howToPlayWrongMove: 'Tocar cualquier casilla que no sea el siguiente numero en la secuencia termina el puzzle de inmediato.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Estudia la cuadricula',
      body: 'Los numeros aparecen en posiciones aleatorias en cada ronda. Escanea toda la cuadricula antes de tocar.',
    },
    {
      num: '2',
      title: 'Toca en orden',
      body: 'Toca siempre el 1 primero, luego el 2, 3, y asi sucesivamente. No hay presion de tiempo mientras los numeros son visibles.',
    },
    {
      num: '3',
      title: 'Los numeros desaparecen al tocarlos',
      body: 'Cada toque correcto elimina ese numero de la cuadricula. Debes recordar las posiciones de los numeros restantes.',
    },
    {
      num: '4',
      title: 'Las rondas aumentan',
      body: 'Cada ronda exitosa anade un numero mas. Llega al numero maximo para resolver el puzzle.',
    },
  ],
  howToPlayTechniques: [],
  howToPlayTips: [
    {
      key: 'scan-first',
      title: 'Escanea antes de tocar',
      body: 'Tomatee un momento para trazar un camino por todos los numeros antes de tocar el 1. Una ruta mental rapida es mas eficiente que buscar en medio de la secuencia.',
    },
    {
      key: 'group',
      title: 'Agrupa los numeros cercanos',
      body: 'Observa los grupos de numeros consecutivos. Tocar los vecinos uno tras otro es mas rapido que saltar por toda la cuadricula.',
    },
  ],
  howToPlayScoring: 'La puntuacion se basa en el tiempo total en todas las rondas. Tocar mas rapido da una puntuacion mas alta.',
  loss: {
    abandoned: {
      eyebrow: 'Puzzle terminado',
      title: 'Puzzle sin terminar',
      body: 'Terminaste este puzzle antes de resolverlo.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle perdido',
      title: 'Puzzle perdido',
      body: 'Tocaste la casilla incorrecta.',
      icon: '⚠️',
    },
  },
};

export default es;
