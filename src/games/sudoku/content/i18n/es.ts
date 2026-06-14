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
        explainButton: 'Explicar este movimiento',
      },
      techniqueLesson: {
        backButton: 'Volver al juego',
        explanations: {
          'hidden-single': 'A hidden single means only one cell in a house can legally hold a particular digit, even though that cell might still show other candidates.\n\nScan the highlighted house for the target digit. Every other cell is blocked because that digit already appears somewhere in each cell\'s row, column, or box. That leaves exactly one valid home — so place the digit here.\n\nHidden singles are easier to spot once you write notes: look for a digit that appears in only one cell\'s candidate list within the highlighted house.',
          'naked-pair': 'A naked pair is two cells in the same house that together hold exactly the same two candidates and nothing else. Because those two digits must fill those two cells, no other cell in that house can use either of them.\n\nFind the two highlighted cells. Both carry exactly two candidates and those candidates are identical. Whichever digit lands in one cell, the other takes the second. Every other cell in the highlighted house can therefore lose both of those candidates — those are the eliminations shown.',
          'hidden-pair': 'A hidden pair is two digits that can only go in exactly two cells within the same house, even though those cells appear to carry other candidates too.\n\nLook at the highlighted house. The two target digits appear as candidates in exactly two cells and nowhere else in that house. Since those two cells must claim both digits between them, any other candidates those cells carry are now impossible and can be removed.',
          'pointing-pair-triple': 'A pointing pair or triple occurs when a digit\'s only valid placements within a box are all confined to the same row or column. That alignment means the digit cannot appear anywhere else in that row or column outside the box.\n\nLook at the highlighted box. The target digit can only go in the highlighted row or column inside it. Any cell in that same row or column that sits outside the box can safely lose that digit — those are the eliminations shown.',
          'box-line-reduction': 'Box-line reduction is the reverse of a pointing pair. When a digit\'s only valid placements in a row or column all fall inside a single box, no other cell in that box can hold that digit.\n\nLook at the highlighted row or column. All remaining positions for the target digit fall within one box. Every other cell in that box that still carries the digit as a candidate can safely have it removed.',
          'x-wing': 'An X-Wing forms when a digit appears as a candidate in exactly two cells in each of two rows, and both pairs align in the same two columns.\n\nThose four cells form a rectangle. The digit must be placed in one pair of diagonally opposite corners. Either way, every other cell in those two columns is eliminated. Look at the highlighted rows and columns to see the rectangle and the cells that lose the candidate.',
          'swordfish': 'A Swordfish extends the X-Wing idea to three rows and three columns. A digit appears as a candidate in two or three cells in each of three rows, and all those cells fall within the same three columns.\n\nBecause the digit must be placed exactly once in each of those three rows, and every placement is confined to the same three columns, no other cell in those three columns can hold that digit. The highlighted rows and columns mark the full pattern.',
          'xy-wing': 'An XY-Wing uses three cells that form a chain of two-candidate cells. A pivot cell shares one candidate with each of two wing cells, and the two wing cells share a candidate with each other that the pivot does not carry.\n\nNo matter how the pivot resolves, the shared digit between the two wings must land in one of them. Any cell that both wings can see can therefore safely lose that shared candidate. The highlighted cells show the pivot, the wings, and the targets.',
          'xyz-wing': 'An XYZ-Wing is a tighter version of an XY-Wing where the pivot holds three candidates instead of two. The pivot shares a pair with each wing, and all three cells together restrict where the shared digit can go.\n\nBecause the shared digit must land in one of the three highlighted cells — the pivot or either wing — any cell that all three can see can safely lose that candidate.',
          'coloring': 'Coloring assigns two alternating colors to all occurrences of a single candidate across the board, following conjugate pairs — houses where the digit has exactly two possible cells.\n\nOnce both colors are mapped, any cell that can see two cells of the same color can eliminate the candidate. If one color is correct, two same-color cells in the same house would be a contradiction. The highlighted cells show the chain and the conflict that forces the elimination.',
          'chains': 'A chain is a sequence of logical inferences linking candidates together. Each link is either a strong inference (if one end is false the other must be true) or a weak inference (both cannot be true at once).\n\nFollowing the chain, if a cell can see both endpoints and those endpoints carry the same candidate with a strong link between them, that candidate can be eliminated from the viewing cell. Chains are the most general technique and can resolve positions that no pattern-based rule can handle alone.',
        },
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
    goal: 'Coloca cada dígito del 1 al 9 exactamente una vez en cada fila, columna y caja de 3×3.',
    controls: 'Toca una celda para seleccionarla, luego toca un botón de dígito para colocarlo. Activa el modo notas para anotar candidatos en su lugar.',
    wrongMove: 'Un dígito repetido en la misma fila, columna o caja se resalta como conflicto.',
    rules: [
      {
        num: '1',
        title: 'Rellena la cuadrícula',
        body: 'Ningún dígito puede aparecer dos veces en la misma fila, columna o caja.',
      },
      {
        num: '2',
        title: 'Respeta las pistas dadas',
        body: 'Los dígitos ya escritos son fijos — no puedes modificarlos.',
      },
      {
        num: '3',
        title: 'Usa notas cuando una celda aún no esté lista',
        body: 'Activa el modo notas para anotar los dígitos posibles de una celda y tacharlos a medida que el puzzle se reduce.',
      },
    ],
    techniques: [
      {
        key: 'naked-single',
        title: 'Single desnudo',
        body: 'Cuando solo queda un dígito permitido en una celda tras descartar todos los que ya aparecen en su fila, columna y caja, colócalo.',
      },
      {
        key: 'hidden-single',
        title: 'Single oculto',
        body: 'Cuando un dígito solo cabe en una celda dentro de una fila, columna o caja, colócalo ahí aunque otros candidatos sigan visibles.',
      },
      {
        key: 'notes-mode',
        title: 'Modo notas',
        body: 'Escribe todos los dígitos posibles como notas y táchalos conforme las filas, columnas y cajas circundantes se van llenando — hasta que solo quede uno.',
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
        body: 'Si una celda todavía tiene varios dígitos válidos, cambia al modo notas en lugar de adivinar. Los candidatos anotados hacen que los hidden singles y otras colocaciones forzadas sean mucho más fáciles de detectar.',
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
