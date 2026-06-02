import { Linking } from 'react-native';
import { getCurrentLanguage } from '../i18n';

export const SUPPORT_EMAIL = 'quiet-grid@outlook.com';
export const REPO_URL = 'https://github.com/lendl/quiet-grid';
export const ISSUES_URL = `${REPO_URL}/issues`;
export const PLAY_STORE_APP_URL = 'market://details?id=com.quietgrid.app';
export const PLAY_STORE_WEB_URL = 'https://play.google.com/store/apps/details?id=com.quietgrid.app';

function buildIssueUrl(title: string, body: string): string {
  return `${ISSUES_URL}/new?title=${encodeURIComponent(title)}&body=${encodeURIComponent(body)}`;
}

function buildBugReportUrl(): string {
  if (getCurrentLanguage() === 'nl') {
    return buildIssueUrl(
      '[Bug] ',
      [
        '## Wat gebeurde er',
        '',
        'Beschrijf het probleem dat je tegenkwam.',
        '',
        '## Stappen om te reproduceren',
        '',
        '1. ',
        '2. ',
        '3. ',
        '',
        '## Verwacht gedrag',
        '',
        'Beschrijf wat je in plaats daarvan verwachtte.',
      ].join('\n'),
    );
  }

  return buildIssueUrl(
    '[Bug] ',
    [
      '## What happened',
      '',
      'Describe problem you ran into.',
      '',
      '## Steps to reproduce',
      '',
      '1. ',
      '2. ',
      '3. ',
      '',
      '## Expected behavior',
      '',
      'Describe what you expected instead.',
    ].join('\n'),
  );
}

function buildFeatureRequestUrl(): string {
  if (getCurrentLanguage() === 'nl') {
    return buildIssueUrl(
      '[Feature] ',
      [
        '## Wat zou helpen',
        '',
        'Beschrijf de functie of verbetering die je graag zou zien.',
        '',
        '## Waarom dit zou helpen',
        '',
        'Leg uit welk probleem het oplost of wat prettiger zou voelen.',
      ].join('\n'),
    );
  }

  return buildIssueUrl(
    '[Feature] ',
    [
      '## What would help',
      '',
      'Describe feature or improvement you would like to see.',
      '',
      '## Why it would help',
      '',
      'Share problem it would solve or what would feel better.',
    ].join('\n'),
  );
}

export function buildMailto(subject?: string): string {
  const query = subject ? `?subject=${encodeURIComponent(subject)}` : '';
  return `mailto:${SUPPORT_EMAIL}${query}`;
}

export async function tryOpenUrl(url: string): Promise<boolean> {
  const supported = await Linking.canOpenURL(url);

  if (!supported) {
    return false;
  }

  return Linking.openURL(url).then(
    () => true,
    () => false,
  );
}

export function openSupportEmail(subject?: string): Promise<boolean> {
  return tryOpenUrl(buildMailto(subject));
}

export function openRepo(): Promise<boolean> {
  return tryOpenUrl(REPO_URL);
}

export function openBugReport(): Promise<boolean> {
  return tryOpenUrl(buildBugReportUrl());
}

export function openFeatureRequest(): Promise<boolean> {
  return tryOpenUrl(buildFeatureRequestUrl());
}

export async function openRateApp(): Promise<boolean> {
  if (await tryOpenUrl(PLAY_STORE_APP_URL)) {
    return true;
  }

  return tryOpenUrl(PLAY_STORE_WEB_URL);
}
