import type { ChimpTestI18n } from './index';

const nl: ChimpTestI18n = {
  strings: {
    title: 'Chimp Test',
    shortTitle: 'Chimp',
    tagline: 'Tik de cijfers op volgorde aan voordat ze verdwijnen.',
    difficultyLabels: {
      easy: 'Makkelijk',
      medium: 'Gemiddeld',
      hard: 'Moeilijk',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: '4×4 raster. Onthoud tot 7 cijfers.',
      medium: '5×5 raster. Onthoud tot 9 cijfers.',
      hard: '6×6 raster. Onthoud tot 11 cijfers.',
      expert: '7×7 raster. Onthoud tot 13 cijfers.',
    },
    play: {
      metadataLabels: {
        round: 'Ronde',
        difficulty: 'Moeilijkheid',
      },
    },
  },
  howToPlayGoal: 'Tik elk cijfer in oplopende volgorde aan voordat de vorige uit het zicht verdwijnen.',
  howToPlayControls: 'Tik het volgende cijfer in de reeks aan. Elk correct aangetikt cijfer verdwijnt meteen, dus je moet onthouden waar de resterende cijfers staan.',
  howToPlayWrongMove: 'Een cel aantikken die niet het volgende cijfer is, beeindigt de puzzel onmiddellijk.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Scan het raster',
      body: 'De cijfers verschijnen elke ronde op willekeurige posities. Scan het hele raster voordat je begint te tikken.',
    },
    {
      num: '2',
      title: 'Tik op volgorde',
      body: 'Tik altijd eerst op 1, dan op 2, 3, enzovoort. Er is geen tijdsdruk zolang de cijfers zichtbaar zijn.',
    },
    {
      num: '3',
      title: 'Cijfers verdwijnen tijdens het tikken',
      body: 'Elk correct aangetikt cijfer verdwijnt uit het raster. Je moet de posities van de resterende cijfers onthouden.',
    },
    {
      num: '4',
      title: 'Rondes worden zwaarder',
      body: 'Elke geslaagde ronde voegt een cijfer toe. Bereik het maximum om de puzzel op te lossen.',
    },
  ],
  howToPlayTechniques: [],
  howToPlayTips: [
    {
      key: 'scan-first',
      title: 'Eerst scannen, dan tikken',
      body: 'Neem even de tijd om een route door alle cijfers te plannen voordat je op 1 tikt. Een snelle mentale route is sneller dan zoeken midden in de reeks.',
    },
    {
      key: 'group',
      title: 'Nabijgelegen cijfers groeperen',
      body: 'Let op clusters van opeenvolgende cijfers. Buren na elkaar aantikken is sneller dan dwars door het raster springen.',
    },
  ],
  howToPlayScoring: 'De score is gebaseerd op de totale tijd over alle rondes. Sneller tikken levert een hogere score op.',
  loss: {
    abandoned: {
      eyebrow: 'Puzzel beeindigd',
      title: 'Puzzel niet af',
      body: 'Je hebt deze puzzel beeindigd voordat hij was opgelost.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzel verloren',
      title: 'Puzzel verloren',
      body: 'Je hebt een verkeerde cel aangetikt.',
      icon: '⚠️',
    },
  },
};

export default nl;
