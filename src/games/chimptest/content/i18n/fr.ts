import type { ChimpTestI18n } from './index';

const fr: ChimpTestI18n = {
  strings: {
    title: 'Chimp Test',
    shortTitle: 'Chimp',
    tagline: 'Touchez les chiffres dans l\'ordre avant qu\'ils ne disparaissent.',
    difficultyLabels: {
      easy: 'Facile',
      medium: 'Moyen',
      hard: 'Difficile',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Grille 4×4. Mémorisez jusqu\'à 7 chiffres.',
      medium: 'Grille 5×5. Mémorisez jusqu\'à 9 chiffres.',
      hard: 'Grille 6×6. Mémorisez jusqu\'à 11 chiffres.',
      expert: 'Grille 7×7. Mémorisez jusqu\'à 13 chiffres.',
    },
    play: {
      metadataLabels: {
        round: 'Manche',
        difficulty: 'Difficulté',
      },
    },
  },
  howToPlayGoal: 'Touchez chaque chiffre dans l\'ordre croissant avant que les précédents ne disparaissent de la vue.',
  howToPlayControls: 'Touchez le prochain chiffre de la séquence. Chaque chiffre correctement touché disparaît immédiatement, vous devez donc retenir où se trouvent les autres.',
  howToPlayWrongMove: 'Toucher une case qui n\'est pas le prochain chiffre met fin au puzzle immédiatement.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Étudier la grille',
      body: 'Les chiffres apparaissent à des positions aléatoires à chaque manche. Parcourez toute la grille avant de toucher.',
    },
    {
      num: '2',
      title: 'Toucher dans l\'ordre',
      body: 'Touchez toujours le 1 en premier, puis le 2, 3, et ainsi de suite. Il n\'y a pas de pression temporelle tant que les chiffres sont visibles.',
    },
    {
      num: '3',
      title: 'Les chiffres disparaissent au toucher',
      body: 'Chaque toucher correct retire ce chiffre de la grille. Vous devez retenir les positions des chiffres restants.',
    },
    {
      num: '4',
      title: 'Les manches progressent',
      body: 'Chaque manche réussie ajoute un chiffre supplémentaire. Atteignez le nombre maximum pour résoudre le puzzle.',
    },
  ],
  howToPlayTechniques: [],
  howToPlayTips: [
    {
      key: 'scan-first',
      title: 'Scanner avant de toucher',
      body: 'Prenez un moment pour tracer un chemin à travers tous les chiffres avant de toucher le 1. Un itinéraire mental rapide est plus efficace que chercher en cours de séquence.',
    },
    {
      key: 'group',
      title: 'Regrouper les chiffres proches',
      body: 'Repérez les groupes de chiffres consécutifs. Toucher les voisins l\'un après l\'autre est plus rapide que de traverser la grille.',
    },
  ],
  howToPlayScoring: 'Le score est basé sur le temps total sur toutes les manches. Toucher plus vite donne un score plus élevé.',
  loss: {
    abandoned: {
      eyebrow: 'Puzzle terminé',
      title: 'Puzzle inachevé',
      body: 'Vous avez terminé ce puzzle avant qu\'il ne soit résolu.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzle perdu',
      title: 'Puzzle perdu',
      body: 'Vous avez touché la mauvaise case.',
      icon: '⚠️',
    },
  },
};

export default fr;
