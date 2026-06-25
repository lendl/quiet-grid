import React from 'react';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import GameTabs from '../navigation/GameTabs';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Game'>;

export default function GameScreen({ route }: Props) {
  return (
    <GameTabs
      gameId={route.params.gameId}
      initialTab={route.params.initialTab}
      initialDirection={route.params.initialDirection}
    />
  );
}
