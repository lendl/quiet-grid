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

const { buildFullCoverageGrid } = require(path.join(ROOT, 'src', 'games', 'wordsearch', 'engine', 'placement.ts'));

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

  test('buildFullCoverageGrid tiles every cell of a 3x3 grid with no gaps left', () => {
    const words = ['CAT', 'DOG', 'OWL', 'ANT', 'BEE'];
    const config = { allowedDirections: ['right', 'down'], overlapFrequency: 0.2 };
    let sawSuccess = false;
    for (let attempt = 0; attempt < 20 && !sawSuccess; attempt += 1) {
      const result = buildFullCoverageGrid(3, 3, words, new Set(), config);
      if (!result) continue;
      sawSuccess = true;
      result.grid.forEach((row) => row.forEach((cell) => {
        assert.notEqual(cell, '', 'every cell must be covered by a word');
        assert.notEqual(cell, '#', 'no reserved sentinel should remain uncovered');
      }));
      const usedWords = new Set();
      result.placements.forEach((placement) => {
        assert.ok(!usedWords.has(placement.word), `word ${placement.word} placed more than once`);
        usedWords.add(placement.word);
        placement.positions.forEach((cell) => {
          assert.ok(cell.row >= 0 && cell.row < 3 && cell.col >= 0 && cell.col < 3);
        });
      });
    }
    assert.ok(sawSuccess, 'expected at least one successful full-coverage tiling across 20 attempts');
  });

  test('buildFullCoverageGrid leaves reserved cells untouched by word placements', () => {
    // 4x4 (not 3x3): with word length 3 and only right/down directions, a 3x3
    // grid forces every placement to span a full row or column, so reserving
    // a corner cell makes the remaining 8 cells combinatorially uncoverable
    // (verified by brute force) regardless of which words are used. At 4x4,
    // row 0 and column 0 each have a second valid start offset that avoids
    // the reserved corner, giving a full tiling with zero overlap needed.
    const words = ['CAT', 'DOG', 'OWL', 'ANT', 'BEE'];
    const config = { allowedDirections: ['right', 'down'], overlapFrequency: 0.2 };
    const reserved = new Set([0 * 1000 + 0]);
    for (let attempt = 0; attempt < 20; attempt += 1) {
      const result = buildFullCoverageGrid(4, 4, words, reserved, config);
      if (!result) continue;
      result.placements.forEach((placement) => {
        placement.positions.forEach((cell) => {
          assert.ok(!(cell.row === 0 && cell.col === 0), 'word placement must not use a reserved cell');
        });
      });
      return;
    }
    assert.fail('expected at least one successful tiling across 20 attempts');
  });

  test('buildFullCoverageGrid returns null when no word can possibly fit the grid', () => {
    const config = { allowedDirections: ['right'], overlapFrequency: 0.1 };
    const result = buildFullCoverageGrid(2, 2, ['ELEPHANT'], new Set(), config);
    assert.equal(result, null);
  });
}

module.exports = { registerTests };
