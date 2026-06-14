const nl = {
  strings: {
    title: 'Takuzu',
    shortTitle: 'Takuzu',
    tagline: 'Vul het rooster met 0 en 1 via logica.',
    difficultyLabels: {
      easy: 'Makkelijk',
      medium: 'Gemiddeld',
      hard: 'Moeilijk',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Meer startcellen en mildere deducties.',
      medium: 'Gebalanceerde openingen die je vragen iets verder vooruit te lezen.',
      hard: 'Strakkere opzetten met minder vrije informatie en sterkere patroonherkenning.',
      expert: 'Schaarse openingen met aanhoudende deductiedruk over het hele bord.',
    },
    play: {
      metadataLabels: {
        size: 'Grootte',
        difficulty: 'Niveau',
      },
      helperToggle: {
        show: 'Toon volgende zet',
        hide: 'Verberg volgende zet',
      },
      noPuzzlesDialog: {
        title: 'Geen puzzels beschikbaar',
        message: (difficultyLabel: string) => `Geen puzzels gevonden in de ${difficultyLabel}-catalogus.`,
      },
      cellLabel: 'Cel',
      tutorial: {
        progressLabel: (step: number) => `Les ${step}`,
        introNote: 'Doel: vul het hele rooster zodat elke rij en kolom in balans blijft, uniek is en geen trio\'s bevat. In het echte spel tik je op de gemarkeerde cel om te wisselen tussen leeg, 0, 1 en weer leeg. In deze tutorial gebruik je de knoppen 0 en 1 hieronder.',
        exitLabel: {
          end: 'Tutorial beëindigen',
          skip: 'Tutorial overslaan',
        },
        status: {
          finishing: 'Tutorial wordt afgerond…',
          nextLesson: 'Volgende les start…',
          nextStep: 'Volgende stap start…',
        },
        selectAnswerLabel: (value: 0 | 1) => `Kies ${value}`,
      },
    },
  },
  howToPlayGoal: 'Vul elke cel met een 0 of 1 op basis van drie regels.',
  howToPlayControls: 'Tik op een cel om te wisselen: leeg → 0 → 1 → leeg.',
  howToPlayWrongMove: 'Een rij of kolom die een regel schendt knippert en kost 500 punten.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Geen drie op rij',
      body: 'Drie gelijke cijfers naast elkaar in een rij of kolom zijn niet toegestaan.',
    },
    {
      num: '2',
      title: 'Gelijke helften',
      body: 'Elke rij en kolom bevat evenveel 0\'en als 1\'en.',
    },
    {
      num: '3',
      title: 'Alle lijnen zijn uniek',
      body: 'Geen twee rijen zijn gelijk, en geen twee kolommen zijn gelijk.',
    },
  ],
  howToPlayTechniques: [
    {
      key: 'find-pairs',
      title: 'Zoek paren',
      body: 'Twee gelijke cijfers naast elkaar dwingen de cellen ernaast tot het tegenovergestelde cijfer.',
    },
    {
      key: 'avoid-trios',
      title: 'Vermijd trio\'s',
      body: 'Als hetzelfde cijfer aan beide kanten van een lege cel staat, moet die cel het tegenovergestelde zijn.',
    },
    {
      key: 'complete-lines',
      title: 'Maak rijen en kolommen af',
      body: 'Zodra een rij of kolom zo veel 0\'en (of 1\'en) heeft als toegestaan, moeten de overige cellen het andere cijfer zijn.',
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Schrap op basis van gevulde lijnen',
      body: 'Een rij of kolom mag geen kopie zijn van een voltooide lijn. Dreigt dat, gebruik dan de andere waarde.',
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Schrap onmogelijke combinaties',
      body: 'Als één waarde uiteindelijk drie op rij zou forceren, moet de andere waarde hier staan.',
    },
  ],
  howToPlayScoring: 'Start op 10.000 en daalt terwijl de timer loopt. Elke fout voltooide lijn kost 500 punten. De score daalt trager bij hogere moeilijkheden.',
  howToPlayTips: [
    {
      key: 'watch-for-flashes',
      title: 'Let op flitsen',
      body: 'Wanneer je een rij of kolom correct voltooit, lichten alle cellen kort op als bevestiging.',
    },
  ],
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
      body: 'Deze puzzel eindigde voordat hij kon worden opgelost.',
      icon: '⚠️',
    },
  },
  tutorialLessons: {
    'find-pairs': {
      title: 'Les 1: Zoek paren',
      body: 'Twee gelijke cijfers naast elkaar betekenen dat de cellen ernaast het tegenovergestelde cijfer moeten zijn.',
      prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
      retry: 'Niet deze. Als de gemarkeerde cel een 1 was, zou de rij beginnen met drie 1\'en op rij.',
      success: 'Correct. Er staan al twee 1\'en naast elkaar, dus de gemarkeerde cel moet 0 zijn.',
    },
    'avoid-trios': {
      title: 'Les 2: Vermijd trio\'s',
      body: 'Als hetzelfde cijfer verschijnt met een lege cel ertussen, moet die middelste cel het tegenovergestelde zijn.',
      prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
      retry: 'Niet deze. De gemarkeerde cel staat tussen twee 1\'en, dus hij kan niet ook 1 zijn.',
      success: 'Correct. De middelste cel moet 0 zijn zodat de rij geen drie 1\'en op rij vormt.',
    },
    'complete-lines': {
      title: 'Les 3: Maak rijen en kolommen af',
      body: 'Zodra het maximum van één cijfer in een lijn is bereikt, moeten alle overgebleven lege cellen het andere cijfer zijn.',
      prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
      retry: 'Niet deze. Deze rij heeft al alle 0\'en die erin mogen, dus de overblijvende cel moet 1 zijn.',
      success: 'Correct. De rij heeft al drie 0\'en, dus de overblijvende cel moet 1 zijn.',
    },
    'eliminate-filled-lines': {
      title: 'Les 4: Schrap op basis van gevulde lijnen',
      body: 'Als het invullen van een rij of kolom die identiek zou maken aan een al complete lijn, moeten die waarden worden omgewisseld.',
      prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
      retry: 'Niet deze. Die keuze zou de onderste rij gelijk maken aan de complete rij erboven.',
      success: 'Correct. Door deze waarde om te wisselen blijft de onderste rij anders dan de complete rij.',
    },
    'eliminate-impossible-combinations': {
      title: 'Les 5: Schrap onmogelijke combinaties',
      body: 'Kijk naar de gemarkeerde cel en de resterende lege vakken in de rij. Gebruik het patroon om te bepalen welke waarde daar past.',
      prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
      retry: 'Niet deze. Als de gemarkeerde cel een 1 was, zouden de resterende lege vakken een trio forceren.',
      success: 'Correct. Kiezen voor 0 voorkomt het trio dat een 1 later in de rij zou forceren.',
    },
  },
  learningCenter: {
    pausedNextMove: {
      title: 'Nog geen duidelijke volgende zet',
      body: 'Dit deel van de puzzel biedt nu geen sterke volgende zet. Probeer een andere rij of kolom en vraag daarna opnieuw.',
    },
    findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Volgende zet in ${lineLabel}`,
        body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: er staan al twee ${repeatedValue}'en naast elkaar in ${lineLabel}, dus nog een ${repeatedValue} zou drie op rij maken.`,
      };
    },
    avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1) {
      return {
        title: `Volgende zet in ${lineLabel}`,
        body: `Plaats ${targetValue} in de gemarkeerde cel. Waarom: ${lineLabel} toont al ${repeatedValue} _ ${repeatedValue}, dus de open cel ertussen moet ${targetValue} zijn om drie op rij te vermijden.`,
      };
    },
    completeLines(lineLabel: string, filledValue: 0 | 1, filledCount: number, targetValue: 0 | 1, cellLabel: string) {
      return {
        title: `Volgende zet in ${lineLabel}`,
        body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: ${lineLabel} heeft al ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, dus de resterende open ${cellLabel} moeten ${targetValue} zijn om de lijn in balans te houden.`,
      };
    },
    eliminateFilledLines(lineLabel: string, matchingLineLabel: string, targetValue: 0 | 1, cellLabel: string, lineKindLabel: string) {
      return {
        title: `Volgende zet in ${lineLabel}`,
        body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: als ${lineLabel} gelijk werd aan ${matchingLineLabel}, zouden complete ${lineKindLabel} niet uniek meer zijn.`,
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
          ? 'een tegenspraak met drie op rij'
          : contradictionKind === 'balance'
            ? 'een balans-tegenspraak'
            : 'een tegenspraak met een dubbele complete lijn';

      return {
        title: `Volgende zet in ${lineLabel}`,
        body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: als deze ${cellLabel} ${blockedValue} was, zou het volgen van ${proofRuleLabel} ${contradictionLabel} in ${contradictionLineLabel} afdwingen, dus ${targetValue} ligt hier vast. ${lineLabel} heeft nog ${validCompletionCount} geldige lijncombinatie${validCompletionCount === 1 ? '' : 's'} om te vergelijken, maar alleen deze waarde vermijdt die tegenspraak.`,
      };
    },
    avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1) {
      return {
        title: `Volgende zet om ${lineLabel} te herstellen`,
        body: `Pas een gemarkeerde cel in ${lineLabel} aan. Waarom: drie ${repeatedValue}'en op rij breken de regel zonder trio's.`,
      };
    },
    completeLinesRepair(lineLabel: string, filledValue: 0 | 1, filledCount: number, limit: number) {
      return {
        title: `Volgende zet om ${lineLabel} opnieuw in balans te brengen`,
        body: `Pas een gemarkeerde cel in ${lineLabel} aan. Waarom: ${lineLabel} bevat al ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, maar de limiet is ${limit}.`,
      };
    },
    eliminateFilledLinesRepair(firstLineLabel: string, secondLineLabel: string, lineLabel: string) {
      return {
        title: `Volgende zet om gelijke ${lineLabel} te scheiden`,
        body: `Pas een gemarkeerde cel aan. Waarom: ${firstLineLabel} en ${secondLineLabel} zijn gelijk, maar complete ${lineLabel} moeten uniek blijven.`,
      };
    },
  },
} as const;

export default nl;
