import React, { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import appConfig from '../../../app.json';
import AppDialog from '../components/AppDialog';
import AppScreen from '../components/AppScreen';
import GridHomeIcon from '../components/GridHomeIcon';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import {
  SUPPORT_EMAIL,
  openBugReport,
  openFeatureRequest,
  openRateApp,
  openRepo,
  openSupportEmail,
} from '../utils/supportLinks';
import { returnToHome } from '../navigation/returnToHome';
import { withAlpha } from '../utils/color';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Support'>;

type SupportRow = {
  key: string;
  label: string;
  detail?: string;
  external?: boolean;
  onPress: () => void;
};

const APP_VERSION = appConfig.expo?.version ?? 'Unknown';

function Section({
  title,
  rows,
  styles,
}: {
  title: string;
  rows: SupportRow[];
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
            <Text style={styles.rowChevron}>{row.external ? 'Open' : '>'}</Text>
          </TouchableOpacity>
          {index < rows.length - 1 ? <View style={styles.rowDivider} /> : null}
        </React.Fragment>
      ))}
    </View>
  );
}

export default function SupportScreen({ navigation }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [openErrorVisible, setOpenErrorVisible] = useState(false);

  const runExternalAction = useCallback(async (action: () => Promise<boolean>) => {
    const opened = await action();

    if (!opened) {
      setOpenErrorVisible(true);
    }
  }, []);

  const handleBugReport = useCallback(() => {
    void runExternalAction(openBugReport);
  }, [runExternalAction]);

  const handleFeatureRequest = useCallback(() => {
    void runExternalAction(openFeatureRequest);
  }, [runExternalAction]);

  const handleContact = useCallback(() => {
    void runExternalAction(() => openSupportEmail());
  }, [runExternalAction]);

  const handleOpenRepo = useCallback(() => {
    void runExternalAction(openRepo);
  }, [runExternalAction]);

  const handleRateApp = useCallback(() => {
    void runExternalAction(openRateApp);
  }, [runExternalAction]);

  const supportRows = useMemo<SupportRow[]>(() => [
    {
      key: 'bug',
      label: strings.support.reportBug,
      detail: strings.support.opensGithubIssues,
      external: true,
      onPress: handleBugReport,
    },
    {
      key: 'feature',
      label: strings.support.requestFeature,
      detail: strings.support.opensGithubIssues,
      external: true,
      onPress: handleFeatureRequest,
    },
    {
      key: 'contact',
      label: strings.support.contact,
      detail: SUPPORT_EMAIL,
      external: true,
      onPress: handleContact,
    },
  ], [handleBugReport, handleContact, handleFeatureRequest, strings]);

  const trustRows = useMemo<SupportRow[]>(() => [
    {
      key: 'privacy',
      label: strings.support.privacy,
      onPress: () => navigation.navigate('SupportInfo', { infoKey: 'privacy' }),
    },
    {
      key: 'source',
      label: strings.support.sourceCode,
      detail: strings.support.opensGithub,
      external: true,
      onPress: handleOpenRepo,
    },
    {
      key: 'licenses',
      label: strings.support.licenses,
      onPress: () => navigation.navigate('SupportInfo', { infoKey: 'licenses' }),
    },
  ], [handleOpenRepo, navigation, strings]);

  const aboutRows = useMemo<SupportRow[]>(() => [
    {
      key: 'about',
      label: strings.support.aboutQuietGrid,
      onPress: () => navigation.navigate('SupportInfo', { infoKey: 'about' }),
    },
    {
      key: 'rate',
      label: strings.support.rateQuietGrid,
      detail: strings.support.opensPlayStore,
      external: true,
      onPress: handleRateApp,
    },
  ], [handleRateApp, navigation, strings]);

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
          <Text style={s.title}>{strings.support.title}</Text>
          <Text style={s.subtitle}>
            {strings.support.subtitle}
          </Text>
        </View>

        <Section title={strings.support.supportSection} rows={supportRows} styles={s} />
        <Section title={strings.support.trustSection} rows={trustRows} styles={s} />
        <Section title={strings.support.aboutSection} rows={aboutRows} styles={s} />

        <View style={s.versionBlock}>
          <Text style={s.versionText}>{strings.support.version(APP_VERSION)}</Text>
        </View>
      </ScrollView>

      <AppDialog
        visible={openErrorVisible}
        title={strings.support.openErrorTitle}
        message={strings.support.openErrorMessage}
        buttons={[
          {
            text: 'OK',
            onPress: () => setOpenErrorVisible(false),
          },
        ]}
        onDismiss={() => setOpenErrorVisible(false)}
      />
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
  rowChevron: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textMuted,
  },
  rowDivider: {
    height: 1,
    backgroundColor: withAlpha(theme.border, 0.48),
  },
  versionBlock: {
    marginTop: 4,
    paddingTop: 8,
  },
  versionText: {
    fontSize: 13,
    color: theme.textMuted,
  },
});
