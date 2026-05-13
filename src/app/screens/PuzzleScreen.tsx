import React from 'react';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTabs from '../navigation/PuzzleTabs';
import type { RootStackParamList } from '../navigation/types';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';

type Props = StackScreenProps<RootStackParamList, 'Puzzle'>;

export default function PuzzleScreen({ route }: Props) {
  const definition = getPuzzleDefinition(route.params.puzzleTypeId);

  return (
    <PuzzleTabs
      puzzleTypeId={route.params.puzzleTypeId}
      supportsTutorial={definition.supports.tutorial}
    />
  );
}
