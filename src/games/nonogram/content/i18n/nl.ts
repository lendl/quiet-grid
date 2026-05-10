import type { NonogramI18n } from './index';

const nl: NonogramI18n = {
  strings: {
    title: 'Nonogram',
    shortTitle: 'Nonogram',
    tagline: 'Teken het plaatje met pure logica.',
    difficultyLabels: {
      easy: 'Makkelijk',
      medium: 'Gemiddeld',
      hard: 'Moeilijk',
      expert: 'Expert',
    },
    difficultyDescriptions: {
      easy: 'Grote aanwijzingen en meteen geforceerde zetten.',
      medium: 'Vraagt overlap en kruislijn-logica.',
      hard: 'Nog niet gebruikt voor Nonogram.',
      expert: 'Nog niet gebruikt voor Nonogram.',
    },
    play: {
      metadataLabels: {
        size: 'Grootte',
        difficulty: 'Moeilijkheid',
      },
      helperToggle: {
        show: 'Toon volgende zet',
        hide: 'Verberg volgende zet',
      },
      noPuzzlesDialog: {
        title: 'Nog geen Nonogram-puzzels',
        message: 'Genereer eerst Nonogram-puzzels voor deze moeilijkheid.',
      },
    },
    tutorial: {
      progressLabel: (step, total) => `Les ${step} van ${total}`,
      exitLabel: {
        end: 'Stoppen',
        skip: 'Overslaan',
      },
      status: {
        finishing: 'Afronden...',
        nextLesson: 'Volgende les...',
      },
      actionLabels: {
        filled: 'Vullen',
        marked: 'Markeer met X',
      },
    },
    analyzer: {
      legendEvidence: 'Bewijs',
      legendTarget: 'Volgende zet',
      noAnalysisTitle: 'Geen logische vervolgstap',
      noAnalysisBody: 'Vanaf deze stand is er geen eenvoudige logische stap beschikbaar.',
    },
  },
  howToPlay: {
    rules: [
      {
        num: '1',
        title: 'Lees de aanwijzingen',
        body: 'De aanwijzingen per rij en kolom geven de lengtes van de gevulde blokken in die lijn.',
      },
      {
        num: '2',
        title: 'Houd blokken gescheiden',
        body: 'Tussen twee verschillende blokken moet minstens een lege cel zitten.',
      },
      {
        num: '3',
        title: 'Gebruik vullen en X-markeringen',
        body: 'Vul cellen die zeker bij het plaatje horen en markeer onmogelijke cellen met een X.',
      },
    ],
    tips: [
      {
        key: 'overlap',
        title: 'Gebruik overlap',
        body: 'Als een blok maar in een klein bereik kan starten, zijn de middelste cellen vaak verplicht.',
      },
      {
        key: 'empty-lines',
        title: 'Markeer onmogelijke cellen',
        body: 'Als elke geldige plaatsing een cel leeg laat, markeer die cel dan met een X.',
      },
      {
        key: 'cross-check',
        title: 'Kruis rijen en kolommen',
        body: 'Een nieuwe vulling in een rij ontgrendelt vaak een geforceerde zet in een kolom, en andersom.',
      },
    ],
  },
  loss: {
    forfeit: {
      eyebrow: 'Nonogram',
      title: 'Puzzel opgegeven',
      body: 'Bekijk de volgende logische stappen en zie hoe het plaatje ontstaat.',
      icon: 'flag-outline',
    },
    'rule-based': {
      eyebrow: 'Nonogram',
      title: 'Puzzel beëindigd',
      body: 'Bekijk het bord en volg vanaf hier de logische voortzetting.',
      icon: 'help-circle-outline',
    },
  },
  tutorialLessons: {
    'read-clues': {
      title: 'Lees de lijn-aanwijzing',
      body: 'Een aanwijzing van 3 betekent dat deze drie cellen samen een gevuld blok vormen.',
      prompt: 'Tik op de cellen die zeker gevuld moeten zijn.',
      retry: 'Nog niet. Het blok van 3 heeft nog steeds maar één gedwongen overlap.',
      success: 'Precies. Die cellen horen bij het enige blok van 3.',
    },
    'forced-fill': {
      title: 'Vind de geforceerde vulling',
      body: 'Deze rij-aanwijzing is zo groot dat de middelste cellen zeker gevuld zijn.',
      prompt: 'Vul de geforceerde cellen.',
      retry: 'Kijk naar de cellen die in elke geldige plaatsing terugkomen.',
      success: 'Juist. Die middelste cellen overlappen altijd.',
    },
    'forced-mark': {
      title: 'Markeer onmogelijke cellen',
      body: 'De aanwijzing is al voldaan, dus de rest van de lijn moet leeg blijven.',
      prompt: 'Markeer de onmogelijke cellen met een X.',
      retry: 'Deze cellen kunnen niet meer bij een geldig blok horen.',
      success: 'Correct. X-markeringen houden de lijn helder.',
    },
    'combine-lines': {
      title: 'Gebruik beide richtingen',
      body: 'Een gevulde cel in de rij beperkt ook de bijbehorende kolom.',
      prompt: 'Vul de volgende geforceerde cel.',
      retry: 'Kijk waar de rij- en kolom-aanwijzingen hetzelfde afdwingen.',
      success: 'Mooi. Door lijnen te kruisen vond je de volgende zet.',
    },
    'tap-cycle': {
      title: 'Gebruik de tik-cyclus',
      body: 'Tik eenmaal om te vullen, nog eens voor een X, en nog eens om te wissen.',
      prompt: 'Markeer de cel met een X.',
      retry: 'Deze zet vraagt om een X, niet om een vulling.',
      success: 'Perfect. Zo wissel je tussen vullen, X en wissen.',
    },
  },
  analysis: {
    legendEvidence: 'Bewijs',
    legendTarget: 'Volgende zet',
    pausedNextMove: {
      title: 'Volgende zet is gepauzeerd',
      body: 'Schakel de helper weer in om één zekere logische zet te zien.',
    },
    overlapFill: (lineLabel, clueLabel, cellCount) => ({
      title: 'Overlap dwingt een vulling af',
      body: `${lineLabel} met aanwijzing ${clueLabel} bedekt ${cellCount === 1 ? 'deze cel' : 'deze cellen'} altijd, dus vul ze.`,
    }),
    forcedEmpty: (lineLabel, clueLabel, cellCount) => ({
      title: 'Deze cellen moeten leeg blijven',
      body: `${lineLabel} met aanwijzing ${clueLabel} kan geen blok plaatsen op ${cellCount === 1 ? 'deze cel' : 'deze cellen'}, dus markeer ze met een X.`,
    }),
    completeLine: (lineLabel, clueLabel) => ({
      title: 'Er past maar één plaatsing',
      body: `${lineLabel} met aanwijzing ${clueLabel} heeft vanuit deze stand nog maar één geldige plaatsing.`,
    }),
    groupedStep: (kind, lineLabel, clueLabel, cellCount) => ({
      title: kind === 'filled' ? 'Pas de geforceerde vullingen toe' : 'Markeer de verplichte X-cellen',
      body: `${lineLabel} met aanwijzing ${clueLabel} geeft ${cellCount === 1 ? 'één geforceerde cel' : `${cellCount} geforceerde cellen`}.`,
    }),
    lineLabel: (axis, index) => `${axis === 'row' ? 'Rij' : 'Kolom'} ${index + 1}`,
    clueLabel: (clues) => clues.length === 1 && clues[0] === 0 ? '0' : clues.join(' '),
  },
};

export default nl;
