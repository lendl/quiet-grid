import { StackActions } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RootStackParamList } from './types';

export function returnToHome<RouteName extends keyof RootStackParamList>(
  navigation: StackNavigationProp<RootStackParamList, RouteName>,
): void {
  const state = navigation.getState();
  const currentRoute = state.routes[state.index];
  const homeIndex = state.routes.findIndex((route) => route.name === 'Home');

  if (homeIndex >= 0 && homeIndex < state.index) {
    navigation.dispatch(StackActions.pop(state.index - homeIndex));
    return;
  }

  if (currentRoute?.name !== 'Home') {
    navigation.replace('Home');
  }
}
