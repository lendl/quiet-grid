const es = {
  strings: {
    title: 'Sudoku',
    shortTitle: 'Sudoku',
    tagline: 'Coloca los dígitos del 1 al 9 para que cada fila, columna y caja siga siendo válida.',
    difficultyLabels: {
      easy: 'Fácil',
      medium: 'Medio',
      hard: 'Difícil',
      expert: 'Experto',
    },
    difficultyDescriptions: {
      easy: 'Cuadrículas iniciales pensadas para resolver con singles primero.',
      medium: 'Cuadrículas equilibradas que pueden crecer hacia técnicas apoyadas por notas.',
      hard: 'Cuadrículas escasas reservadas para análisis y validación más ricos.',
      expert: 'Cuadrículas muy escasas reservadas para deducciones avanzadas con cadenas y coloreado.',
    },
    play: {
      metadataLabels: {
        size: 'Tamaño',
        difficulty: 'Dificultad',
        filled: 'Relleno',
      },
      noPuzzlesDialog: {
        title: 'Sudoku no disponible',
        message: (difficultyLabel: string) => `Todavía no hay un puzzle de Sudoku listo para ${difficultyLabel}.`,
      },
      cellLabel: 'Celda',
      resetZoom: 'Restablecer zoom',
      helperToggle: {
        show: 'Mostrar siguiente movimiento',
        hide: 'Ocultar siguiente movimiento',
      },
      controls: {
        noteModeEnabled: 'Modo notas activado',
        noteModeDisabled: 'Modo notas desactivado',
        selectedCellPrompt: 'Selecciona una celda para introducir un dígito o una nota.',
        selectedCellLabel: (cellLabel: string) => `Seleccionada: ${cellLabel}`,
        digitButtonLabel: (digit: number) => `Usar el dígito ${digit} en la celda seleccionada`,
        noteDigitLabel: (digit: number) => `Alternar la nota ${digit} en la celda seleccionada`,
      },
      nextMove: {
        invalidConflictTitle: 'El tablero necesita una corrección primero',
        invalidConflictBody: (houseLabel: string, digit: number) => (
          `El dígito ${digit} aparece más de una vez en ${houseLabel}. Corrige ese conflicto antes de pedir el siguiente movimiento.`
        ),
        invalidDeadCellTitle: 'El tablero necesita una corrección primero',
        invalidDeadCellBody: (cellLabel: string) => (
          `${cellLabel} ya no tiene ningún dígito válido disponible. Corrige las entradas cercanas antes de pedir el siguiente movimiento.`
        ),
        placementTitle: (techniqueLabel: string, digit: number) => `${techniqueLabel}: colocar ${digit}`,
        nakedSingleBody: (digit: number, cellLabel: string) => (
          `Solo el dígito ${digit} encaja en ${cellLabel} después de revisar su fila, su columna y su caja.`
        ),
        hiddenSingleBody: (digit: number, houseLabel: string, cellLabel: string) => (
          `El dígito ${digit} solo encaja en una celda dentro de ${houseLabel}, así que colócalo en ${cellLabel}.`
        ),
        placementBody: (techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string) => (
          `${techniqueLabel} fuerza el dígito ${digit} en ${cellLabel}. Usa las ${houseLabels} resaltadas para confirmarlo.`
        ),
        eliminationTitle: (techniqueLabel: string, digitsLabel: string) => `${techniqueLabel}: eliminar ${digitsLabel}`,
        lockedCandidatesBody: (
          digitsLabel: string,
          sourceHouseLabel: string,
          targetHouseLabel: string,
        ) => (
          `Los dígitos ${digitsLabel} están bloqueados dentro de ${sourceHouseLabel}, así que quítalos de las demás celdas resaltadas en ${targetHouseLabel}.`
        ),
        eliminationBody: (
          techniqueLabel: string,
          digitsLabel: string,
          targetLabels: string,
          houseLabels: string,
        ) => (
          `${techniqueLabel} elimina ${digitsLabel} de ${targetLabels}. Las ${houseLabels} resaltadas ya no dejan un lugar válido para esos candidatos ahí.`
        ),
        unsupportedTitle: 'Todavía no hay un siguiente movimiento compatible',
        unsupportedBody: 'Esta posición puede necesitar una técnica más profunda de la que Sudoku enseña ahora mismo en Quiet Grid.',
      },
    },
    tutorial: {
      exitLabel: {
        skip: 'Omitir tutorial',
        end: 'Abrir Sudoku',
      },
      controlLabel: 'Juego en vivo',
      progressLabel: (current: number, total: number) => `Lección ${current} de ${total}`,
      status: {
        nextLesson: 'Bien. Pasando a la siguiente lección…',
        finishing: 'Bien. Abriendo Sudoku…',
      },
      lessons: {
        goal: {
          title: 'Rellena cada fila, columna y caja',
          body: 'Sudoku se resuelve cuando cada fila, cada columna y cada caja de 3×3 usan los dígitos del 1 al 9 exactamente una vez. Los dígitos dados permanecen fijos.',
          summary: 'Empieza cada revisión comprobando una sola casa cada vez: fila, columna o caja.',
          controlHint: 'Juego en vivo: toca una celda y luego usa los dígitos de la barra. Vuelve a tocar el mismo dígito para borrarlo.',
          continueLabel: 'Mostrar el primer movimiento',
        },
        'naked-single': {
          title: 'Ya hay un naked single listo',
          body: 'La celda resaltada ya solo tiene un dígito válido después de revisar su fila, su columna y su caja.',
          summary: 'Cuando una celda tiene un solo dígito legal, colócalo de inmediato.',
          controlHint: 'Juego en vivo: sigue en modo dígitos, toca la celda resaltada y luego toca el dígito 4 en la barra.',
          prompt: '¿Qué dígito va en la fila 1, columna 3?',
          options: {
            '4': '4',
            '8': '8',
          },
          correctOptionKey: '4',
          correctFeedback: 'Correcto. El dígito 4 es el único candidato que queda para esa celda.',
          wrongFeedback: 'Inténtalo otra vez. Revisa la fila, la columna y la caja juntas antes de colocar un dígito.',
        },
        'notes-mode': {
          title: 'Usa notas antes de adivinar',
          body: 'Esta celda resaltada todavía tiene más de un candidato válido, así que aún no está lista para un dígito definitivo.',
          summary: 'Las notas son una herramienta de apoyo de primera clase: marca candidatos antes de fijar un valor.',
          controlHint: 'Juego en vivo: toca el lápiz para cambiar al modo notas y luego toca un dígito de la barra para alternar esa nota.',
          prompt: '¿Qué modo deberías usar ahora para la fila 6, columna 2?',
          options: {
            digit: 'Dígitos',
            notes: 'Notas',
          },
          correctOptionKey: 'notes',
          correctFeedback: 'Correcto. Esta celda primero necesita notas antes de estar lista para un dígito final.',
          wrongFeedback: 'Todavía no. El modo dígitos fija un valor, pero esta celda aún tiene varios candidatos válidos.',
        },
        'hidden-single': {
          title: 'Las notas pueden revelar un hidden single',
          body: 'La fila resaltada muestra varias notas candidatas, pero solo una celda puede seguir aceptando el dígito 5.',
          summary: 'Un hidden single aparece cuando un candidato solo queda en una celda dentro de una casa.',
          controlHint: 'Juego en vivo: después de detectar el hidden single, apaga el lápiz y toca el dígito 5 en la barra.',
          prompt: 'Después de leer las notas, ¿qué dígito debe ir en la fila 6, columna 2?',
          options: {
            '4': '4',
            '5': '5',
          },
          correctOptionKey: '5',
          correctFeedback: 'Correcto. El dígito 5 solo aparece una vez en las notas de la fila resaltada, así que debe ir ahí.',
          wrongFeedback: 'Inténtalo otra vez. A la fila todavía le falta un 5 y esta es la única celda que puede llevarlo.',
        },
      },
    },
    learning: {
      labels: {
        cell: (row: number, col: number) => `fila ${row}, columna ${col}`,
        row: (index: number) => `fila ${index}`,
        column: (index: number) => `columna ${index}`,
        box: (index: number) => `caja ${index}`,
        joinList: (items: string[]) => {
          if (items.length <= 1) {
            return items[0] ?? '';
          }
          if (items.length === 2) {
            return `${items[0]} y ${items[1]}`;
          }
          return `${items.slice(0, -1).join(', ')} y ${items[items.length - 1]}`;
        },
      },
      techniqueLabels: {
        'naked-single': 'Naked single',
        'hidden-single': 'Hidden single',
        'naked-pair': 'Naked pair',
        'hidden-pair': 'Hidden pair',
        'pointing-pair-triple': 'Pointing pair/triple',
        'box-line-reduction': 'Reducción caja-línea',
        'x-wing': 'X-Wing',
        'swordfish': 'Swordfish',
        'xy-wing': 'XY-Wing',
        'xyz-wing': 'XYZ-Wing',
        coloring: 'Coloreado',
        chains: 'Cadenas',
      },
      analyzer: {
        legend: {
          evidence: 'Evidencia',
          place: 'Colocar dígito',
          eliminate: 'Borrar nota',
        },
      },
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Rellena la cuadrícula',
        body: 'Coloca los dígitos del 1 al 9 para que cada fila, columna y caja de 3×3 use cada dígito una sola vez.',
      },
      {
        num: '2',
        title: 'Respeta las pistas dadas',
        body: 'Las pistas iniciales permanecen fijas y sostienen cualquier sesión válida de Sudoku.',
      },
      {
        num: '3',
        title: 'Usa notas cuando una celda aún no esté lista',
        body: 'Las notas son acciones de apoyo opcionales, pero te ayudan a seguir los candidatos antes de fijar un dígito final.',
      },
    ],
    tips: [
      {
        key: 'scan-rows',
        title: 'Revisa una sola casa cada vez',
        body: 'Elige una fila, una columna o una caja y pregúntate qué dígitos faltan todavía. Las comprobaciones pequeñas y locales son más fáciles de confiar que las suposiciones amplias.',
        example: [
          [5, 3, null],
          [6, 7, 2],
          [1, 9, 8],
        ],
      },
      {
        key: 'notes-first',
        title: 'Las notas mantienen honestas a las celdas difíciles',
        body: 'Si una celda todavía tiene varios dígitos legales, cambia al modo notas en lugar de adivinar. Las notas ayudan a que los hidden singles y las deducciones por pares destaquen después.',
        example: [
          [null, '4·5', 3],
          ['1·4·7', '4·7', 6],
          ['1·7', 8, 2],
        ],
      },
    ],
  },
  loss: {
    abandoned: {
      eyebrow: 'Puzzle terminado',
      title: 'Sesión de Sudoku terminada',
      body: 'Terminaste esta sesión de Sudoku antes de completar la cuadrícula.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle bloqueado',
      title: 'El tablero de Sudoku se volvió inválido',
      body: 'Al menos una fila, columna o caja ahora entra en conflicto con las reglas del Sudoku. Corrige el conflicto antes de pedir el siguiente movimiento.',
      icon: '⚠️',
    },
  },
} as const;

export default es;
