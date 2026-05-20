import React from 'react';
import type { StackScreenProps } from '@react-navigation/stack';
import GameTabs from '../navigation/GameTabs';
import type { RootStackParamList } from '../navigation/types';

type Props = StackScreenProps<RootStackParamList, 'Game'>;

export default function GameScreen({ route }: Props) {
  return (
    <GameTabs
      gameId={route.params.gameId}
      initialTab={route.params.initialTab}
      initialDirection={route.params.initialDirection}
    />
  );
}
