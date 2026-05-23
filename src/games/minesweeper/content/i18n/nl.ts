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
    play: {
      metadataLabels: {
        size: 'Grootte',
        difficulty: 'Niveau',
        minesLeft: 'Mijnen',
      },
      helperToggle: {
        show: 'Toon volgende zet',
        hide: 'Verberg volgende zet',
      },
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
    abandoned: {
      eyebrow: 'Puzzel beëindigd',
      title: 'Puzzel niet af',
      body: 'Je hebt deze puzzel beëindigd voordat hij was opgelost.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Puzzel verloren',
      title: 'Puzzel verloren',
      body: 'Deze puzzel eindigde toen een mijn werd geopend. Er staat een nieuwe puzzel klaar wanneer jij wilt.',
      icon: '💣',
    },
  },
  analysis: {
    lossSummary({ safeCount, mineCount }: { safeCount: number; mineCount: number }) {
      if (safeCount > 0 && mineCount > 0) {
        return {
          title: 'Er waren logische zetten beschikbaar',
          body: `Op dit bord waren al ${safeCount} veilige ${safeCount === 1 ? 'zet' : 'zetten'} om te openen en ${mineCount} zekere ${mineCount === 1 ? 'mijn' : 'mijnen'} om te markeren.`,
        };
      }

      if (safeCount > 0) {
        return {
          title: 'Er waren veilige openingen beschikbaar',
          body: `Op dit bord waren al ${safeCount} veilige ${safeCount === 1 ? 'zet' : 'zetten'} om te openen op basis van de getoonde cijfers.`,
        };
      }

      return {
        title: 'Er waren zekere markeringen beschikbaar',
        body: `Op dit bord waren al ${mineCount} zekere ${mineCount === 1 ? 'mijn' : 'mijnen'} om te markeren op basis van de getoonde cijfers.`,
      };
    },
    groupedFlagStep({ mineCount }: { mineCount: number }) {
      return {
        title: 'Zekere mijnen op dit bord',
        body: `Je kunt de gemarkeerde ${mineCount === 1 ? 'tegel' : 'tegels'} nu markeren. De huidige cijfers bewijzen al dat ${mineCount === 1 ? 'dit vak een mijn is' : 'deze vakken mijnen zijn'}.`,
      };
    },
    groupedSafeStep({ targetCount, reasonCount }: { targetCount: number; reasonCount: number }) {
      return {
        title: 'Meerdere aanwijzingen steunen deze veilige opening',
        body: `Open de gemarkeerde ${targetCount === 1 ? 'tegel' : 'tegels'}. ${reasonCount} cijferpatronen wijzen onafhankelijk naar dezelfde veilige zet, dus deze opening is van meer dan één kant bevestigd.`,
      };
    },
    legendEvidence: 'Bewijs',
    legendSafe: 'Veilig openen',
    legendMine: 'Mijn markeren',
  },
  tutorialText: {
    'goal-and-stakes': {
      title: 'Open elk veilig vak',
      body: 'Open elk vak zonder mijn. Als je een mijn opent, eindigt de run direct.',
      prompt: 'Vlaggen helpen gevaar bij te houden, maar alleen veilige openingen winnen het bord.',
      summary: 'Je wint door elk veilig vak te openen. Je verliest door een mijn te openen.',
      continueLabel: 'Verder',
    },
    'core-actions': {
      title: 'Je hebt twee acties',
      body: 'In een echte puzzel tik je op een verborgen vak om het te openen. Houd je een verborgen vak ingedrukt, dan plaats of verwijder je een vlag.',
      prompt: 'In deze tutorial kies je Openen of Markeren met de knoppen onderaan. Op een echt bord opent tikken en markeert ingedrukt houden.',
      summary: 'Openen maakt een vak zichtbaar. Ingedrukt houden wisselt een vlag. Vlaggen helpen mijnen bij te houden, maar lossen het bord niet vanzelf op.',
      continueLabel: 'Verder',
    },
    'reading-clues': {
      title: 'Cijfers tellen alle rakende mijnen',
      body: 'Elk cijfer toont hoeveel mijnen dat vak raken in alle acht omliggende posities.',
      prompt: 'Diagonale buren tellen ook mee.',
      summary: 'Een aanwijzing zegt hoeveel mijnen raken, niet meteen waar ze precies liggen.',
      continueLabel: 'Verder',
    },
    'forced-flag': {
      title: 'Markeer het vak dat zeker een mijn verbergt',
      body: 'Deze aanwijzing heeft nog één mijn nodig, en het gemarkeerde vak is de enige verborgen plek die overblijft.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'Deze aanwijzing heeft nog één mijn nodig, en geen andere verborgen buur kan die leveren.',
      success: 'Juist. Het gemarkeerde vak moest een mijn zijn, dus Markeren is correct.',
    },
    'safe-reveal': {
      title: 'Open het vak dat zeker veilig is',
      body: 'Deze aanwijzing heeft zijn mijn al, dus de gemarkeerde buur kan geen extra mijn verbergen.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'De aanwijzing is al voldaan door de gemarkeerde mijn, dus de overblijvende verborgen buur is veilig.',
      success: 'Juist. Zodra de aanwijzing zijn mijn al heeft, kun je het gemarkeerde vak veilig openen.',
    },
    'compare-clues': {
      title: 'Vergelijk aanwijzingen samen',
      body: 'Deze aanwijzingen delen verborgen vakken. Zodra de gedeelde groep het mijnaantal opvangt, wordt het extra vak veilig.',
      prompt: 'Wat moet je doen met het gemarkeerde vak?',
      retry: 'Lees beide aanwijzingen samen. De gedeelde verborgen vakken nemen het mijnaantal op, dus het extra vak is veilig.',
      success: 'Juist. Door beide aanwijzingen te vergelijken bewijs je dat het gemarkeerde vak geen mijn kan zijn.',
    },
    'advanced-patterns': {
      title: 'Sommige patronen gebruiken diagonalen en overlap',
      body: 'Hier telt de hoek-1 de gemarkeerde diagonale mijn mee, en de 2 en 1 ernaast lezen allebei dezelfde verborgen bovenstrook.',
      prompt: 'Een hoekaanwijzing telt diagonalen mee, en nabije aanwijzingen kunnen dezelfde verborgen vakken delen.',
      summary: 'Lees niet maar één aanwijzing wanneer een diagonale mijn of een gedeelde verborgen strook verandert wat nabije aanwijzingen betekenen.',
      continueLabel: 'Verder',
    },
    'guess-and-help': {
      title: 'Soms raakt logica op',
      body: 'Deze bovenrand laat nog meer dan één mogelijke mijnopstelling toe. De rij met 1-en vertelt niet welke verborgen vakken de mijnen zijn.',
      prompt: 'Maak dan de rustigste gok die je kunt en gebruik hulp wanneer dat nodig is.',
      summary: 'Verschillende mijnopstellingen kunnen bij dezelfde aanwijzingen passen. Hints helpen, undo herstelt mis-taps en je kunt deze tutorial later opnieuw spelen.',
      continueLabel: 'Afronden',
    },
  },
  learningCenter: {
    formatCellLabel({ row, col }: { row: number; col: number }) {
      return `rij ${row + 1}, kolom ${col + 1}`;
    },
    tileLabel(count: number) {
      return count === 1 ? 'vak' : 'vakken';
    },
    mineLabel(count: number) {
      return count === 1 ? 'mijn' : 'mijnen';
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
            title: `Zekere mijn bij ${clueLabel ?? 'dit cijfer'}`,
            body: `Markeer de gemarkeerde ${tileLabel}. ${clueLabel ?? 'Dit cijfer'} heeft nog ${mineCount} ${mineLabel} nodig, en de gemarkeerde verborgen ${tileLabel} zijn de enige plekken die over zijn.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Direct lokaal mijnpatroon',
              explanationTitle: 'Uitleg',
              explanation: `Dit cijfer heeft nog ${mineCount} ${mineLabel} nodig. Omdat alleen de gemarkeerde verborgen ${tileLabel} ernaast overblijven, moet elk gemarkeerd vak een mijn zijn.`,
            },
          };
        case 'subset-difference':
          return {
            title: 'Zekere mijn door cijfers te vergelijken',
            body: `Markeer de gemarkeerde ${tileLabel}. Door ${clueLabel ?? 'deze aanwijzing'} met ${secondaryClueLabel ?? 'de andere aanwijzing'} te vergelijken zie je dat de extra verborgen ${tileLabel} de overblijvende ${mineLabel} moeten bevatten.`,
            teaching: {
              patternTitle: 'Patroon',
              patternLabel: 'Subset-mijnverschil',
              explanationTitle: 'Uitleg',
              explanation: `De verborgen vakken van het kleinere cijfer passen binnen die van het grotere cijfer. Na de gedeelde mijnplekken blijven de extra verborgen ${tileLabel} over als plaats voor de resterende ${mineLabel}.`,
            },
          };
        default:
          throw new Error(`Unhandled flag move pattern: ${reason satisfies never}`);
      }
    },
    guess: {
      title: 'Nog geen zekere volgende zet',
      body: 'Geen aanwijzing wijst nu op een zekere veilige zet. Hier kan een gok nodig zijn, dus vertrouw op je beste lezing van het bord en vraag opnieuw na de volgende onthulling.',
    },
  },
  tutorialUi: {
    progressLabel: (step: number) => `Les ${step}`,
    exitLabel: {
      end: 'Tutorial beëindigen',
      skip: 'Tutorial overslaan',
    },
    status: {
      finishing: 'Tutorial wordt afgerond\u2026',
      nextLesson: 'Volgende les start\u2026',
    },
    highlightedTile: 'Gemarkeerd tutorialvak',
  },
} as const;

export default nl;
import type { LearningCenterMineFlagParams, LearningCenterPatternParams } from './index';
