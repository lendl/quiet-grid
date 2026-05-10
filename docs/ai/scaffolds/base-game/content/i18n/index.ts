import en from './en';
import nl from './nl';

export const gameI18nTemplate = {
  en,
  nl,
} as const;

export function getGameStringsTemplate(language: 'en' | 'nl') {
  return gameI18nTemplate[language];
}
