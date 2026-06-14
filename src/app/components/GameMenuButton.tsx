import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StyleSheet } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
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
    <TouchableRipple
      accessibilityRole="button"
      accessibilityLabel={open ? strings.common.goBack : strings.common.open}
      onPress={onPress}
      style={s.button}
      borderless
    >
      <Ionicons
        name="menu"
        size={20}
        color={open ? theme.primaryLight : theme.text}
      />
    </TouchableRipple>
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
