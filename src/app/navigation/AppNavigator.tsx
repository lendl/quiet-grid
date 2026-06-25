import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, ActivityIndicator, Text, StyleSheet } from 'react-native';
import type { Theme as NavigationTheme } from '@react-navigation/native';
import { DefaultTheme, NavigationContainer, createNavigationContainerRef } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from './types';
import { hasSeenWelcome } from '../utils/settingsStorage';
import { loadActiveSessionState } from '../utils/activeSessionStateStorage';
import type { ActiveSession } from '../shell/activeSessionTypes';
import MainTabs             from './MainTabs';
import WelcomeScreen         from '../screens/WelcomeScreen';
import GameScreen            from '../screens/GameScreen';
import CompletionScreen      from '../screens/CompletionScreen';
import LossScreen            from '../screens/LossScreen';
import PuzzleAnalysisScreen  from '../screens/PuzzleAnalysisScreen';
import PuzzlePlayScreen      from '../screens/PuzzlePlayScreen';
import HowToPlayScreen       from '../screens/HowToPlayScreen';
import SupportInfoScreen     from '../screens/SupportInfoScreen';
import TechniqueLessonScreen from '../screens/TechniqueLessonScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();
const navigationRef = createNavigationContainerRef<RootStackParamList>();

export default function AppNavigator() {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const [initialRoute, setInitialRoute] = useState<'MainTabs' | 'Welcome' | null>(null);
  const [startupSession, setStartupSession] = useState<ActiveSession | null>(null);
  const navigationTheme = useMemo<NavigationTheme>(() => ({
    ...DefaultTheme,
    dark: isDark,
    colors: {
      ...DefaultTheme.colors,
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

    void Promise.all([hasSeenWelcome(), loadActiveSessionState()]).then(([seen, session]) => {
      if (mounted) {
        setInitialRoute(seen ? 'MainTabs' : 'Welcome');
        setStartupSession(seen ? session : null);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  const handleNavigationReady = useCallback(() => {
    if (startupSession && navigationRef.isReady()) {
      navigationRef.navigate('PuzzlePlay', {
        puzzleTypeId: startupSession.gameId,
        difficulty: startupSession.puzzle.difficulty,
        resume: true,
      });
    }
  }, [startupSession]);

  if (!initialRoute) {
    return (
      <View style={[styles.loading, { backgroundColor: theme.background }]}>
        <ActivityIndicator size="large" color={theme.primary} />
        <Text style={[styles.loadingText, { color: theme.textSecondary }]}>{strings.puzzlePlay.loading}</Text>
      </View>
    );
  }

  return (
    <NavigationContainer ref={navigationRef} theme={navigationTheme} onReady={handleNavigationReady}>
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: theme.background },
          gestureEnabled: true,
        }}
      >
        <Stack.Screen name="Welcome"         component={WelcomeScreen} />
        <Stack.Screen name="MainTabs"        component={MainTabs} />
        <Stack.Screen name="SupportInfo"     component={SupportInfoScreen} />
        <Stack.Screen name="Game"            component={GameScreen} />
        <Stack.Screen name="PuzzlePlay"      component={PuzzlePlayScreen} />
        <Stack.Screen name="Completion"      component={CompletionScreen} />
        <Stack.Screen name="Loss"            component={LossScreen} />
        <Stack.Screen name="Analysis"        component={PuzzleAnalysisScreen} />
        <Stack.Screen name="HowToPlay"       component={HowToPlayScreen} />
        <Stack.Screen name="TechniqueLesson" component={TechniqueLessonScreen} />
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
