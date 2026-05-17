import React from 'react';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTabs from '../navigation/PuzzleTabs';
import type { RootStackParamList } from '../navigation/types';

type Props = StackScreenProps<RootStackParamList, 'Puzzle'>;

export default function PuzzleScreen({ route }: Props) {
  return (
    <PuzzleTabs
      puzzleTypeId={route.params.puzzleTypeId}
      initialTab={route.params.initialTab}
      initialDirection={route.params.initialDirection}
    />
  );
}
