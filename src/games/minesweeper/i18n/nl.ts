const nl = {
  strings: {
    title: 'Minesweeper',
    shortTitle: 'Minesweeper',
    tagline: 'Maak het rooster vrij zonder een mijn te openen.',
    difficultyLabels: {
      easy: 'Makkelijk',
      medium: 'Gemiddeld',
      hard: 'Moeilijk',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Meer ademruimte voor vroeg scannen en rustig aanwijzingen lezen.',
      medium: 'Een gebalanceerd bord met meer mijnen en minder veilige openingen.',
      hard: 'Strakkere ruimtes die zorgvuldig markeren en aanwijzingen volgen belonen.',
      expert: 'Dichte mijnenvelden met vanaf het begin heel weinig ademruimte.',
    },
  },
  howToPlay: {
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
  },
  loss: {
    forfeit: {
      eyebrow: 'Puzzel beëindigd',
      title: 'Puzzel niet af',
      body: 'Je hebt deze puzzel beëindigd voordat hij was opgelost.',
      icon: '🏁',
    },
    'rule-based': {
      eyebrow: 'Puzzel verloren',
      title: 'Puzzel verloren',
      body: 'Deze puzzel eindigde toen een mijn werd geopend. Er staat een nieuwe puzzel klaar wanneer jij wilt.',
      icon: '💣',
    },
  },
  tutorialText: {
    'forced-flag': {
      title: 'Markeer het vak dat zeker een mijn verbergt',
      body: 'Die 1 heeft nog steeds één mijn nodig, en het gemarkeerde vak is de enige verborgen buur.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'Kijk naar de 1 naast het gemarkeerde vak. Die heeft nog steeds één mijn nodig, en geen andere verborgen buur kan dat leveren.',
      success: 'Juist. Die aanwijzing had nog één mijn nodig, dus het gemarkeerde vak moest worden gemarkeerd.',
    },
    'safe-reveal': {
      title: 'Open het vak dat zeker veilig is',
      body: 'Deze 1 raakt zijn gemarkeerde mijn al, dus het gemarkeerde vak kan geen andere mijn verbergen.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'Die aanwijzing is al voldaan door de gemarkeerde mijn. Het gemarkeerde vak is de overblijvende verborgen buur, dus het is veilig.',
      success: 'Juist. Zodra die aanwijzing zijn mijn al heeft, kun je het gemarkeerde vak veilig openen.',
    },
    'diagonals-count': {
      title: 'Diagonale buren tellen ook mee',
      body: 'De zichtbare aanwijzingen leggen eerst de gemarkeerde mijn vast. Daarna telt de hoek-1 mee omdat diagonale buren ook meetellen.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'De zichtbare aanwijzingen forceren de gemarkeerde tegel al als mijn. Als je die diagonale mijn meetelt voor de hoek-1, is het gemarkeerde vak veilig.',
      success: 'Juist. De gemarkeerde tegel is een bekende mijn, en de hoekaanwijzing telt die diagonaal mee, dus het gemarkeerde vak kan worden geopend.',
    },
    'compare-clues': {
      title: 'Vergelijk twee aanwijzingen samen',
      body: 'Deze twee 1-aanwijzingen delen verborgen vakken. Zodra het gedeelde paar één mijn bevat, moet het extra vak bij de rechter 1 veilig zijn.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'Lees beide 1-aanwijzingen samen. Het gedeelde verborgen paar kan maar één mijn bevatten, dus het extra vak bij de rechter aanwijzing is veilig.',
      success: 'Juist. Door beide aanwijzingen te vergelijken zie je dat het gemarkeerde vak geen mijn kan verbergen.',
    },
    'guess-moments': {
      title: 'Soms is de volgende zet een gok',
      body: 'Niet elke puzzel biedt een volledig bewezen volgende zet. Op dit bord ondersteunt de verborgen bovenrand nog meer dan één mogelijke mijnopstelling.',
      prompt: 'Wanneer aanwijzingen niet één zet bewijzen, maak dan de rustigste gok die je kunt.',
      summary: 'Er past nog meer dan één mijnpatroon in de verborgen bovenrand, dus daar is nog geen enkele zet bewezen.',
      continueLabel: 'Verder',
    },
  },
  learningCenter: {
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
            title: `Veilige volgende zet bij ${clueLabel ?? 'dit cijfer'}`,
            body: `Open de gemarkeerde ${tileLabel}. Dit lokale cijferpatroon laat nog maar één mijnvak over, waardoor de andere verborgen ${tileLabel} veilig zijn.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Single-Mine Logic',
              explanationTitle: 'Uitleg',
              explanation: `Dit lokale cijferpatroon heeft nog precies één ${mineLabel} nodig. Zodra dat ene mijnvak vastligt, zijn de andere aangrenzende verborgen ${tileLabel} veilig.`,
            },
          };
        case 'all-mines-accounted-for':
          return {
            title: `Veilige volgende zet bij ${clueLabel ?? 'dit cijfer'}`,
            body: `Open de gemarkeerde ${tileLabel}. Rond ${clueLabel ?? 'dit cijfer'} zijn alle ${mineCount} ${mineLabel} al verklaard.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Alle mijnen al verklaard',
              explanationTitle: 'Uitleg',
              explanation: `Dit cijfer heeft al alle ${mineCount} ${mineLabel} die het nodig heeft, dus elk ander verborgen ${tileLabel} ernaast is veilig.`,
            },
          };
        case 'only-one-possible-mine':
          return {
            title: 'Veilige volgende zet door cijfers te vergelijken',
            body: `Open de gemarkeerde ${tileLabel}. Samen laten ${clueLabel ?? 'deze aanwijzing'} en ${secondaryClueLabel ?? 'de andere aanwijzing'} nog maar één legale plek voor de overblijvende mijn over.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Slechts één mogelijke mijn',
              explanationTitle: 'Uitleg',
              explanation: `Door deze cijfers samen te lezen blijft er precies één legale plek voor de overblijvende ${mineLabel} over. De extra verborgen ${tileLabel} buiten dat mijnvak zijn dus veilig.`,
            },
          };
        case 'guaranteed-safe-tile':
          return {
            title: `Veilige volgende zet bij ${clueLabel ?? 'dit cijfer'}`,
            body: `Open de gemarkeerde ${tileLabel}. Als dit vak een mijn was, zouden nabije cijfers te veel mijnen tellen.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Gegarandeerd veilig vak',
              explanationTitle: 'Uitleg',
              explanation: `Als het gemarkeerde ${tileLabel} een mijn was, zou minstens één naburig cijfer te veel ${mineLabel} hebben. Dat kan niet, dus dit vak is veilig.`,
            },
          };
        case 'full-clue-resolution':
          return {
            title: `Veilige volgende zet bij ${clueLabel ?? 'dit cijfer'}`,
            body: `Open de gemarkeerde ${tileLabel}. De mijnopgave van dit cijfer is volledig opgelost, dus de resterende verborgen ${tileLabel} zijn veilig.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Volledige cijferoplossing',
              explanationTitle: 'Uitleg',
              explanation: `De mijnopgave van dit cijfer is volledig opgelost door nabije geforceerde mijnvakken, dus de resterende verborgen ${tileLabel} ernaast zijn veilig.`,
            },
          };
        default:
          throw new Error(`Unhandled next move pattern: ${patternKey satisfies never}`);
      }
    },
    guess: {
      title: 'Nog geen zekere volgende zet',
      body: 'Geen aanwijzing wijst nu op een zekere veilige zet. Hier kan een gok nodig zijn, dus vertrouw op je beste lezing van het bord en vraag opnieuw na de volgende onthulling.',
    },
  },
} as const;

export default nl;
import type { LearningCenterPatternParams } from './index';
