import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getCurrentLanguage } from '../../../app/i18n';

export function getMinesweeperHowToPlay(): HowToPlayContent {
  if (getCurrentLanguage() === 'nl') {
    return {
      rules: [
        {
          num: '1',
          title: 'Open veilige vakken',
          body: 'Open vakken waarvan je denkt dat ze veilig zijn. Een geopende mijn beëindigt de actieve puzzel.',
        },
        {
          num: '2',
          title: 'Lees de cijfers',
          body: 'Elk zichtbaar cijfer toont hoeveel mijnen die cel raken, inclusief diagonalen.',
        },
        {
          num: '3',
          title: 'Markeer waarschijnlijke mijnen',
          body: 'Houd een verborgen cel lang ingedrukt om een vlag te plaatsen of verwijderen wanneer je zeker denkt te zijn dat daar een mijn ligt.',
        },
        {
          num: '4',
          title: 'Maak elk veilig vak vrij',
          body: 'De puzzel is opgelost wanneer elke cel zonder mijn zichtbaar is.',
        },
      ],
      tips: [
        {
          key: 'start-from-openings',
          title: 'Begin bij openingen',
          body: 'Grote lege openingen onthullen meerdere veilige vakken tegelijk en tonen vaak de eerste sterke aanwijzingen.',
        },
        {
          key: 'count-neighbors',
          title: 'Tel gedeelde buren',
          body: 'Wanneer twee zichtbare cijfers sommige verborgen vakken delen, vergelijk dan eerst hun resterende mijnenaantallen voordat je vlaggen plaatst.',
        },
        {
          key: 'Use flags carefully',
          title: 'Gebruik vlaggen zorgvuldig',
          body: 'Vlaggen helpen je waarschijnlijke mijnen bij te houden, maar bewijzen niet dat een cel gevaarlijk is tenzij de omliggende aanwijzingen dat ondersteunen.',
        },
        {
          key: 'pace-matters',
          title: 'Hoe scoren werkt',
          body: 'Je score start op 10.000 en daalt terwijl de timer loopt. Sneller veilig oplossen houdt meer score over.',
        },
      ],
    };
  }

  return {
    rules: [
      {
        num: '1',
        title: 'Reveal safe cells',
        body: 'Open cells that you believe are safe. A revealed mine ends the active puzzle.',
      },
      {
        num: '2',
        title: 'Read the numbers',
        body: 'Each revealed number shows how many mines touch that cell, including diagonals.',
      },
      {
        num: '3',
        title: 'Flag likely mines',
        body: 'Long-press a hidden cell to place or remove a flag when you are confident a mine is there.',
      },
      {
        num: '4',
        title: 'Clear every safe cell',
        body: 'The puzzle is solved when every non-mine cell is revealed.',
      },
    ],
    tips: [
      {
        key: 'start-from-openings',
        title: 'Start from openings',
        body: 'Large empty openings reveal several safe cells at once and often expose the first strong clues.',
      },
      {
        key: 'count-neighbors',
        title: 'Count shared neighbors',
        body: 'When two revealed numbers touch some of the same hidden cells, compare their remaining mine counts before placing flags.',
      },
      {
        key: 'Use flags carefully',
        title: 'Use flags carefully',
        body: 'Flags help you track likely mines, but they do not prove a cell is dangerous unless the surrounding clues support it.',
      },
      {
        key: 'pace-matters',
        title: 'How scoring works',
        body: 'Your score starts at 10,000 and drops while the puzzle timer runs. Faster safe solves keep more score.',
      },
    ],
  };
}
