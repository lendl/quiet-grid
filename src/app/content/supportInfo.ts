export type SupportInfoKey = 'privacy' | 'about' | 'licenses';

interface SupportInfoSection {
  heading: string;
  body: string[];
}

export interface SupportInfoContent {
  title: string;
  intro: string;
  sections: SupportInfoSection[];
}

import { getSupportInfoContent } from '../i18n';
import { getLocalizedGameNameList } from '../shell/games/gameNameList';

function replaceGameNames(value: string): string {
  return value.replace(/\{games\}/g, getLocalizedGameNameList());
}

export function getLocalizedSupportInfoContent(): Record<SupportInfoKey, SupportInfoContent> {
  const content = getSupportInfoContent();

  return Object.fromEntries(
    Object.entries(content).map(([key, entry]) => [
      key,
      {
        ...entry,
        intro: replaceGameNames(entry.intro),
        sections: entry.sections.map((section) => ({
          ...section,
          heading: replaceGameNames(section.heading),
          body: section.body.map(replaceGameNames),
        })),
      },
    ]),
  ) as Record<SupportInfoKey, SupportInfoContent>;
}
