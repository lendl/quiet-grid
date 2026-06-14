import React from 'react';
import { StyleSheet } from 'react-native';
import { Button, Dialog, Portal, Text } from 'react-native-paper';
import { useTheme } from '../context/ThemeContext';

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
  const { theme } = useTheme();

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onDismiss} style={styles.dialog}>
        <Dialog.Title style={styles.title}>{title}</Dialog.Title>
        {message ? (
          <Dialog.Content>
            <Text variant="bodyMedium" style={{ color: theme.textSecondary }}>{message}</Text>
          </Dialog.Content>
        ) : null}
        <Dialog.Actions>
          {buttons.map((btn, i) => (
            <Button
              key={i}
              onPress={btn.onPress}
              textColor={btn.style === 'destructive' ? theme.difficultyExpert : btn.style === 'cancel' ? theme.textSecondary : theme.primary}
            >
              {btn.text}
            </Button>
          ))}
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}

const styles = StyleSheet.create({
  dialog: {
    borderRadius: 24,
  },
  title: {
    textAlign: 'center',
    fontWeight: '800',
  },
});
