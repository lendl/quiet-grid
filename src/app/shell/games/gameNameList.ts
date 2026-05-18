import { getAppStrings } from '../../i18n';
import { puzzleRegistry } from './gameRegistry';

export function getLocalizedPuzzleNameList(): string {
  const titles = puzzleRegistry.map((definition) => definition.title);

  if (titles.length === 0) {
    return '';
  }

  if (titles.length === 1) {
    return titles[0];
  }

  const connector = getAppStrings().common.and;

  if (titles.length === 2) {
    return `${titles[0]} ${connector} ${titles[1]}`;
  }

  const head = titles.slice(0, -1).join(', ');
  const tail = titles[titles.length - 1];
  return `${head}, ${connector} ${tail}`;
}
