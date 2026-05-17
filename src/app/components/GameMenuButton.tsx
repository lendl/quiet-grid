import React, { useMemo } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, TouchableOpacity } from 'react-native';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';

type Props = {
  open: boolean;
  onPress: () => void;
};

export default function GameMenuButton({ open, onPress }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(), []);

  return (
    <TouchableOpacity
      accessibilityRole="button"
      accessibilityLabel={open ? strings.common.goBack : strings.common.open}
      onPress={onPress}
      style={s.button}
      activeOpacity={0.82}
    >
      <Ionicons
        name="menu"
        size={20}
        color={open ? theme.primaryLight : theme.text}
      />
    </TouchableOpacity>
  );
}

const makeStyles = () => StyleSheet.create({
  button: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
