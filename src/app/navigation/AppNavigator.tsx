import React, { useEffect, useMemo, useState } from 'react';
import { View, ActivityIndicator, Text, StyleSheet, Easing } from 'react-native';
import type { Theme as NavigationTheme } from '@react-navigation/native';
import { DefaultTheme, NavigationContainer } from '@react-navigation/native';
import {
  createStackNavigator,
  type StackCardStyleInterpolator,
} from '@react-navigation/stack';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from './types';
import { hasSeenWelcome } from '../utils/settingsStorage';
import MainTabs             from './MainTabs';
import WelcomeScreen         from '../screens/WelcomeScreen';
import GameScreen            from '../screens/GameScreen';
import CompletionScreen      from '../screens/CompletionScreen';
import LossScreen            from '../screens/LossScreen';
import PuzzleAnalysisScreen  from '../screens/PuzzleAnalysisScreen';
import PuzzlePlayScreen      from '../screens/PuzzlePlayScreen';
import HowToPlayScreen       from '../screens/HowToPlayScreen';
import SupportInfoScreen     from '../screens/SupportInfoScreen';
import TutorialHostScreen    from '../screens/TutorialHostScreen';
import TechniqueLessonScreen from '../screens/TechniqueLessonScreen';

const Stack = createStackNavigator<RootStackParamList>();

// MD3 shared-axis horizontal transition: small translate + fade, no full-width iOS slide.
const md3CardStyleInterpolator: StackCardStyleInterpolator = ({ current, next }) => ({
  cardStyle: {
    opacity: next
      ? next.progress.interpolate({ inputRange: [0, 1], outputRange: [1, 0.94] })
      : current.progress,
    transform: [{
      translateX: next
        ? next.progress.interpolate({ inputRange: [0, 1], outputRange: [0, -10] })
        : current.progress.interpolate({ inputRange: [0, 1], outputRange: [28, 0] }),
    }],
  },
  overlayStyle: {
    opacity: current.progress.interpolate({ inputRange: [0, 1], outputRange: [0, 0.1] }),
  },
});

export default function AppNavigator() {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const [initialRoute, setInitialRoute] = useState<'MainTabs' | 'Welcome' | null>(null);
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
          gestureDirection: 'horizontal',
          transitionSpec: {
            open: { animation: 'timing', config: { duration: 280, easing: Easing.bezier(0.05, 0.7, 0.1, 1.0) } },
            close: { animation: 'timing', config: { duration: 220, easing: Easing.bezier(0.3, 0.0, 0.8, 0.15) } },
          },
          cardStyleInterpolator: md3CardStyleInterpolator,
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
        <Stack.Screen
          name="Tutorial"
          component={TutorialHostScreen}
          initialParams={{ gameId: 'takuzu', entry: 'startup' }}
        />
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
