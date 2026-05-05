import { getCurrentLanguage } from '../../../app/i18n';

export type HowToPlayCellValue = 0 | 1 | null | 'a0' | 'a1';

export type HowToPlayTipKey =
  | 'find-pairs'
  | 'avoid-trios'
  | 'complete-lines'
  | 'eliminate-filled-lines'
  | 'eliminate-impossible-combinations'
  | 'score-matters'
  | 'watch-for-flashes';

export interface HowToPlayTip {
  key: HowToPlayTipKey;
  title: string;
  body: string;
  example?: HowToPlayCellValue[][];
}

export function getBinaryHowToPlayTips(): HowToPlayTip[] {
  if (getCurrentLanguage() === 'nl') {
    return [
      {
        key: 'find-pairs',
        title: 'Zoek paren',
        body: 'Twee gelijke cijfers naast elkaar betekenen dat de cellen ernaast het tegenovergestelde cijfer moeten zijn.',
        example: [[0, 0, 'a1']],
      },
      {
        key: 'avoid-trios',
        title: 'Vermijd trio\'s',
        body: 'Als hetzelfde cijfer verschijnt met een lege cel ertussen, moet die middelste cel het tegenovergestelde zijn.',
        example: [[1, 'a0', 1]],
      },
      {
        key: 'complete-lines',
        title: 'Maak rijen en kolommen af',
        body: 'Zodra het maximum van één cijfer in een lijn is bereikt, moeten alle overgebleven lege cellen het andere cijfer zijn.',
        example: [[0, 1, 0, 1, 0, 'a1']],
      },
      {
        key: 'eliminate-filled-lines',
        title: 'Schrap op basis van gevulde lijnen',
        body: 'Als het invullen van een rij of kolom die identiek zou maken aan een al complete lijn, moeten die waarden worden omgewisseld.',
        example: [
          [1, 0, 1, 0, 1, 0],
          [1, 0, 'a0', 'a1', 1, 0],
        ],
      },
      {
        key: 'eliminate-impossible-combinations',
        title: 'Schrap onmogelijke combinaties',
        body: 'Als de gemarkeerde waarde een één zou zijn, zouden de resterende lege vakken een trio forceren. Omdat dat niet mag, moet de gemarkeerde waarde nul zijn.',
        example: [[1, 1, 0, null, null, 'a0']],
      },
      {
        key: 'score-matters',
        title: 'Hoe scoren werkt',
        body: 'Je score start op 10.000 en daalt terwijl de timer loopt. Elke voltooide lijn die niet met de oplossing overeenkomt kost 500 punten. Hogere moeilijkheden verliezen langzamer score.',
      },
      {
        key: 'watch-for-flashes',
        title: 'Let op flitsen',
        body: 'Wanneer je een rij of kolom correct voltooit, lichten alle cellen kort op als bevestiging.',
      },
    ];
  }

  return [
    {
      key: 'find-pairs',
      title: 'Find pairs',
      body: 'Two adjacent identical digits mean the cells on either side must be the opposite digit.',
      example: [[0, 0, 'a1']],
    },
    {
      key: 'avoid-trios',
      title: 'Avoid trios',
      body: 'If the same digit appears with one empty cell between them, that middle cell must be the opposite.',
      example: [[1, 'a0', 1]],
    },
    {
      key: 'complete-lines',
      title: 'Complete rows and columns',
      body: 'Once the maximum count of one digit is reached in a line, all remaining empty cells must be the other digit.',
      example: [[0, 1, 0, 1, 0, 'a1']],
    },
    {
      key: 'eliminate-filled-lines',
      title: 'Eliminate based on filled lines',
      body: 'If filling a row or column would make it identical to an already-complete one, those values must be swapped.',
      example: [
        [1, 0, 1, 0, 1, 0],
        [1, 0, 'a0', 'a1', 1, 0],
      ],
    },
    {
      key: 'eliminate-impossible-combinations',
      title: 'Eliminate impossible combinations',
      body: 'If the highlighted value were one, the remaining blanks would force a trio. Because that is not allowed, the highlighted value must be zero.',
      example: [[1, 1, 0, null, null, 'a0']],
    },
    {
      key: 'score-matters',
      title: 'How scoring works',
      body: 'Your score starts at 10,000 and drops while the puzzle timer runs. Each completed line that does not match the solution subtracts 500 points. Harder difficulties lose score more slowly.',
    },
    {
      key: 'watch-for-flashes',
      title: 'Watch for flashes',
      body: 'When you correctly complete a row or column, all its cells briefly flash as confirmation.',
    },
  ];
}
