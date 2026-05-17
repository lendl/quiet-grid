import React, { useEffect, useMemo, useState } from 'react';
import { View, ActivityIndicator, Text, StyleSheet } from 'react-native';
import type { Theme as NavigationTheme } from '@react-navigation/native';
import { NavigationContainer } from '@react-navigation/native';
import {
  createStackNavigator,
  TransitionPresets,
} from '@react-navigation/stack';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from './types';
import { hasSeenWelcome } from '../utils/settingsStorage';
import MainTabs             from './MainTabs';
import WelcomeScreen         from '../screens/WelcomeScreen';
import PuzzleScreen          from '../screens/PuzzleScreen';
import CompletionScreen      from '../screens/CompletionScreen';
import LossScreen            from '../screens/LossScreen';
import PuzzleAnalysisScreen  from '../screens/PuzzleAnalysisScreen';
import PuzzlePlayScreen      from '../screens/PuzzlePlayScreen';
import HowToPlayScreen       from '../screens/HowToPlayScreen';
import SupportInfoScreen     from '../screens/SupportInfoScreen';
import TutorialHostScreen    from '../screens/TutorialHostScreen';

const Stack = createStackNavigator<RootStackParamList>();

export default function AppNavigator() {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const [initialRoute, setInitialRoute] = useState<'MainTabs' | 'Welcome' | null>(null);
  const navigationTheme = useMemo<NavigationTheme>(() => ({
    dark: isDark,
    colors: {
      primary: theme.primary,
      background: theme.background,
      card: theme.background,
      text: theme.text,
      border: theme.border,
      notification: theme.primaryLight,
    },
  }), [isDark, theme]);

  useEffect(() => {
    let mounted = true;

    void hasSeenWelcome().then((seen) => {
      if (mounted) {
        setInitialRoute(seen ? 'MainTabs' : 'Welcome');
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  if (!initialRoute) {
    return (
      <View style={[styles.loading, { backgroundColor: theme.background }]}>
        <ActivityIndicator size="large" color={theme.primary} />
        <Text style={[styles.loadingText, { color: theme.textSecondary }]}>{strings.puzzlePlay.loading}</Text>
      </View>
    );
  }

  return (
    <NavigationContainer theme={navigationTheme}>
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{
          headerShown: false,
          cardStyle: { backgroundColor: theme.background },
          gestureEnabled: true,
          ...TransitionPresets.SlideFromRightIOS,
        }}
      >
        <Stack.Screen name="Welcome"         component={WelcomeScreen} />
        <Stack.Screen name="MainTabs"        component={MainTabs} />
        <Stack.Screen name="SupportInfo"     component={SupportInfoScreen} />
        <Stack.Screen name="Puzzle"          component={PuzzleScreen} />
        <Stack.Screen name="PuzzlePlay"      component={PuzzlePlayScreen} />
        <Stack.Screen name="Completion"      component={CompletionScreen} />
        <Stack.Screen name="Loss"            component={LossScreen} />
        <Stack.Screen name="Analysis"        component={PuzzleAnalysisScreen} />
        <Stack.Screen name="HowToPlay"       component={HowToPlayScreen} />
        <Stack.Screen
          name="Tutorial"
          component={TutorialHostScreen}
          initialParams={{ puzzleTypeId: 'takuzu', entry: 'startup' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  loadingText: {
    fontSize: 15,
  },
});
