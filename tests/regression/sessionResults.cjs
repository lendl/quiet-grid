const path = require('node:path');
const { assert, test } = require('./testHarness.cjs');

const ROOT = path.resolve(__dirname, '..', '..');

const { computeAccuracyPct, computeFinalScore } = require(path.join(ROOT, 'src', 'app', 'utils', 'scoring.ts'));
const { buildMinesweeperResult } = require(path.join(ROOT, 'src', 'games', 'minesweeper', 'gameplay', 'playContract.ts'));
const { buildNonogramResult } = require(path.join(ROOT, 'src', 'games', 'nonogram', 'gameplay', 'playContract.ts'));
const { buildSudokuResult } = require(path.join(ROOT, 'src', 'games', 'sudoku', 'gameplay', 'playContract.ts'));
const { buildTakuzuResult } = require(path.join(ROOT, 'src', 'games', 'takuzu', 'gameplay', 'playContract.ts'));

const DEFAULT_TIME_CAPS = { easy: 300, medium: 450, hard: 600, expert: 900 };

function registerTests() {
  test('scoring helpers clamp and scale predictably', () => {
    assert.equal(computeFinalScore('easy', 60, 0, DEFAULT_TIME_CAPS), 8000);
    assert.equal(computeFinalScore('medium', 0, 1, DEFAULT_TIME_CAPS), 9500);
    assert.equal(computeAccuracyPct(3), 70);
    assert.equal(computeAccuracyPct(15), 0);
  });

  test('takuzu result builder reports solved and failed states', () => {
    const solvedSession = {
      puzzle: { difficulty: 'easy' },
      board: [[0, 1], [1, 0]],
      solution: [[0, 1], [1, 0]],
      accuracyDrops: 2,
    };
    const failedSession = {
      ...solvedSession,
      board: [[0, 1], [1, null]],
    };

    assert.deepEqual(buildTakuzuResult(solvedSession, 45), {
      gameId: 'takuzu',
      difficulty: 'easy',
      status: 'solved',
      score: computeFinalScore('easy', 45, 2, DEFAULT_TIME_CAPS),
      accuracy: computeAccuracyPct(2),
      elapsedSeconds: 45,
      streak: 0,
    });

    assert.deepEqual(buildTakuzuResult(failedSession, 45), {
      gameId: 'takuzu',
      difficulty: 'easy',
      status: 'failed',
      score: 0,
      accuracy: computeAccuracyPct(2),
      elapsedSeconds: 45,
      streak: 0,
    });
  });

  test('minesweeper result builder preserves current time-based scoring model', () => {
    const solvedSession = {
      puzzle: { difficulty: 'hard' },
      board: { status: 'won' },
    };
    const failedSession = {
      puzzle: { difficulty: 'hard' },
      board: { status: 'lost' },
    };

    assert.deepEqual(buildMinesweeperResult(solvedSession, 120), {
      gameId: 'minesweeper',
      difficulty: 'hard',
      status: 'solved',
      score: computeFinalScore('hard', 120, 0, DEFAULT_TIME_CAPS),
      accuracy: 100,
      elapsedSeconds: 120,
      streak: 0,
    });

    assert.equal(buildMinesweeperResult(failedSession, 120).score, 0);
    assert.equal(buildMinesweeperResult(failedSession, 120).status, 'failed');
    assert.equal(buildMinesweeperResult(failedSession, 120).accuracy, 100);
  });

  test('nonogram and sudoku result builders only award solved boards', () => {
    const nonogramSolved = buildNonogramResult({
      puzzle: { difficulty: 'medium' },
      board: [[1, 0], [0, 1]],
      solution: [[true, false], [false, true]],
    }, 50);
    const sudokuFailed = buildSudokuResult({
      puzzle: { difficulty: 'expert', solution: [[1, 2], [3, 4]] },
      board: [[1, 2], [3, null]],
      accuracyDrops: 1,
    }, 90);

    assert.equal(nonogramSolved.status, 'solved');
    assert.equal(nonogramSolved.accuracy, 100);
    assert.equal(nonogramSolved.score, 9500);

    assert.equal(sudokuFailed.status, 'failed');
    assert.equal(sudokuFailed.score, 0);
    assert.equal(sudokuFailed.accuracy, 90);
  });
}

module.exports = { registerTests };
