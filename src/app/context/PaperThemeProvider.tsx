import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { PaperProvider } from 'react-native-paper';
import { useTheme } from './ThemeContext';
import { buildPaperTheme } from '../theme/paperTheme';

// Map Paper's internal Material Design Icons names to Ionicons equivalents.
const MDI_TO_IONICONS: Record<string, string> = {
  'check': 'checkmark',
  'close': 'close',
  'close-circle': 'close-circle',
  'menu-down': 'chevron-down',
  'menu-up': 'chevron-up',
  'chevron-left': 'chevron-back',
  'chevron-right': 'chevron-forward',
  'arrow-left': 'arrow-back',
  'arrow-left-alt': 'arrow-back',
  'arrow-right': 'arrow-forward',
  'arrow-right-alt': 'arrow-forward',
  'magnify': 'search',
  'eye': 'eye',
  'eye-off': 'eye-off',
  'radiobox-blank': 'radio-button-off',
  'radiobox-marked': 'radio-button-on',
  'checkbox-blank-outline': 'square-outline',
  'checkbox-marked': 'checkbox',
};

function paperIcon({ name, size, color }: { name: string; size: number; color?: string }) {
  const ionName = MDI_TO_IONICONS[name];
  if (!ionName) return null;
  return <Ionicons name={ionName as React.ComponentProps<typeof Ionicons>['name']} size={size} color={color ?? 'currentColor'} />;
}

export function PaperThemeProvider({ children }: { children: React.ReactNode }) {
  const { themeMode } = useTheme();
  const paperTheme = useMemo(() => buildPaperTheme(themeMode), [themeMode]);
  return (
    <PaperProvider theme={paperTheme} settings={{ icon: paperIcon }}>
      {children}
    </PaperProvider>
  );
}
