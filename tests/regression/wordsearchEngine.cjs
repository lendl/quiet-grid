const path = require('node:path');
const { assert, test } = require('./testHarness.cjs');

const ROOT = path.resolve(__dirname, '..', '..');

const {
  buildHiddenWordPool,
  pickHiddenWord,
  reserveHiddenWordCells,
} = require(path.join(ROOT, 'src', 'games', 'wordsearch', 'engine', 'hiddenWord.ts'));

const {
  hasCoverageViolation,
  hasDuplicateOccurrence,
} = require(path.join(ROOT, 'src', 'games', 'wordsearch', 'engine', 'generationChecks.ts'));

const {
  buildQualityMetrics,
  passesQualityThreshold,
  buildDifficultyRatedScore,
} = require(path.join(ROOT, 'src', 'games', 'wordsearch', 'engine', 'quality.ts'));

function registerTests() {
  test('buildHiddenWordPool normalizes, dedupes, and drops words under 3 letters', () => {
    const pool = buildHiddenWordPool(['Cat', 'cat', 'Ox', 'Dog!']);
    assert.deepEqual(pool, ['CAT', 'DOG']);
  });

  test('pickHiddenWord only returns words that fit inside the grid', () => {
    const picked = pickHiddenWord(['CAT', 'ELEPHANT'], 2, 2);
    assert.equal(picked, 'CAT');
  });

  test('pickHiddenWord returns null when no candidate fits', () => {
    const picked = pickHiddenWord(['ELEPHANT'], 2, 2);
    assert.equal(picked, null);
  });

  test('reserveHiddenWordCells reserves exactly word.length cells, in reading order, inside the grid', () => {
    const reserved = reserveHiddenWordCells('CATS', 5, 5);
    assert.equal(reserved.word, 'CATS');
    assert.equal(reserved.positions.length, 4);
    reserved.positions.forEach((cell) => {
      assert.ok(cell.row >= 0 && cell.row < 5);
      assert.ok(cell.col >= 0 && cell.col < 5);
    });
    for (let i = 1; i < reserved.positions.length; i += 1) {
      const prev = reserved.positions[i - 1];
      const curr = reserved.positions[i];
      const inOrder = curr.row > prev.row || (curr.row === prev.row && curr.col > prev.col);
      assert.ok(inOrder, `positions must be sorted in reading order: ${JSON.stringify(reserved.positions)}`);
    }
  });

  test('hasCoverageViolation flags a word fully covered by others', () => {
    const dominated = { word: 'CAT', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] };
    const covering = { word: 'CATS', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }, { row: 0, col: 3 }] };
    assert.equal(hasCoverageViolation([dominated, covering]), true);
  });

  test('hasCoverageViolation allows words that each have a unique cell', () => {
    const wordA = { word: 'CAT', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] };
    const wordB = { word: 'TAP', positions: [{ row: 0, col: 2 }, { row: 1, col: 2 }, { row: 2, col: 2 }] };
    assert.equal(hasCoverageViolation([wordA, wordB]), false);
  });

  test('hasDuplicateOccurrence detects a word appearing at an unintended second location', () => {
    const grid = [
      ['C', 'A', 'T', 'X'],
      ['X', 'X', 'X', 'X'],
      ['C', 'A', 'T', 'X'],
      ['X', 'X', 'X', 'X'],
    ];
    const words = [{ word: 'CAT', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] }];
    assert.equal(hasDuplicateOccurrence(grid, words), true);
  });

  test('hasDuplicateOccurrence passes when every word appears only at its intended position', () => {
    const grid = [
      ['C', 'A', 'T', 'X'],
      ['X', 'X', 'X', 'X'],
      ['D', 'O', 'G', 'X'],
      ['X', 'X', 'X', 'X'],
    ];
    const words = [
      { word: 'CAT', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] },
      { word: 'DOG', positions: [{ row: 2, col: 0 }, { row: 2, col: 1 }, { row: 2, col: 2 }] },
    ];
    assert.equal(hasDuplicateOccurrence(grid, words), false);
  });

  test('buildQualityMetrics reports zero overlap and zero entropy for a single non-overlapping word', () => {
    const placements = [
      { word: 'CAT', direction: 'right', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] },
    ];
    const metrics = buildQualityMetrics(placements);
    assert.equal(metrics.overlapRatio, 0);
    assert.equal(metrics.directionEntropy, 0);
  });

  test('buildQualityMetrics reports positive overlap when words share cells', () => {
    const placements = [
      { word: 'CAT', direction: 'right', positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }] },
      { word: 'TAP', direction: 'down', positions: [{ row: 0, col: 2 }, { row: 1, col: 2 }, { row: 2, col: 2 }] },
    ];
    const metrics = buildQualityMetrics(placements);
    assert.ok(metrics.overlapRatio > 0);
  });

  test('passesQualityThreshold rejects an all-zero score against the expert threshold', () => {
    const lowQuality = { overlapRatio: 0, directionEntropy: 0, score: 0 };
    assert.equal(passesQualityThreshold('expert', lowQuality), false);
  });

  test('buildDifficultyRatedScore ranks a higher difficulty above a lower one at equal relative quality', () => {
    const easyScore = buildDifficultyRatedScore('easy', 1);
    const expertScore = buildDifficultyRatedScore('expert', 1);
    assert.ok(expertScore > easyScore);
  });
}

module.exports = { registerTests };
