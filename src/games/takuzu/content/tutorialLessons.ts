import type { HowToPlayCellValue, HowToPlayTip, HowToPlayTipKey } from './howToPlayTips';
import { getCurrentLanguage } from '../../../app/i18n';
import { getTakuzuHowToPlayTips } from './howToPlayTips';
import type { TutorialLesson } from '../types';

type ExampleTip = HowToPlayTip & { example: HowToPlayCellValue[][] };

function getTip(key: HowToPlayTipKey): ExampleTip {
  const tip = getTakuzuHowToPlayTips().find((entry) => entry.key === key);
  if (!tip || !tip.example) {
    throw new Error(`Missing tutorial tip example for ${key}`);
  }

  return tip as ExampleTip;
}

const findPairsTip = getTip('find-pairs');
const avoidTriosTip = getTip('avoid-trios');
const completeLinesTip = getTip('complete-lines');
const filledLinesTip = getTip('eliminate-filled-lines');
const impossibleTip = getTip('eliminate-impossible-combinations');

function toGridAndMoves(example: HowToPlayCellValue[][]) {
  const moves: TutorialLesson['moves'] = [];
  const grid = example.map((row, rowIndex) =>
    row.map((cell, colIndex) => {
      if (cell === 'a0') {
        moves.push({ row: rowIndex, col: colIndex, value: 0 });
        return null;
      }

      if (cell === 'a1') {
        moves.push({ row: rowIndex, col: colIndex, value: 1 });
        return null;
      }

      return cell;
    }),
  );

  return { grid, moves };
}

const pairsExample = toGridAndMoves(findPairsTip.example);
const triosExample = toGridAndMoves(avoidTriosTip.example);
const completeLinesExample = toGridAndMoves(completeLinesTip.example);
const filledLinesExample = toGridAndMoves(filledLinesTip.example);
const impossibleExample = toGridAndMoves(impossibleTip.example);

export function getTakuzuTutorialLessons(): TutorialLesson[] {
  if (getCurrentLanguage() === 'nl') {
    return [
      {
        key: 'find-pairs',
        title: `Les 1: ${findPairsTip.title}`,
        body: findPairsTip.body,
        prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
        retry: 'Niet deze. Als de gemarkeerde cel een 1 was, zou de rij beginnen met drie 1\'en op rij.',
        success: 'Correct. Er staan al twee 1\'en naast elkaar, dus de gemarkeerde cel moet 0 zijn.',
        grid: pairsExample.grid,
        moves: pairsExample.moves,
      },
      {
        key: 'avoid-trios',
        title: `Les 2: ${avoidTriosTip.title}`,
        body: avoidTriosTip.body,
        prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
        retry: 'Niet deze. De gemarkeerde cel staat tussen twee 1\'en, dus hij kan niet ook 1 zijn.',
        success: 'Correct. De middelste cel moet 0 zijn zodat de rij geen drie 1\'en op rij vormt.',
        grid: triosExample.grid,
        moves: triosExample.moves,
      },
      {
        key: 'complete-lines',
        title: `Les 3: ${completeLinesTip.title}`,
        body: completeLinesTip.body,
        prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
        retry: 'Niet deze. Deze rij heeft al alle 0\'en die erin mogen, dus de overblijvende cel moet 1 zijn.',
        success: 'Correct. De rij heeft al drie 0\'en, dus de overblijvende cel moet 1 zijn.',
        grid: completeLinesExample.grid,
        moves: completeLinesExample.moves,
      },
      {
        key: 'eliminate-filled-lines',
        title: `Les 4: ${filledLinesTip.title}`,
        body: filledLinesTip.body,
        prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
        retry: 'Niet deze. Die keuze zou de onderste rij gelijk maken aan de complete rij erboven.',
        success: 'Correct. Door deze waarde om te wisselen blijft de onderste rij anders dan de complete rij.',
        grid: filledLinesExample.grid,
        moves: filledLinesExample.moves,
      },
      {
        key: 'eliminate-impossible-combinations',
        title: `Les 5: ${impossibleTip.title}`,
        body: 'Kijk naar de gemarkeerde cel en de resterende lege vakken in de rij. Gebruik het patroon om te bepalen welke waarde daar past.',
        prompt: 'Moet de gemarkeerde cel een 1 of een 0 zijn?',
        retry: 'Niet deze. Als de gemarkeerde cel een 1 was, zouden de resterende lege vakken een trio forceren.',
        success: 'Correct. Kiezen voor 0 voorkomt het trio dat een 1 later in de rij zou forceren.',
        grid: impossibleExample.grid,
        moves: impossibleExample.moves,
      },
    ];
  }

  return [
    {
      key: 'find-pairs',
      title: `Lesson 1: ${findPairsTip.title}`,
      body: findPairsTip.body,
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. If the highlighted cell were 1, the row would start with three 1s in a row.',
      success: 'Correct. Two 1s already sit together, so the highlighted cell should be 0.',
      grid: pairsExample.grid,
      moves: pairsExample.moves,
    },
    {
      key: 'avoid-trios',
      title: `Lesson 2: ${avoidTriosTip.title}`,
      body: avoidTriosTip.body,
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. The highlighted cell sits between two 1s, so it cannot also be 1.',
      success: 'Correct. The middle cell should be 0 so the row does not form three 1s in a row.',
      grid: triosExample.grid,
      moves: triosExample.moves,
    },
    {
      key: 'complete-lines',
      title: `Lesson 3: ${completeLinesTip.title}`,
      body: completeLinesTip.body,
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. This row already has all the 0s it can hold, so the remaining cell should be 1.',
      success: 'Correct. The row already has three 0s, so the remaining cell should be 1.',
      grid: completeLinesExample.grid,
      moves: completeLinesExample.moves,
    },
    {
      key: 'eliminate-filled-lines',
      title: `Lesson 4: ${filledLinesTip.title}`,
      body: filledLinesTip.body,
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. That choice would make the lower row match the completed row above it.',
      success: 'Correct. Swapping this value keeps the lower row different from the completed row.',
      grid: filledLinesExample.grid,
      moves: filledLinesExample.moves,
    },
    {
      key: 'eliminate-impossible-combinations',
      title: `Lesson 5: ${impossibleTip.title}`,
      body: 'Look at the highlighted cell and the remaining blanks in the row. Use the pattern to work out which value fits there.',
      prompt: 'Should the highlighted cell be a 1 or a 0?',
      retry: 'Not this one. If the highlighted cell were 1, the remaining blanks would force a trio.',
      success: 'Correct. Choosing 0 avoids the trio that a 1 would force later in the row.',
      grid: impossibleExample.grid,
      moves: impossibleExample.moves,
    },
  ];
}
