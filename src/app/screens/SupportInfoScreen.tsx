import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import GlobalPageShell from '../components/GlobalPageShell';
import { getLocalizedSupportInfoContent } from '../content/supportInfo';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'SupportInfo'>;

export default function SupportInfoScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const info = getLocalizedSupportInfoContent()[route.params.infoKey];

  return (
    <GlobalPageShell activeTab="Support" contentStyle={s.container}>
      <ScrollView contentContainerStyle={s.scroll}>
        <TouchableOpacity
          style={s.backButton}
          onPress={() => navigation.goBack()}
          accessibilityRole="button"
            accessibilityLabel={strings.common.goBack}
            activeOpacity={0.8}
          >
            <Text style={s.backButtonText}>{strings.supportInfoBack}</Text>
          </TouchableOpacity>

        <Text style={s.title}>{info.title}</Text>
        <Text style={s.intro}>{info.intro}</Text>

        {info.sections.map((section) => (
          <View key={section.heading} style={s.section}>
            <Text style={s.sectionHeading}>{section.heading}</Text>
            {section.body.map((paragraph) => (
              <Text key={paragraph} style={s.bodyText}>
                {paragraph}
              </Text>
            ))}
            <View style={s.sectionDivider} />
          </View>
        ))}
      </ScrollView>
    </GlobalPageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  scroll: {
    padding: 20,
    gap: 20,
  },
  backButton: {
    alignSelf: 'flex-start',
    minHeight: 32,
    justifyContent: 'center',
  },
  backButtonText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  title: {
    marginTop: 4,
    fontSize: 28,
    fontWeight: '900',
    color: theme.text,
  },
  intro: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
  },
  section: {
    gap: 10,
  },
  sectionHeading: {
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  bodyText: {
    fontSize: 15,
    lineHeight: 23,
    color: theme.text,
  },
  sectionDivider: {
    height: 1,
    marginTop: 6,
    backgroundColor: theme.border,
  },
});
