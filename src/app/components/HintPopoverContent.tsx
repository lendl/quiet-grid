import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { ReactNode } from 'react';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';

interface Props {
  title: string;
  body: string;
  children?: ReactNode;
}

export function HintPopoverContent({ title, body, children }: Props) {
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);

  return (
    <>
      <View style={s.header}>
        <View style={s.badge}>
          <Text style={s.badgeText}>i</Text>
        </View>
        <Text style={s.title}>{title}</Text>
      </View>
      <Text style={s.body}>{body}</Text>
      {children}
    </>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  badge: {
    width: 22,
    height: 22,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
  },
  badgeText: {
    color: theme.onPrimary,
    fontSize: 12,
    fontWeight: '800',
  },
  title: {
    flex: 1,
    color: theme.text,
    fontSize: 13,
    fontWeight: '800',
  },
  body: {
    color: theme.textSecondary,
    fontSize: 12,
    lineHeight: 17,
  },
});
