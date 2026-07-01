const path = require('node:path');
const { assert, test } = require('./testHarness.cjs');

const ROOT = path.resolve(__dirname, '..', '..');

const {
  buildHiddenWordPool,
  pickHiddenWord,
  reserveHiddenWordCells,
} = require(path.join(ROOT, 'src', 'games', 'wordsearch', 'engine', 'hiddenWord.ts'));

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
}

module.exports = { registerTests };
