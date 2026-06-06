import React, { useMemo } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';
import appIcon from '../../../assets/icon.png';

type Props = {
  compact?: boolean;
  gameName?: string;
};

export default function AppBrand({ compact = false, gameName }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme, compact), [compact, theme]);

  return (
    <View style={s.container} accessibilityRole="image" accessibilityLabel={strings.home.brandMark}>
      <Image source={appIcon} style={s.logo} />
      <View style={s.textWrap}>
        <Text style={s.title} numberOfLines={1}>
          {gameName ? `Quiet Grid - ${gameName}` : 'Quiet Grid'}
        </Text>
        {compact ? null : <Text style={s.subtitle}>{strings.home.subtitle}</Text>}
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme, compact: boolean) => StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flexShrink: 1,
  },
  logo: {
    width: compact ? 32 : 40,
    height: compact ? 32 : 40,
    borderRadius: compact ? 10 : 12,
  },
  textWrap: {
    flexShrink: 1,
    gap: compact ? 0 : 2,
  },
  title: {
    fontSize: compact ? 17 : 19,
    fontWeight: '900',
    color: theme.text,
  },
  subtitle: {
    fontSize: 12,
    color: theme.textSecondary,
  },
});
