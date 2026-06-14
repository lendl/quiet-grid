import React, { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import Ionicons from '@react-native-vector-icons/ionicons';
import { Divider, List } from 'react-native-paper';
import { useNavigation, type NavigationProp } from '@react-navigation/native';
import appConfig from '../../../app.json';
import AppDialog from '../components/AppDialog';
import GlobalPageShell from '../components/GlobalPageShell';
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
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

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
  iconColor,
}: {
  title: string;
  rows: SupportRow[];
  styles: ReturnType<typeof makeStyles>;
  iconColor: string;
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
              <View style={styles.rowIconRight}>
                <Ionicons
                  name={row.external ? 'open-outline' : 'chevron-forward'}
                  size={16}
                  color={iconColor}
                />
              </View>
            )}
          />
          {index < rows.length - 1 ? <Divider /> : null}
        </React.Fragment>
      ))}
    </View>
  );
}

export default function SupportScreen() {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
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
    <GlobalPageShell activeTab="Support">
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.subtitle}>
            {strings.support.subtitle}
          </Text>
        </View>

        <Section title={strings.support.supportSection} rows={supportRows} styles={s} iconColor={theme.textMuted} />
        <Section title={strings.support.trustSection} rows={trustRows} styles={s} iconColor={theme.textMuted} />
        <Section title={strings.support.aboutSection} rows={aboutRows} styles={s} iconColor={theme.textMuted} />

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
  rowIconRight: {
    justifyContent: 'center',
    paddingRight: 4,
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
