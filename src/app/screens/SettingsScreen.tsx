import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import Ionicons from '@react-native-vector-icons/ionicons';
import { Divider, List, Menu, TouchableRipple } from 'react-native-paper';
import GlobalPageShell from '../components/GlobalPageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';
import { getThemeOptions } from '../theme/options';
import {
  loadBetaGamesEnabled,
  loadShowTimerInPlay,
  loadTutorialsEnabled,
  saveBetaGamesEnabled,
  saveShowTimerInPlay,
  saveTutorialsEnabled,
} from '../utils/settingsStorage';

type SettingsRow = {
  key: string;
  label: string;
  detail?: string;
  value?: string;
  onPress: () => void;
};

type LanguageOption = {
  key: 'en' | 'nl' | 'de' | 'fr' | 'es';
  label: string;
  detail: string;
  icon: string;
};

function Section({
  title,
  rows,
  styles,
}: {
  title: string;
  rows: SettingsRow[];
  styles: ReturnType<typeof makeStyles>;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {rows.map((row, index) => (
        <React.Fragment key={row.key}>
          <List.Item
            title={row.label}
            description={row.detail ?? null}
            titleStyle={styles.rowLabel}
            descriptionStyle={styles.rowDetail}
            onPress={row.onPress}
            right={() => (
              row.value ? <View style={styles.rowValueWrap}><Text style={styles.rowValue}>{row.value}</Text></View> : null
            )}
          />
          {index < rows.length - 1 ? <Divider /> : null}
        </React.Fragment>
      ))}
    </View>
  );
}

export default function SettingsScreen() {
  const { strings, resolvedLanguage, setLanguageSetting } = useLanguage();
  const { theme, themeMode, setThemeMode } = useTheme();
  const [showTimerInPlay, setShowTimerInPlay] = React.useState(true);
  const [tutorialsEnabled, setTutorialsEnabled] = React.useState(true);
  const [betaGamesEnabled, setBetaGamesEnabled] = React.useState(false);
  const [themeDropdownOpen, setThemeDropdownOpen] = React.useState(false);
  const [languageDropdownOpen, setLanguageDropdownOpen] = React.useState(false);
  const s = useMemo(() => makeStyles(theme), [theme]);

  React.useEffect(() => {
    let mounted = true;

    void Promise.all([
      loadTutorialsEnabled(),
      loadShowTimerInPlay(),
      loadBetaGamesEnabled(),
    ]).then(([tutorials, showTimer, betaGames]) => {
      if (mounted) {
        setTutorialsEnabled(tutorials);
        setShowTimerInPlay(showTimer);
        setBetaGamesEnabled(betaGames);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  const themeOptions = useMemo(() => getThemeOptions(strings), [strings]);
  const selectedTheme = useMemo(
    () => themeOptions.find((option) => option.key === themeMode) ?? themeOptions[0],
    [themeMode, themeOptions],
  );

  const languageOptions = useMemo<LanguageOption[]>(() => ([
    {
      key: 'en',
      label: strings.common.english,
      detail: strings.settings.languageEnglishDetail,
      icon: '🇬🇧',
    },
    {
      key: 'nl',
      label: strings.common.dutch,
      detail: strings.settings.languageDutchDetail,
      icon: '🇳🇱',
    },
    {
      key: 'de',
      label: strings.common.german,
      detail: strings.settings.languageGermanDetail,
      icon: '🇩🇪',
    },
    {
      key: 'fr',
      label: strings.common.french,
      detail: strings.settings.languageFrenchDetail,
      icon: '🇫🇷',
    },
    {
      key: 'es',
      label: strings.common.spanish,
      detail: strings.settings.languageSpanishDetail,
      icon: '🇪🇸',
    },
  ]), [strings]);
  const selectedLanguage = useMemo(
    () => languageOptions.find((option) => option.key === resolvedLanguage) ?? languageOptions[0],
    [languageOptions, resolvedLanguage],
  );

  const timerRows = useMemo<SettingsRow[]>(() => [
    {
      key: 'showTimerInPlay',
      label: strings.settings.showTimerInPlayLabel,
      detail: strings.settings.showTimerInPlayDetail,
      value: showTimerInPlay ? strings.common.on : strings.common.off,
      onPress: () => {
        const next = !showTimerInPlay;
        setShowTimerInPlay(next);
        void saveShowTimerInPlay(next);
      },
    },
  ], [showTimerInPlay, strings]);

  const tutorialRows = useMemo<SettingsRow[]>(() => [
    {
      key: 'tutorials',
      label: strings.settings.tutorialsLabel,
      detail: strings.settings.tutorialsDetail,
      value: tutorialsEnabled ? strings.common.on : strings.common.off,
      onPress: () => {
        const next = !tutorialsEnabled;
        setTutorialsEnabled(next);
        void saveTutorialsEnabled(next);
      },
    },
    {
      key: 'betaGames',
      label: strings.settings.betaGamesLabel,
      detail: strings.settings.betaGamesDetail,
      value: betaGamesEnabled ? strings.common.on : strings.common.off,
      onPress: () => {
        const next = !betaGamesEnabled;
        setBetaGamesEnabled(next);
        void saveBetaGamesEnabled(next);
      },
    },
  ], [tutorialsEnabled, betaGamesEnabled, strings]);
  return (
    <GlobalPageShell activeTab="Settings">
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.subtitle}>
            {strings.settings.subtitle}
          </Text>
        </View>

        <View style={s.section}>
          <Text style={s.sectionTitle}>{strings.settings.appearance}</Text>
          <Menu
            visible={themeDropdownOpen}
            onDismiss={() => setThemeDropdownOpen(false)}
            anchor={
              <TouchableRipple
                style={s.dropdownTrigger}
                onPress={() => {
                  setLanguageDropdownOpen(false);
                  setThemeDropdownOpen((open) => !open);
                }}
                accessibilityRole="button"
                accessibilityLabel={strings.settings.theme}
              >
                <View style={s.dropdownTriggerContent}>
                  <View style={s.dropdownIconWrap}>
                    <Ionicons name={selectedTheme.iconName} size={18} color={selectedTheme.iconColor} />
                  </View>
                  <View style={s.rowTextWrap}>
                    <Text style={s.rowLabel}>{strings.settings.theme}</Text>
                  </View>
                  <View style={s.dropdownValueWrap}>
                    <Text style={s.rowValue}>{selectedTheme.label}</Text>
                    <Ionicons
                      name={themeDropdownOpen ? 'chevron-up' : 'chevron-down'}
                      size={18}
                      color={theme.textSecondary}
                    />
                  </View>
                </View>
              </TouchableRipple>
            }
          >
            {themeOptions.map((option) => (
              <Menu.Item
                key={option.key}
                title={option.label}
                onPress={() => {
                  setThemeMode(option.key);
                  setThemeDropdownOpen(false);
                }}
                leadingIcon={({ size }) => (
                  <Ionicons name={option.iconName} size={size} color={option.iconColor} />
                )}
                trailingIcon={themeMode === option.key ? ({ size }) => (
                  <Ionicons name="checkmark" size={size} color={theme.primaryLight} />
                ) : undefined}
              />
            ))}
          </Menu>
          <Divider />
          <Menu
            visible={languageDropdownOpen}
            onDismiss={() => setLanguageDropdownOpen(false)}
            anchor={
              <TouchableRipple
                style={s.dropdownTrigger}
                onPress={() => {
                  setThemeDropdownOpen(false);
                  setLanguageDropdownOpen((open) => !open);
                }}
                accessibilityRole="button"
                accessibilityLabel={strings.settings.language}
              >
                <View style={s.dropdownTriggerContent}>
                  <View style={s.dropdownIconWrap}>
                    <Ionicons name="globe-outline" size={18} color={theme.primaryLight} />
                  </View>
                  <View style={s.rowTextWrap}>
                    <Text style={s.rowLabel}>{strings.settings.language}</Text>
                    <Text style={s.rowDetail}>{strings.settings.languageDropdownDetail}</Text>
                  </View>
                  <View style={s.dropdownValueWrap}>
                    <Text style={s.rowValue}>{selectedLanguage.label}</Text>
                    <Ionicons
                      name={languageDropdownOpen ? 'chevron-up' : 'chevron-down'}
                      size={18}
                      color={theme.textSecondary}
                    />
                  </View>
                </View>
              </TouchableRipple>
            }
          >
            {languageOptions.map((option) => (
              <Menu.Item
                key={option.key}
                title={option.label}
                onPress={() => {
                  setLanguageSetting(option.key);
                  setLanguageDropdownOpen(false);
                }}
                leadingIcon={({ size }) => (
                  <Text style={{ fontSize: size, lineHeight: size + 2 }}>{option.icon}</Text>
                )}
                trailingIcon={resolvedLanguage === option.key ? ({ size }) => (
                  <Ionicons name="checkmark" size={size} color={theme.primaryLight} />
                ) : undefined}
              />
            ))}
          </Menu>
          <Text style={s.disclaimerText}>{strings.settings.languageAiDisclaimer}</Text>
          <Divider />
          {timerRows.map((row) => (
            <List.Item
              key={row.key}
              title={row.label}
              description={row.detail ?? null}
              titleStyle={s.rowLabel}
              descriptionStyle={s.rowDetail}
              onPress={row.onPress}
              right={() => (
                row.value ? <View style={s.rowValueWrap}><Text style={s.rowValue}>{row.value}</Text></View> : null
              )}
            />
          ))}
        </View>
        <Section title={strings.settings.tutorials} rows={tutorialRows} styles={s} />
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
    gap: 22,
  },
  header: {
    gap: 8,
  },
  subtitle: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
  },
  section: {
    gap: 0,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
    marginBottom: 4,
  },
  rowLabel: {
    fontSize: 16,
    fontWeight: '700',
    color: theme.text,
  },
  rowDetail: {
    fontSize: 13,
    lineHeight: 19,
    color: theme.textSecondary,
  },
  rowValue: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.primaryLight,
  },
  rowValueWrap: {
    justifyContent: 'center',
    paddingRight: 4,
  },
  rowTextWrap: {
    flex: 1,
    gap: 4,
  },
  dropdownTrigger: {
    minHeight: 62,
    paddingVertical: 12,
  },
  dropdownTriggerContent: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  dropdownIconWrap: {
    width: 28,
    alignItems: 'center',
  },
  dropdownValueWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  disclaimerText: {
    marginTop: 10,
    fontSize: 12,
    lineHeight: 18,
    color: theme.textMuted,
  },
});
