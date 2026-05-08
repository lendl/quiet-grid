import React from 'react';
import {
  Modal, View, Text, TouchableOpacity, StyleSheet, Pressable,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

export interface DialogButton {
  text: string;
  onPress?: () => void;
  style?: 'default' | 'cancel' | 'destructive';
}

export interface DialogConfig {
  title: string;
  message?: string;
  buttons: DialogButton[];
}

export interface AppDialogProps extends DialogConfig {
  visible: boolean;
  onDismiss?: () => void;
}

export default function AppDialog({ visible, title, message, buttons, onDismiss }: AppDialogProps) {
  const { theme, isDark } = useTheme();
  const s = makeStyles(theme, isDark);

  return (
    <Modal transparent animationType="fade" visible={visible} onRequestClose={onDismiss}>
      <Pressable style={s.backdrop} onPress={onDismiss}>
        <Pressable style={s.card} onPress={() => {}}>
          <Text style={s.title}>{title}</Text>
          {message ? <Text style={s.message}>{message}</Text> : null}
          <View style={s.buttons}>
            {buttons.map((btn, i) => (
              <TouchableOpacity
                key={i}
                style={[s.btn, btnVariantStyle(btn.style ?? 'default', theme, isDark)]}
                onPress={btn.onPress}
                activeOpacity={0.75}
              >
                <Text style={[s.btnText, btnTextStyle(btn.style ?? 'default', theme)]}>
                  {btn.text}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function btnVariantStyle(style: DialogButton['style'], theme: Theme, isDark: boolean) {
  if (style === 'destructive') {
    return {
      backgroundColor: theme.surface,
      borderColor: withAlpha(theme.difficultyExpert, isDark ? 0.36 : 0.28),
    };
  }

  if (style === 'cancel') {
    return {
      backgroundColor: theme.surface,
      borderColor: withAlpha(theme.border, 0.95),
    };
  }

  return { backgroundColor: theme.primary, borderColor: theme.primary };
}

function btnTextStyle(style: DialogButton['style'], theme: Theme) {
  if (style === 'destructive') return { color: theme.difficultyExpert };
  if (style === 'cancel') return { color: theme.textSecondary };
  return { color: theme.onPrimary };
}

const makeStyles = (theme: Theme, isDark: boolean) => StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: withAlpha(theme.background, isDark ? 0.82 : 0.56),
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  card: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: theme.surfaceElevated,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: withAlpha(theme.border, isDark ? 0.9 : 1),
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 24,
    shadowColor: isDark ? '#000000' : theme.text,
    shadowOpacity: isDark ? 0.32 : 0.12,
    shadowRadius: 22,
    shadowOffset: { width: 0, height: 16 },
    elevation: 12,
  },
  title: {
    color: theme.text,
    textAlign: 'center',
    fontSize: 24,
    fontWeight: '800',
    lineHeight: 30,
  },
  message: {
    marginTop: 10,
    color: theme.textSecondary,
    textAlign: 'center',
    fontSize: 15,
    lineHeight: 22,
  },
  buttons: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 20,
  },
  btn: {
    flex: 1,
    minHeight: 52,
    paddingHorizontal: 14,
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  btnText: {
    fontSize: 15,
    fontWeight: '700',
  },
});
