const fr = {
  strings: {
    title: 'Minesweeper',
    shortTitle: 'Minesweeper',
    tagline: 'Libérez la grille sans ouvrir de mine.',
    difficultyLabels: {
      easy: 'Facile',
      medium: 'Moyen',
      hard: 'Difficile',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: "Plus d'espace pour explorer tôt et lire les indices tranquillement.",
      medium: "Un plateau équilibré avec plus de mines et moins d'ouvertures sûres.",
      hard: 'Des espaces plus serrés qui récompensent un marquage soigneux et un suivi des indices.',
      expert: "Des champs de mines denses avec très peu d'espace dès le départ.",
    },
    play: {
      metadataLabels: {
        size: 'Taille',
        difficulty: 'Difficulté',
        minesLeft: 'Mines',
      },
      helperToggle: {
        show: 'Afficher le prochain coup',
        hide: 'Masquer le prochain coup',
      },
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Révéler les cases sûres',
        body: "Ouvrez les cases que vous pensez sûres. Une mine révélée termine le puzzle en cours.",
      },
      {
        num: '2',
        title: 'Lire les chiffres',
        body: 'Chaque chiffre révélé indique combien de mines touchent cette case, y compris en diagonal.',
      },
      {
        num: '3',
        title: 'Marquer les mines probables',
        body: "Appuyez longuement sur une case cachée pour placer ou retirer un drapeau quand vous êtes sûr qu'une mine s'y trouve.",
      },
      {
        num: '4',
        title: 'Libérer toutes les cases sûres',
        body: 'Le puzzle est résolu quand chaque case sans mine est révélée.',
      },
    ],
    tips: [
      {
        key: 'start-from-openings',
        title: 'Commencer par les ouvertures',
        body: 'Les grandes ouvertures vides révèlent plusieurs cases sûres à la fois et exposent souvent les premiers indices forts.',
      },
      {
        key: 'count-neighbors',
        title: 'Compter les voisins communs',
        body: 'Quand deux chiffres révélés touchent certaines des mêmes cases cachées, comparez leurs nombres de mines restants avant de placer des drapeaux.',
      },
      {
        key: 'Use flags carefully',
        title: 'Utiliser les drapeaux avec soin',
        body: "Les drapeaux vous aident à suivre les mines probables, mais ne prouvent pas qu'une case est dangereuse à moins que les indices environnants ne le confirment.",
      },
      {
        key: 'pace-matters',
        title: 'Comment fonctionne le score',
        body: 'Votre score commence à 10 000 et diminue pendant que le chronomètre tourne. Résoudre plus vite conserve plus de points.',
      },
    ],
  },
  loss: {
    abandoned: {
      eyebrow: 'Puzzle terminé',
      title: 'Puzzle inachevé',
      body: "Vous avez terminé ce puzzle avant qu'il soit résolu.",
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle perdu',
      title: 'Puzzle perdu',
      body: "Ce puzzle s'est terminé quand une mine a été ouverte. Un nouveau puzzle vous attend quand vous serez prêt.",
      icon: '💣',
    },
  },
  analysis: {
    lossSummary({ safeCount, mineCount }: { safeCount: number; mineCount: number }) {
      if (safeCount > 0 && mineCount > 0) {
        return {
          title: 'Des coups logiques étaient disponibles',
          body: `Ce plateau avait déjà ${safeCount} ${safeCount === 1 ? 'case sûre' : 'cases sûres'} à révéler et ${mineCount} ${mineCount === 1 ? 'mine certaine' : 'mines certaines'} à marquer.`,
        };
      }

      if (safeCount > 0) {
        return {
          title: 'Des révélations sûres étaient disponibles',
          body: `Ce plateau avait déjà ${safeCount} ${safeCount === 1 ? 'case sûre' : 'cases sûres'} à révéler d'après les indices montrés.`,
        };
      }

      return {
        title: 'Des marquages certains étaient disponibles',
        body: `Ce plateau avait déjà ${mineCount} ${mineCount === 1 ? 'mine certaine' : 'mines certaines'} à marquer d'après les indices montrés.`,
      };
    },
    groupedFlagStep({ mineCount }: { mineCount: number }) {
      return {
        title: 'Mines certaines sur ce plateau',
        body: `Vous pouvez marquer les ${mineCount === 1 ? 'case' : 'cases'} mises en évidence maintenant. Les indices actuels prouvent déjà que ${mineCount === 1 ? "c'est une mine" : 'ce sont des mines'}.`,
      };
    },
    legendEvidence: 'Preuve',
    legendSafe: 'Révélation sûre',
    legendMine: 'Marquer une mine',
  },
  tutorialText: {
    'goal-and-stakes': {
      title: 'Révéler toutes les cases sûres',
      body: "Révélez chaque case qui ne cache pas de mine. Si vous révélez une mine, la partie se termine immédiatement.",
      prompt: 'Les drapeaux vous aident à repérer le danger, mais seules les révélations sûres gagnent le plateau.',
      summary: 'Gagnez en révélant toutes les cases sûres. Perdez en ouvrant une mine.',
      continueLabel: 'Continuer',
    },
    'core-actions': {
      title: 'Vous avez deux actions',
      body: "Dans un vrai puzzle, appuyez sur une case cachée pour la révéler. Appuyez et maintenez sur une case cachée pour placer ou retirer un drapeau.",
      prompt: 'Dans ce tutoriel, choisissez Révéler ou Marquer avec les boutons ci-dessous. Sur un vrai plateau, appuyer révèle et appuyer longuement marque.',
      summary: 'Révéler ouvre une case. Appuyer longuement bascule un drapeau. Les drapeaux vous aident à suivre les mines mais ne résolvent pas le plateau seuls.',
      continueLabel: 'Continuer',
    },
    'reading-clues': {
      title: 'Les chiffres comptent toutes les mines adjacentes',
      body: 'Chaque chiffre indique combien de mines touchent cette case dans les huit espaces voisins.',
      prompt: 'Les voisins en diagonal comptent aussi.',
      summary: 'Un indice vous dit combien de mines le touchent, pas exactement où elles se trouvent.',
      continueLabel: 'Continuer',
    },
    'forced-flag': {
      title: 'Marquer la case qui doit cacher une mine',
      body: "Cet indice a encore besoin d'une mine, et la case mise en évidence est le seul endroit caché restant.",
      prompt: 'Que devez-vous faire avec la case mise en évidence\u00a0?',
      retry: "Cet indice a encore besoin d'une mine, et aucun autre voisin caché ne peut la fournir.",
      success: 'Correct. La case mise en évidence devait être une mine, donc Marquer est la bonne réponse.',
    },
    'safe-reveal': {
      title: 'Révéler la case qui doit être sûre',
      body: "Cet indice a déjà sa mine, donc le voisin mis en évidence ne peut pas en cacher une autre.",
      prompt: 'Que devez-vous faire avec la case mise en évidence\u00a0?',
      retry: "L'indice est déjà satisfait par la mine marquée, donc le voisin caché restant est sûr.",
      success: "Correct. Une fois que l'indice a déjà sa mine, la case mise en évidence est sûre à révéler.",
    },
    'compare-clues': {
      title: 'Comparer les indices ensemble',
      body: 'Ces indices partagent des cases cachées. Une fois que le groupe partagé couvre le nombre de mines, la case supplémentaire devient sûre.',
      prompt: 'Que devez-vous faire avec la case mise en évidence\u00a0?',
      retry: 'Lisez les deux indices ensemble. Les cases cachées partagées absorbent le nombre de mines, donc la case supplémentaire est sûre.',
      success: 'Correct. En comparant les deux indices, vous prouvez que la case mise en évidence ne peut pas être une mine.',
    },
    'advanced-patterns': {
      title: 'Certains schémas utilisent les diagonales et les chevauchements',
      body: 'Ici, le 1 du coin compte la mine diagonale marquée, et le 2 et le 1 à côté lisent tous les deux la même bande cachée du haut.',
      prompt: 'Un indice de coin compte encore les diagonales, et les indices proches peuvent se chevaucher sur les mêmes cases cachées.',
      summary: 'Ne lisez pas un seul indice quand une mine diagonale ou une bande cachée partagée change ce que signifient les indices proches.',
      continueLabel: 'Continuer',
    },
    'guess-and-help': {
      title: "Parfois la logique s'épuise",
      body: "Ce bord supérieur correspond encore à plus d'une disposition de mines. La rangée de 1 ne vous dit pas quelles cases cachées sont les mines.",
      prompt: "Quand cela arrive, faites la supposition la plus sereine possible et utilisez les outils d'aide si nécessaire.",
      summary: 'Différentes dispositions de mines peuvent correspondre aux mêmes indices. Les conseils aident, annuler corrige les mauvaises touches, et vous pouvez rejouer ce tutoriel plus tard.',
      continueLabel: 'Terminer',
    },
  },
  learningCenter: {
    formatCellLabel({ row, col }: { row: number; col: number }) {
      return `ligne ${row + 1}, colonne ${col + 1}`;
    },
    tileLabel(count: number) {
      return count === 1 ? 'case' : 'cases';
    },
    mineLabel(count: number) {
      return count === 1 ? 'mine' : 'mines';
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
            title: `Prochain coup sûr près de ${clueLabel ?? 'cet indice'}`,
            body: `Révélez les ${tileLabel} mises en évidence. Ce schéma d'indice local laisse encore un emplacement de mine, ce qui rend les autres ${tileLabel} cachées sûres.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Logique mine unique',
              explanationTitle: 'Explication',
              explanation: `Ce schéma d'indice local a encore besoin d'exactement une ${mineLabel}. Une fois cet emplacement de mine unique fixé, les autres ${tileLabel} cachées touchantes doivent être sûres.`,
            },
          };
        case 'all-mines-accounted-for':
          return {
            title: `Prochain coup sûr près de ${clueLabel ?? 'cet indice'}`,
            body: `Révélez les ${tileLabel} mises en évidence. Autour de ${clueLabel ?? 'cet indice'}, toutes les ${mineCount} ${mineLabel} sont déjà comptées.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Toutes les mines comptées',
              explanationTitle: 'Explication',
              explanation: `Cet indice a déjà toutes les ${mineCount} ${mineLabel} dont il a besoin, donc toutes les autres ${tileLabel} cachées le touchant doivent être sûres.`,
            },
          };
        case 'only-one-possible-mine':
          return {
            title: 'Prochain coup sûr en comparant les indices',
            body: `Révélez les ${tileLabel} mises en évidence. Lire ${clueLabel ?? 'un indice'} avec ${secondaryClueLabel ?? 'un autre indice'} ne laisse qu'un seul emplacement légal pour la mine restante.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Une seule mine possible',
              explanationTitle: 'Explication',
              explanation: `Comparer ces indices ne laisse qu'un seul emplacement légal pour la ${mineLabel} restante, donc les ${tileLabel} cachées supplémentaires en dehors de cet emplacement de mine doivent être sûres.`,
            },
          };
        case 'guaranteed-safe-tile':
          return {
            title: `Prochain coup sûr près de ${clueLabel ?? 'cet indice'}`,
            body: `Révélez les ${tileLabel} mises en évidence. Si cette case était une mine, les indices proches auraient trop de mines.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Case garantie sûre',
              explanationTitle: 'Explication',
              explanation: `Si les ${tileLabel} mises en évidence étaient des mines, au moins un indice proche aurait trop de ${mineLabel}. Puisque ce n'est pas possible, la case doit être sûre.`,
            },
          };
        case 'full-clue-resolution':
          return {
            title: `Prochain coup sûr près de ${clueLabel ?? 'cet indice'}`,
            body: `Révélez les ${tileLabel} mises en évidence. L'exigence en mines de cet indice est entièrement résolue, donc les ${tileLabel} cachées restantes sont sûres.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: "Résolution complète de l'indice",
              explanationTitle: 'Explication',
              explanation: `L'exigence en mines de cet indice est entièrement résolue par les positions de mines forcées proches, donc les ${tileLabel} cachées restantes le touchant doivent être sûres.`,
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
            title: `Mine certaine près de ${clueLabel ?? 'cet indice'}`,
            body: `Marquez les ${tileLabel} mises en évidence. ${clueLabel ?? 'Cet indice'} a encore besoin de ${mineCount} ${mineLabel}, et les ${tileLabel} cachées mises en évidence sont les seuls emplacements restants.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Mine locale directe',
              explanationTitle: 'Explication',
              explanation: `Cet indice a encore besoin de ${mineCount} ${mineLabel}. Parce que seules les ${tileLabel} cachées mises en évidence restent autour de lui, chaque case mise en évidence doit être une mine.`,
            },
          };
        case 'subset-difference':
          return {
            title: 'Mine certaine en comparant les indices',
            body: `Marquez les ${tileLabel} mises en évidence. Comparer ${clueLabel ?? 'un indice'} avec ${secondaryClueLabel ?? 'un autre indice'} montre que les ${tileLabel} cachées supplémentaires doivent contenir les ${mineLabel} restantes.`,
            teaching: {
              patternTitle: 'Schéma',
              patternLabel: 'Différence de sous-ensemble',
              explanationTitle: 'Explication',
              explanation: `Les cases cachées du plus petit indice s'inscrivent dans celles du plus grand. Après avoir pris en compte les emplacements de mines partagés, les ${tileLabel} cachées supplémentaires doivent contenir les ${mineLabel} restantes.`,
            },
          };
        default:
          throw new Error(`Unhandled flag move pattern: ${reason satisfies never}`);
      }
    },
    guess: {
      title: 'Pas encore de prochain coup certain',
      body: "Aucun indice ne pointe vers une révélation sûre certaine pour l'instant. Cet endroit peut nécessiter une supposition, donc faites confiance à votre meilleure lecture du plateau et redemandez après la prochaine révélation.",
    },
  },
  tutorialUi: {
    progressLabel: (step: number) => `Leçon ${step}`,
    exitLabel: {
      end: 'Terminer le tutoriel',
      skip: 'Passer le tutoriel',
    },
    status: {
      finishing: 'Tutoriel en cours de fin\u2026',
      nextLesson: 'Prochaine leçon en cours\u2026',
    },
    highlightedTile: 'Case de tutoriel mise en évidence',
  },
} as const;

export default fr;
import type { LearningCenterMineFlagParams, LearningCenterPatternParams } from './index';
