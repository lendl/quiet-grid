import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import { Ionicons } from '@expo/vector-icons';
import AppScreen from '../components/AppScreen';
import GridHomeIcon from '../components/GridHomeIcon';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { returnToHome } from '../navigation/returnToHome';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import {
  loadTutorialsEnabled,
  saveTutorialsEnabled,
} from '../utils/settingsStorage';

type Props = StackScreenProps<RootStackParamList, 'Config'>;

type ConfigRow = {
  key: string;
  label: string;
  detail?: string;
  value?: string;
  onPress: () => void;
};

type LanguageOption = {
  key: 'en' | 'nl';
  label: string;
  detail: string;
  icon: string;
};

type ThemeOption = {
  key: 'dark' | 'light';
  label: string;
  detail: string;
};

function Section({
  title,
  rows,
  styles,
}: {
  title: string;
  rows: ConfigRow[];
  styles: ReturnType<typeof makeStyles>;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {rows.map((row, index) => (
        <React.Fragment key={row.key}>
          <TouchableOpacity
            style={styles.row}
            onPress={row.onPress}
            accessibilityRole="button"
            accessibilityLabel={row.label}
            activeOpacity={0.82}
          >
            <View style={styles.rowTextWrap}>
              <Text style={styles.rowLabel}>{row.label}</Text>
              {row.detail ? <Text style={styles.rowDetail}>{row.detail}</Text> : null}
            </View>
            {row.value ? <Text style={styles.rowValue}>{row.value}</Text> : null}
          </TouchableOpacity>
          {index < rows.length - 1 ? <View style={styles.rowDivider} /> : null}
        </React.Fragment>
      ))}
    </View>
  );
}

export default function ConfigScreen({ navigation }: Props) {
  const { strings, resolvedLanguage, setLanguageSetting } = useLanguage();
  const { theme, themeMode, setThemeMode } = useTheme();
  const [tutorialsEnabled, setTutorialsEnabled] = React.useState(true);
  const [themeDropdownOpen, setThemeDropdownOpen] = React.useState(false);
  const [languageDropdownOpen, setLanguageDropdownOpen] = React.useState(false);
  const s = useMemo(() => makeStyles(theme), [theme]);

  React.useEffect(() => {
    let mounted = true;

    void loadTutorialsEnabled().then((enabled) => {
      if (mounted) {
        setTutorialsEnabled(enabled);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  const themeOptions = useMemo<ThemeOption[]>(() => [
    {
      key: 'dark',
      label: strings.config.dark,
      detail: strings.config.darkDetail,
    },
  ], [strings]);
  const selectedTheme = useMemo(
    () => {
      if (themeMode === 'light') {
        return {
          key: 'light',
          label: strings.config.light,
          detail: strings.config.lightDetail,
        } satisfies ThemeOption;
      }

      return themeOptions[0];
    },
    [strings, themeMode, themeOptions],
  );

  const languageOptions = useMemo<LanguageOption[]>(() => ([
    {
      key: 'en',
      label: strings.common.english,
      detail: strings.config.languageEnglishDetail,
      icon: '🇬🇧',
    },
    {
      key: 'nl',
      label: strings.common.dutch,
      detail: strings.config.languageDutchDetail,
      icon: '🇳🇱',
    },
  ]), [strings]);
  const selectedLanguage = useMemo(
    () => languageOptions.find((option) => option.key === resolvedLanguage) ?? languageOptions[0],
    [languageOptions, resolvedLanguage],
  );

  const tutorialRows = useMemo<ConfigRow[]>(() => [
    {
      key: 'tutorials',
      label: strings.config.tutorialsLabel,
      detail: strings.config.tutorialsDetail,
      value: tutorialsEnabled ? strings.common.on : strings.common.off,
      onPress: () => {
        const next = !tutorialsEnabled;
        setTutorialsEnabled(next);
        void saveTutorialsEnabled(next);
      },
    },
  ], [strings, tutorialsEnabled]);

  return (
    <AppScreen contentStyle={s.container}>
      <ScrollView contentContainerStyle={s.scroll}>
        <TouchableOpacity
          style={s.homeButton}
          onPress={() => returnToHome(navigation)}
          accessibilityLabel={strings.common.goHome}
          activeOpacity={0.8}
        >
          <GridHomeIcon />
        </TouchableOpacity>

        <View style={s.header}>
          <Text style={s.title}>{strings.config.title}</Text>
          <Text style={s.subtitle}>
            {strings.config.subtitle}
          </Text>
        </View>

        <View style={s.section}>
          <Text style={s.sectionTitle}>{strings.config.appearance}</Text>
          <TouchableOpacity
            style={s.dropdownTrigger}
            onPress={() => {
              setThemeDropdownOpen((open) => {
                const next = !open;
                if (next) {
                  setLanguageDropdownOpen(false);
                }
                return next;
              });
            }}
            accessibilityRole="button"
            accessibilityLabel={strings.config.appearance}
            activeOpacity={0.82}
          >
            <View style={s.dropdownIconWrap}>
              <Ionicons name="color-palette-outline" size={18} color={theme.primaryLight} />
            </View>
            <View style={s.rowTextWrap}>
              <Text style={s.rowLabel}>{strings.config.appearance}</Text>
            </View>
            <View style={s.dropdownValueWrap}>
              <Text style={s.rowValue}>{selectedTheme.label}</Text>
              <Ionicons
                name={themeDropdownOpen ? 'chevron-up' : 'chevron-down'}
                size={18}
                color={theme.textSecondary}
              />
            </View>
          </TouchableOpacity>
          {themeDropdownOpen ? (
            <View style={s.dropdownMenu}>
              {themeOptions.map((option, index) => (
                <React.Fragment key={option.key}>
                  <TouchableOpacity
                    style={s.dropdownOptionNoIcon}
                    onPress={() => {
                      setThemeMode(option.key);
                      setThemeDropdownOpen(false);
                    }}
                    accessibilityRole="button"
                    accessibilityLabel={option.label}
                    activeOpacity={0.82}
                  >
                    <View style={s.rowTextWrap}>
                      <Text style={s.rowLabel}>{option.label}</Text>
                      <Text style={s.rowDetail}>{option.detail}</Text>
                    </View>
                    {themeMode === option.key ? (
                      <Ionicons name="checkmark" size={18} color={theme.primaryLight} />
                    ) : null}
                  </TouchableOpacity>
                  {index < themeOptions.length - 1 ? <View style={s.rowDivider} /> : null}
                </React.Fragment>
              ))}
            </View>
          ) : null}
        </View>
        <View style={s.section}>
          <Text style={s.sectionTitle}>{strings.config.language}</Text>
          <TouchableOpacity
            style={s.dropdownTrigger}
            onPress={() => {
              setLanguageDropdownOpen((open) => {
                const next = !open;
                if (next) {
                  setThemeDropdownOpen(false);
                }
                return next;
              });
            }}
            accessibilityRole="button"
            accessibilityLabel={strings.config.language}
            activeOpacity={0.82}
          >
            <View style={s.dropdownIconWrap}>
              <Ionicons name="globe-outline" size={18} color={theme.primaryLight} />
            </View>
            <View style={s.rowTextWrap}>
              <Text style={s.rowLabel}>{strings.config.language}</Text>
              <Text style={s.rowDetail}>{strings.config.languageDropdownDetail}</Text>
            </View>
            <View style={s.dropdownValueWrap}>
              <Text style={s.rowValue}>{selectedLanguage.label}</Text>
              <Ionicons
                name={languageDropdownOpen ? 'chevron-up' : 'chevron-down'}
                size={18}
                color={theme.textSecondary}
              />
            </View>
          </TouchableOpacity>
          {languageDropdownOpen ? (
            <View style={s.dropdownMenu}>
              {languageOptions.map((option, index) => (
                <React.Fragment key={option.key}>
                  <TouchableOpacity
                    style={s.dropdownOption}
                    onPress={() => {
                      setLanguageSetting(option.key);
                      setLanguageDropdownOpen(false);
                    }}
                    accessibilityRole="button"
                    accessibilityLabel={option.label}
                    activeOpacity={0.82}
                  >
                    <Text style={s.dropdownFlag}>{option.icon}</Text>
                    <View style={s.rowTextWrap}>
                      <Text style={s.rowLabel}>{option.label}</Text>
                      <Text style={s.rowDetail}>{option.detail}</Text>
                    </View>
                    {resolvedLanguage === option.key ? (
                      <Ionicons name="checkmark" size={18} color={theme.primaryLight} />
                    ) : null}
                  </TouchableOpacity>
                  {index < languageOptions.length - 1 ? <View style={s.rowDivider} /> : null}
                </React.Fragment>
              ))}
            </View>
          ) : null}
          <Text style={s.disclaimerText}>{strings.config.languageAiDisclaimer}</Text>
        </View>
        <Section title={strings.config.tutorials} rows={tutorialRows} styles={s} />
      </ScrollView>
    </AppScreen>
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
  homeButton: {
    alignSelf: 'flex-start',
    minWidth: 44,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 2,
  },
  header: {
    gap: 8,
  },
  title: {
    fontSize: 30,
    fontWeight: '900',
    color: theme.text,
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
  row: {
    minHeight: 62,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  rowTextWrap: {
    flex: 1,
    gap: 4,
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
  rowDivider: {
    height: 1,
    backgroundColor: withAlpha(theme.border, 0.48),
  },
  dropdownTrigger: {
    minHeight: 62,
    paddingVertical: 12,
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
  dropdownMenu: {
    marginTop: 4,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.64),
    backgroundColor: withAlpha(theme.surfaceElevated, 0.98),
    overflow: 'hidden',
  },
  dropdownOption: {
    minHeight: 62,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  dropdownOptionNoIcon: {
    minHeight: 62,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  dropdownFlag: {
    width: 28,
    fontSize: 18,
    textAlign: 'center',
  },
  disclaimerText: {
    marginTop: 10,
    fontSize: 12,
    lineHeight: 18,
    color: theme.textMuted,
  },
});
