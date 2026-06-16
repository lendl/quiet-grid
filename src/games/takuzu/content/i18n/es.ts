const es = {
  strings: {
    title: 'Takuzu',
    shortTitle: 'Takuzu',
    tagline: 'Rellena la cuadrícula con 0s y 1s usando lógica.',
    difficultyLabels: {
      easy: 'Fácil',
      medium: 'Medio',
      hard: 'Difícil',
      expert: 'Experto',
    },
    difficultyDescriptions: {
      easy: 'Más celdas iniciales y deducciones más suaves.',
      medium: 'Aperturas equilibradas que piden leer la cuadrícula un poco más hacia adelante.',
      hard: 'Configuraciones más ajustadas con menos información libre y mayor trabajo de patrones.',
      expert: 'Aperturas escasas con presión de deducción sostenida en todo el puzzle.',
    },
    play: {
      metadataLabels: {
        size: 'Tamaño',
        difficulty: 'Dificultad',
      },
      helperToggle: {
        show: 'Mostrar siguiente movimiento',
        hide: 'Ocultar siguiente movimiento',
      },
      noPuzzlesDialog: {
        title: 'No hay puzzles disponibles',
        message: (difficultyLabel: string) => `No se encontraron puzzles en el catálogo ${difficultyLabel}.`,
      },
      cellLabel: 'Celda',
      tutorial: {
        progressLabel: (step: number) => `Lección ${step}`,
        introNote: 'Objetivo: rellena toda la cuadrícula de modo que cada fila y columna permanezca equilibrada, única y sin trillizos. En el juego real, toca la celda resaltada para ciclar entre vacío, 0, 1 y vacío de nuevo. En este tutorial, usa los botones 0 y 1 de abajo.',
        exitLabel: {
          end: 'Terminar tutorial',
          skip: 'Omitir tutorial',
        },
        status: {
          finishing: 'Tutorial finalizando…',
          nextLesson: 'Siguiente lección iniciando…',
          nextStep: 'Siguiente paso iniciando…',
        },
        selectAnswerLabel: (value: 0 | 1) => `Seleccionar ${value}`,
      },
    },
  },
  howToPlayGoal: 'Rellena cada celda con un 0 o un 1 siguiendo tres reglas.',
  howToPlayControls: 'Toca una celda para ciclar: vacío → 0 → 1 → vacío.',
  howToPlayWrongMove: 'Completar una fila o columna que incumple una regla la hace parpadear y cuesta 500 puntos.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Sin tres seguidos',
      body: 'Tres dígitos idénticos seguidos en una fila o columna no están permitidos.',
    },
    {
      num: '2',
      title: 'Mitades iguales',
      body: 'Cada fila y cada columna contiene el mismo número de 0s y 1s.',
    },
    {
      num: '3',
      title: 'Todas las líneas son únicas',
      body: 'No puede haber dos filas idénticas, ni dos columnas idénticas.',
    },
  ],
  howToPlayTechniques: [
    {
      key: 'find-pairs',
      title: 'Encontrar pares',
      body: 'Dos dígitos iguales uno al lado del otro obligan a las celdas de ambos lados a ser el dígito opuesto.',
    },
    {
      key: 'avoid-trios',
      title: 'Evitar trillizos',
      body: 'Cuando el mismo dígito flanquea una celda vacía, esa celda debe ser el dígito opuesto.',
    },
    {
      key: 'complete-lines',
      title: 'Completar filas y columnas',
      body: 'Cuando una fila o columna ya tiene todos los 0s (o 1s) que puede contener, las celdas restantes deben ser el otro dígito.',
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Eliminar por líneas rellenas',
      body: 'Una fila o columna no puede ser copia de una ya completa. Si lo fuera, usa el valor opuesto.',
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Eliminar combinaciones imposibles',
      body: 'Si un valor terminaría forzando tres seguidos, el otro valor debe ir aquí.',
    },
  ],
  howToPlayScoring: 'Empieza en 10.000 y baja mientras el cronómetro corre. Cada línea completada incorrectamente resta 500 puntos. El marcador baja más lentamente en dificultades altas.',
  howToPlayTips: [
    {
      key: 'watch-for-flashes',
      title: 'Observar los destellos',
      body: 'Cuando completas correctamente una fila o columna, todas sus celdas parpadean brevemente como confirmación.',
    },
  ],
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
      body: 'Este puzzle terminó antes de poder resolverse.',
      icon: '⚠️',
    },
  },
  tutorialLessons: {
    'find-pairs': {
      title: 'Lección 1: Encontrar pares',
      body: 'Dos dígitos idénticos adyacentes significan que las celdas a cada lado deben ser el dígito opuesto.',
      prompt: '¿La celda resaltada debe ser un 1 o un 0?',
      retry: 'No esta. Si la celda resaltada fuera 1, la fila comenzaría con tres 1s seguidos.',
      success: 'Correcto. Dos 1s ya están juntos, así que la celda resaltada debe ser 0.',
    },
    'avoid-trios': {
      title: 'Lección 2: Evitar trillizos',
      body: 'Si el mismo dígito aparece con una celda vacía entre ellos, esa celda central debe ser el dígito opuesto.',
      prompt: '¿La celda resaltada debe ser un 1 o un 0?',
      retry: 'No esta. La celda resaltada está entre dos 1s, por lo que no puede ser también 1.',
      success: 'Correcto. La celda central debe ser 0 para que la fila no forme tres 1s seguidos.',
    },
    'complete-lines': {
      title: 'Lección 3: Completar filas y columnas',
      body: 'Una vez alcanzado el número máximo de un dígito en una línea, todas las celdas vacías restantes deben ser el otro dígito.',
      prompt: '¿La celda resaltada debe ser un 1 o un 0?',
      retry: 'No esta. Esta fila ya tiene todos los 0s que puede contener, así que la celda restante debe ser 1.',
      success: 'Correcto. La fila ya tiene tres 0s, así que la celda restante debe ser 1.',
    },
    'eliminate-filled-lines': {
      title: 'Lección 4: Eliminar por líneas rellenas',
      body: 'Si rellenar una fila o columna la haría idéntica a una ya completa, esos valores deben intercambiarse.',
      prompt: '¿La celda resaltada debe ser un 1 o un 0?',
      retry: 'No esta. Esa elección haría que la fila inferior coincidiera con la fila completa de arriba.',
      success: 'Correcto. Intercambiar este valor mantiene la fila inferior diferente de la fila completa.',
    },
    'eliminate-impossible-combinations': {
      title: 'Lección 5: Eliminar combinaciones imposibles',
      body: 'Mira la celda resaltada y los espacios vacíos restantes en la fila. Usa el patrón para determinar qué valor encaja ahí.',
      prompt: '¿La celda resaltada debe ser un 1 o un 0?',
      retry: 'No esta. Si la celda resaltada fuera 1, los espacios vacíos restantes forzarían un trillizo.',
      success: 'Correcto. Elegir 0 evita el trillizo que un 1 forzaría más adelante en la fila.',
    },
  },
  learningCenter: {
    pausedNextMove: {
      title: 'Aún no hay un movimiento claro',
      body: 'Esta parte del puzzle no ofrece un movimiento fuerte ahora mismo. Prueba otra fila o columna y vuelve a preguntar.',
    },
    findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Siguiente movimiento en ${lineLabel}`,
        body: `Coloca ${targetValue} en la ${cellLabel} resaltada. Por qué: dos ${repeatedValue}s ya están juntos en ${lineLabel}, por lo que otro ${repeatedValue} crearía tres seguidos.`,
      };
    },
    avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Siguiente movimiento en ${lineLabel}`,
        body: `Coloca ${targetValue} en la celda resaltada. Por qué: ${lineLabel} ya muestra ${repeatedValue} _ ${repeatedValue}, así que la celda abierta entre ellos debe ser ${targetValue} para evitar tres seguidos.`,
      };
    },
    completeLines(lineLabel: string, filledValue: 0 | 1, filledCount: number, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Siguiente movimiento en ${lineLabel}`,
        body: `Coloca ${targetValue} en la ${cellLabel} resaltada. Por qué: ${lineLabel} ya tiene ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, así que la ${cellLabel} abierta restante debe ser ${targetValue} para mantener la línea equilibrada.`,
      };
    },
    eliminateFilledLines(lineLabel: string, matchingLineLabel: string, targetValue: 0 | 1, cellLabel: string, lineKindLabel: string) {
      return {
        title: `Siguiente movimiento en ${lineLabel}`,
        body: `Coloca ${targetValue} en la ${cellLabel} resaltada. Por qué: si ${lineLabel} coincidiera con ${matchingLineLabel}, las ${lineKindLabel} completas dejarían de ser únicas.`,
      };
    },
    eliminateImpossible(
      lineLabel: string,
      validCompletionCount: number,
      blockedValue: 0 | 1,
      targetValue: 0 | 1,
      cellLabel: string,
      contradictionKind: 'triple' | 'balance' | 'duplicate-line',
      contradictionLineLabel: string,
      proofRuleLabel: string,
    ) {
      const contradictionLabel =
        contradictionKind === 'triple'
          ? 'una contradicción de tres seguidos'
          : contradictionKind === 'balance'
            ? 'una contradicción de equilibrio'
            : 'una contradicción por línea completa duplicada';

      return {
        title: `Siguiente movimiento en ${lineLabel}`,
        body: `Coloca ${targetValue} en la ${cellLabel} resaltada. Por qué: si esta ${cellLabel} fuera ${blockedValue}, seguir ${proofRuleLabel} provocaría ${contradictionLabel} en ${contradictionLineLabel}, así que ${targetValue} queda forzado aquí. ${lineLabel} todavía tiene ${validCompletionCount} compleción${validCompletionCount === 1 ? '' : 'es'} de línea válida${validCompletionCount === 1 ? '' : 's'} para comparar, pero solo este valor evita esa contradicción.`,
      };
    },
    avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1) {
      return {
        title: `Siguiente movimiento para reparar ${lineLabel}`,
        body: `Cambia una celda resaltada en ${lineLabel}. Por qué: tres ${repeatedValue}s seguidos infringen la regla de sin trillizos.`,
      };
    },
    completeLinesRepair(lineLabel: string, filledValue: 0 | 1, filledCount: number, limit: number) {
      return {
        title: `Siguiente movimiento para reequilibrar ${lineLabel}`,
        body: `Cambia una celda resaltada en ${lineLabel}. Por qué: ${lineLabel} ya contiene ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, pero el límite es ${limit}.`,
      };
    },
    eliminateFilledLinesRepair(firstLineLabel: string, secondLineLabel: string, lineLabel: string) {
      return {
        title: `Siguiente movimiento para separar las ${lineLabel} coincidentes`,
        body: `Cambia una celda resaltada. Por qué: ${firstLineLabel} y ${secondLineLabel} coinciden, pero las ${lineLabel} completas deben seguir siendo únicas.`,
      };
    },
    lineLabel(lineKind: 'row' | 'column', index: number) {
      return `${lineKind === 'row' ? 'fila' : 'columna'} ${index + 1}`;
    },
    cellLabel(count: number) {
      return count === 1 ? 'celda' : 'celdas';
    },
    lineKindLabel(lineKind: 'row' | 'column', count: number) {
      if (lineKind === 'row') return count === 1 ? 'fila' : 'filas';
      return count === 1 ? 'columna' : 'columnas';
    },
    ruleLabel(rule: 'find-pairs' | 'avoid-trios' | 'complete-lines' | 'eliminate-filled-lines' | null) {
      switch (rule) {
        case 'find-pairs': return 'la regla de las parejas';
        case 'avoid-trios': return 'la regla de evitar tríos';
        case 'complete-lines': return 'la regla de completar líneas';
        case 'eliminate-filled-lines': return 'la regla de eliminar líneas iguales';
        default: return 'las reglas';
      }
    },
  },
} as const;

export default es;
