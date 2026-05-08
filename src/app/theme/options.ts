import type { ComponentProps } from 'react';
import type { Ionicons } from '@expo/vector-icons';
import type { Theme, ThemeMode } from './index';
import { darkTheme, getTheme, pencilTheme } from './index';

type ThemeOptionStrings = {
  settings: {
    dark: string;
    darkDetail: string;
    light: string;
    lightDetail: string;
    pencil: string;
    pencilDetail: string;
  };
};

export interface ThemeOption {
  key: ThemeMode;
  label: string;
  detail: string;
  iconName: ComponentProps<typeof Ionicons>['name'];
  iconColor: string;
  theme: Theme;
}

const LIGHT_ICON_COLOR = '#f2b705';

export function getThemeOptions(strings: ThemeOptionStrings): ThemeOption[] {
  return [
    {
      key: 'dark',
      label: strings.settings.dark,
      detail: strings.settings.darkDetail,
      iconName: 'moon-outline',
      iconColor: darkTheme.primaryLight,
      theme: getTheme('dark'),
    },
    {
      key: 'light',
      label: strings.settings.light,
      detail: strings.settings.lightDetail,
      iconName: 'sunny',
      iconColor: LIGHT_ICON_COLOR,
      theme: getTheme('light'),
    },
    {
      key: 'pencil',
      label: strings.settings.pencil,
      detail: strings.settings.pencilDetail,
      iconName: 'pencil',
      iconColor: pencilTheme.text,
      theme: getTheme('pencil'),
    },
  ];
}
