interface MinesweeperCellRef {
  row: number;
  col: number;
}

interface SafeRevealCopyParams {
  clueCell: MinesweeperCellRef;
  targetCount: number;
  mineCount: number;
}

interface NextMoveCopy {
  title: string;
  body: string;
}

function formatCell({ row, col }: MinesweeperCellRef): string {
  return `row ${row + 1}, column ${col + 1}`;
}

function pluralize(count: number, singular: string, plural: string): string {
  return count === 1 ? singular : plural;
}

export function buildSafeRevealNextMove({
  clueCell,
  targetCount,
  mineCount,
}: SafeRevealCopyParams): NextMoveCopy {
  const clueLabel = formatCell(clueCell);
  const tileLabel = pluralize(targetCount, 'tile', 'tiles');
  const mineLabel = pluralize(mineCount, 'mine', 'mines');

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Veilige volgende zet bij ${clueLabel}`,
      body: `Open de gemarkeerde ${tileLabel}. Waarom: rond ${clueLabel} verklaren de gearceerde vakken al alle ${mineCount} ${mineLabel}, dus de overblijvende verborgen ${tileLabel} zijn veilig.`,
    };
  }

  return {
    title: `Safe next move near ${clueLabel}`,
    body: `Reveal highlighted ${tileLabel}. Why: around ${clueLabel}, shaded tiles already account for all ${mineCount} ${mineLabel}, so remaining hidden ${tileLabel} are safe.`,
  };
}

export function buildGuessNextMove(): NextMoveCopy {
  if (getCurrentLanguage() === 'nl') {
    return {
      title: 'Nog geen zekere volgende zet',
      body: 'Geen aanwijzing wijst nu op een zekere veilige zet. Hier kan een gok nodig zijn, dus vertrouw op je beste lezing van het bord en vraag opnieuw na de volgende onthulling.',
    };
  }

  return {
    title: 'No certain next move yet',
    body: 'No clue points to a certain safe reveal right now. This spot may need a guess, so trust your best read of the board and ask again after the next reveal.',
  };
}
import { getCurrentLanguage } from '../../../app/i18n';
