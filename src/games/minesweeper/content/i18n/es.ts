const es = {
  strings: {
    title: 'Buscaminas',
    shortTitle: 'Buscaminas',
    tagline: 'Limpia la cuadrícula sin abrir ninguna mina.',
    difficultyLabels: {
      easy: 'Fácil',
      medium: 'Medio',
      hard: 'Difícil',
      expert: 'Experto',
    },
    difficultyDescriptions: {
      easy: 'Más espacio para explorar temprano y leer las pistas con calma.',
      medium: 'Un tablero equilibrado con más minas y menos aperturas seguras.',
      hard: 'Espacios más ajustados que recompensan el marcado cuidadoso y el seguimiento de pistas.',
      expert: 'Campos de minas densos con muy poco espacio desde el principio.',
    },
    play: {
      metadataLabels: {
        size: 'Tamaño',
        difficulty: 'Dificultad',
        minesLeft: 'Minas',
      },
      helperToggle: {
        show: 'Mostrar siguiente movimiento',
        hide: 'Ocultar siguiente movimiento',
      },
    },
  },
  howToPlay: {
    goal: 'Revela cada casilla segura sin abrir una mina.',
    controls: 'Toca para revelar una casilla. Mantén pulsado para colocar o quitar una bandera.',
    wrongMove: 'Abrir una mina termina el puzzle inmediatamente.',
    rules: [
      {
        num: '1',
        title: 'Revelar casillas seguras',
        body: 'Toca cualquier casilla oculta que creas segura para abrirla.',
      },
      {
        num: '2',
        title: 'Leer los números',
        body: 'Cada número revelado muestra cuántas minas tocan esa casilla, incluidas las diagonales.',
      },
      {
        num: '3',
        title: 'Marcar las minas probables',
        body: 'Mantén pulsada cualquier casilla oculta para marcarla o desmarcarla como posible mina.',
      },
      {
        num: '4',
        title: 'Limpiar cada casilla segura',
        body: 'El puzzle se completa cuando cada casilla segura está revelada.',
      },
    ],
    techniques: [
      {
        key: 'forced-flag',
        title: 'Bandera forzada',
        body: 'Cuando los vecinos ocultos restantes de una pista igualan su recuento de minas, todos son minas — márcalos todos.',
      },
      {
        key: 'safe-reveal',
        title: 'Revelación segura',
        body: 'Cuando una pista ya tiene todas sus minas marcadas, cada vecino oculto restante es seguro para revelar.',
      },
      {
        key: 'compare-clues',
        title: 'Comparar pistas',
        body: 'Dos pistas que comparten casillas ocultas revelan juntas más que cada una por separado — la superposición muestra qué casillas son seguras y cuáles son minas.',
      },
    ],
    scoring: 'Comienza en 10.000 y baja mientras el temporizador avanza. Terminar más rápido conserva más puntuación.',
    tips: [
      {
        key: 'start-from-openings',
        title: 'Empezar por las aperturas',
        body: 'Las grandes aperturas vacías revelan varias casillas seguras a la vez y suelen exponer las primeras pistas fuertes.',
      },
      {
        key: 'count-neighbors',
        title: 'Contar vecinos compartidos',
        body: 'Cuando dos números revelados tocan algunas de las mismas casillas ocultas, compara sus recuentos de minas restantes antes de colocar banderas.',
      },
      {
        key: 'use-flags-carefully',
        title: 'Usar las banderas con cuidado',
        body: 'Una bandera solo es tan fiable como el razonamiento detrás — no trates las casillas marcadas como confirmadas a menos que las pistas lo respalden.',
      },
    ],
  },
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
      body: 'Este puzzle terminó cuando se abrió una mina. Un nuevo puzzle te espera cuando quieras.',
      icon: '💣',
    },
  },
  analysis: {
    lossSummary({ safeCount, mineCount }: { safeCount: number; mineCount: number }) {
      if (safeCount > 0 && mineCount > 0) {
        return {
          title: 'Había movimientos lógicos disponibles',
          body: `Este tablero ya tenía ${safeCount} ${safeCount === 1 ? 'casilla segura' : 'casillas seguras'} para revelar y ${mineCount} ${mineCount === 1 ? 'mina segura' : 'minas seguras'} para marcar.`,
        };
      }

      if (safeCount > 0) {
        return {
          title: 'Había revelaciones seguras disponibles',
          body: `Este tablero ya tenía ${safeCount} ${safeCount === 1 ? 'casilla segura' : 'casillas seguras'} para revelar a partir de las pistas mostradas.`,
        };
      }

      return {
        title: 'Había marcados seguros disponibles',
        body: `Este tablero ya tenía ${mineCount} ${mineCount === 1 ? 'mina segura' : 'minas seguras'} para marcar a partir de las pistas mostradas.`,
      };
    },
    groupedFlagStep({ mineCount }: { mineCount: number }) {
      return {
        title: 'Minas seguras en este tablero',
        body: `Puedes marcar las ${mineCount === 1 ? 'casilla resaltada' : 'casillas resaltadas'} ahora. Las pistas actuales ya prueban que ${mineCount === 1 ? 'es una mina' : 'son minas'}.`,
      };
    },
    groupedSafeStep({ targetCount, reasonCount }: { targetCount: number; reasonCount: number }) {
      return {
        title: 'Varias pistas respaldan esta revelación segura',
        body: `Revela las ${targetCount === 1 ? 'casilla resaltada' : 'casillas resaltadas'}. ${reasonCount} patrones de pista apuntan de forma independiente al mismo movimiento seguro, así que esta revelación queda confirmada desde más de un ángulo.`,
      };
    },
    legendEvidence: 'Evidencia',
    legendSafe: 'Revelar seguro',
    legendMine: 'Marcar mina',
  },
  tutorialText: {
    'goal-and-stakes': {
      title: 'Revelar cada casilla segura',
      body: 'Revela cada casilla que no oculte una mina. Si revelas una mina, la partida termina inmediatamente.',
      prompt: 'Las banderas ayudan a rastrear el peligro, pero solo las revelaciones seguras ganan el tablero.',
      summary: 'Gana revelando cada casilla segura. Pierde abriendo una mina.',
      continueLabel: 'Continuar',
    },
    'core-actions': {
      title: 'Tienes dos acciones',
      body: 'En un puzzle real, toca una casilla oculta para revelarla. Mantén pulsada una casilla oculta para colocar o quitar una bandera.',
      prompt: 'En este tutorial elige Revelar o Marcar con los botones de abajo. En un tablero real, tocar revela y mantener pulsado marca.',
      summary: 'Revelar abre una casilla. Mantener pulsado activa una bandera. Las banderas ayudan a rastrear minas pero no resuelven el tablero por sí solas.',
      continueLabel: 'Continuar',
    },
    'reading-clues': {
      title: 'Los números cuentan todas las minas adyacentes',
      body: 'Cada número muestra cuántas minas tocan esa casilla en cualquiera de los ocho espacios vecinos.',
      prompt: 'Los vecinos en diagonal también cuentan.',
      summary: 'Una pista te dice cuántas minas la tocan, no exactamente dónde están todavía.',
      continueLabel: 'Continuar',
    },
    'forced-flag': {
      title: 'Marcar la casilla que debe ocultar una mina',
      body: 'Esta pista todavía necesita una mina, y la casilla resaltada es el único lugar oculto que queda.',
      prompt: '¿Qué debes hacer con la casilla resaltada?',
      retry: 'Esta pista todavía necesita una mina, y ningún otro vecino oculto puede proporcionarla.',
      success: 'Correcto. La casilla resaltada tenía que ser una mina, así que Marcar es la respuesta correcta.',
    },
    'safe-reveal': {
      title: 'Revelar la casilla que debe ser segura',
      body: 'Esta pista ya tiene su mina, así que el vecino resaltado no puede ocultar otra.',
      prompt: '¿Qué debes hacer con la casilla resaltada?',
      retry: 'La pista ya está satisfecha por la mina marcada, así que el vecino oculto restante es seguro.',
      success: 'Correcto. Una vez que la pista ya tiene su mina, la casilla resaltada es segura para revelar.',
    },
    'compare-clues': {
      title: 'Comparar pistas juntas',
      body: 'Estas pistas comparten casillas ocultas. Una vez que el grupo compartido absorbe el recuento de minas, la casilla extra se vuelve segura.',
      prompt: '¿Qué debes hacer con la casilla resaltada?',
      retry: 'Lee las dos pistas juntas. Las casillas ocultas compartidas absorben el recuento de minas, así que la casilla extra es segura.',
      success: 'Correcto. Al comparar las dos pistas, demuestras que la casilla resaltada no puede ser una mina.',
    },
    'advanced-patterns': {
      title: 'Algunos patrones usan diagonales y superposición',
      body: 'Aquí el 1 de esquina cuenta la mina diagonal marcada, y el 2 y el 1 a su lado leen la misma franja oculta superior.',
      prompt: 'Una pista de esquina todavía cuenta diagonales, y las pistas cercanas pueden superponerse en las mismas casillas ocultas.',
      summary: 'No leas una sola pista cuando una mina diagonal o una franja oculta compartida cambia lo que significan las pistas cercanas.',
      continueLabel: 'Continuar',
    },
    'guess-and-help': {
      title: 'A veces la lógica se agota',
      body: 'Este borde superior todavía encaja con más de una disposición de minas. La fila de 1s no te dice qué casillas ocultas son las minas.',
      prompt: 'Cuando eso ocurra, haz la suposición más tranquila que puedas y usa las herramientas de ayuda cuando sea necesario.',
      summary: 'Diferentes disposiciones de minas pueden encajar con las mismas pistas. Las pistas ayudan, deshacer corrige los toques erróneos, y puedes repetir este tutorial más adelante.',
      continueLabel: 'Finalizar',
    },
  },
  learningCenter: {
    formatCellLabel({ row, col }: { row: number; col: number }) {
      return `fila ${row + 1}, columna ${col + 1}`;
    },
    tileLabel(count: number) {
      return count === 1 ? 'casilla' : 'casillas';
    },
    mineLabel(count: number) {
      return count === 1 ? 'mina' : 'minas';
    },
    nextMovePattern({
      patternKey,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterPatternParams) {
      switch (patternKey) {
        case 'single-mine-logic':
          return {
            title: `Siguiente movimiento seguro cerca de ${clueLabel ?? 'esta pista'}`,
            body: `Revela las ${tileLabel} resaltadas. Este patrón de pista local todavía deja un espacio de mina, lo que hace que las otras ${tileLabel} ocultas sean seguras.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Lógica de mina única',
              explanationTitle: 'Explicación',
              explanation: `Este patrón de pista local todavía necesita exactamente una ${mineLabel}. Una vez que ese único espacio de mina está fijado, las otras ${tileLabel} ocultas tocantes deben ser seguras.`,
            },
          };
        case 'all-mines-accounted-for':
          return {
            title: `Siguiente movimiento seguro cerca de ${clueLabel ?? 'esta pista'}`,
            body: `Revela las ${tileLabel} resaltadas. Alrededor de ${clueLabel ?? 'esta pista'}, todas las ${mineCount} ${mineLabel} ya están contadas.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Todas las minas contadas',
              explanationTitle: 'Explanation',
              explanation: `Esta pista ya tiene todas las ${mineCount} ${mineLabel} que necesita, así que todas las demás ${tileLabel} ocultas que la tocan deben ser seguras.`,
            },
          };
        case 'only-one-possible-mine':
          return {
            title: 'Siguiente movimiento seguro comparando pistas',
            body: `Revela las ${tileLabel} resaltadas. Leer ${clueLabel ?? 'una pista'} junto con ${secondaryClueLabel ?? 'otra pista'} deja solo un lugar legal para la mina restante.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Solo una mina posible',
              explanationTitle: 'Explicación',
              explanation: `Comparar estas pistas deja exactamente un lugar legal para la ${mineLabel} restante, así que las ${tileLabel} ocultas extra fuera de ese espacio de mina deben ser seguras.`,
            },
          };
        case 'guaranteed-safe-tile':
          return {
            title: `Siguiente movimiento seguro cerca de ${clueLabel ?? 'esta pista'}`,
            body: `Revela las ${tileLabel} resaltadas. Si esta casilla fuera una mina, las pistas cercanas tendrían demasiadas minas.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Casilla garantizada segura',
              explanationTitle: 'Explicación',
              explanation: `Si las ${tileLabel} resaltadas fueran minas, al menos una pista cercana tendría demasiadas ${mineLabel}. Como eso no es posible, la casilla debe ser segura.`,
            },
          };
        case 'full-clue-resolution':
          return {
            title: `Siguiente movimiento seguro cerca de ${clueLabel ?? 'esta pista'}`,
            body: `Revela las ${tileLabel} resaltadas. El requisito de minas de esta pista está completamente resuelto, así que las ${tileLabel} ocultas restantes son seguras.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Resolución completa de la pista',
              explanationTitle: 'Explicación',
              explanation: `El requisito de minas de esta pista está completamente resuelto por las posiciones de minas forzadas cercanas, así que las ${tileLabel} ocultas restantes que la tocan deben ser seguras.`,
            },
          };
        default:
          throw new Error(`Unhandled next move pattern: ${patternKey satisfies never}`);
      }
    },
    flagMovePattern({
      reason,
      clueLabel,
      secondaryClueLabel,
      tileLabel,
      mineLabel,
      mineCount,
    }: LearningCenterMineFlagParams) {
      switch (reason) {
        case 'direct-local':
          return {
            title: `Mina segura cerca de ${clueLabel ?? 'esta pista'}`,
            body: `Marca las ${tileLabel} resaltadas. ${clueLabel ?? 'Esta pista'} todavía necesita ${mineCount} ${mineLabel}, y las ${tileLabel} ocultas resaltadas son los únicos lugares que quedan.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Mina local directa',
              explanationTitle: 'Explicación',
              explanation: `Esta pista todavía necesita ${mineCount} ${mineLabel}. Como solo quedan las ${tileLabel} ocultas resaltadas a su alrededor, cada casilla resaltada debe ser una mina.`,
            },
          };
        case 'subset-difference':
          return {
            title: 'Mina segura comparando pistas',
            body: `Marca las ${tileLabel} resaltadas. Comparar ${clueLabel ?? 'una pista'} con ${secondaryClueLabel ?? 'otra pista'} muestra que las ${tileLabel} ocultas extra deben contener las ${mineLabel} restantes.`,
            teaching: {
              patternTitle: 'Patrón',
              patternLabel: 'Diferencia de subconjunto',
              explanationTitle: 'Explicación',
              explanation: `Las casillas ocultas de la pista menor encajan dentro de las de la pista mayor. Después de contabilizar los espacios de minas compartidos, las ${tileLabel} ocultas extra deben contener las ${mineLabel} restantes.`,
            },
          };
        default:
          throw new Error(`Unhandled flag move pattern: ${reason satisfies never}`);
      }
    },
    guess: {
      title: 'Todavía no hay un siguiente movimiento seguro',
      body: 'Ninguna pista apunta a una revelación segura ahora mismo. Este lugar puede necesitar una suposición, así que confía en tu mejor lectura del tablero y vuelve a preguntar después de la próxima revelación.',
    },
  },
  tutorialUi: {
    progressLabel: (step: number) => `Lección ${step}`,
    exitLabel: {
      end: 'Terminar tutorial',
      skip: 'Omitir tutorial',
    },
    status: {
      finishing: 'Cerrando el tutorial\u2026',
      nextLesson: 'Iniciando la siguiente lección\u2026',
    },
    highlightedTile: 'Casilla del tutorial resaltada',
  },
} as const;

export default es;
import type { LearningCenterMineFlagParams, LearningCenterPatternParams } from './index';
