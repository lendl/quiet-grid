import type { NavigationProp } from '@react-navigation/native';
import type { RootStackParamList } from './types';

export function returnToHome<RouteName extends keyof RootStackParamList>(
  navigation: NavigationProp<RootStackParamList, RouteName>,
): void {
  navigation.navigate('MainTabs', { screen: 'Games' });
}
