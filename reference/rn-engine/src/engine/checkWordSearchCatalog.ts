import entries from '../games/wordsearch/puzzles/all';
import {
  validateWordSearchCatalog,
  validateWordSearchSeedCorpus,
} from '../games/wordsearch/engine/validator';

function main(): void {
  const seedErrors = validateWordSearchSeedCorpus();
  const catalogErrors = validateWordSearchCatalog(entries);
  const errors = [
    ...seedErrors.map((error) => ({
      scope: 'seed',
      id: error.language,
      message: error.message,
    })),
    ...catalogErrors.map((error) => ({
      scope: 'catalog',
      id: error.id,
      message: error.message,
    })),
  ];

  if (errors.length === 0) {
    console.log('Word Search seed corpus and catalog checks passed.');
    return;
  }

  console.error(`Word Search checks failed with ${errors.length} issue(s):`);
  errors.forEach((error) => {
    console.error(`- [${error.scope}:${error.id}] ${error.message}`);
  });

  process.exit(1);
}

main();
