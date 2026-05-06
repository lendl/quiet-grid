import type { ResolvedLanguage } from './index';

export type LocalizedGameContent<T> = {
  en: T;
} & Partial<Record<ResolvedLanguage, T>>;

export function resolveGameContent<T>(
  language: ResolvedLanguage,
  content: LocalizedGameContent<T>,
): T {
  return content[language] ?? content.en;
}
