const path = require('node:path');
const { assert, test } = require('./testHarness.cjs');

const ROOT = path.resolve(__dirname, '..', '..');

const { minesweeperPlayContract } = require(path.join(ROOT, 'src', 'games', 'minesweeper', 'gameplay', 'playContract.ts'));
const { nonogramPlayContract } = require(path.join(ROOT, 'src', 'games', 'nonogram', 'gameplay', 'playContract.ts'));
const { sudokuPlayContract } = require(path.join(ROOT, 'src', 'games', 'sudoku', 'gameplay', 'playContract.ts'));
const { takuzuPlayContract } = require(path.join(ROOT, 'src', 'games', 'takuzu', 'gameplay', 'playContract.ts'));

function toSolvedNonogramBoard(solution) {
  return solution.map((row) => row.map((cell) => (cell ? 1 : 0)));
}

function registerContractSuite(gameId, contract, makeSolvedSession) {
  test(`${gameId} play contract creates, persists, and restores a session`, () => {
    const session = contract.createSession({ difficulty: 'easy' });

    assert.ok(session, `${gameId} should create an easy session`);
    assert.equal(session.puzzle.difficulty, 'easy');
    assert.equal(contract.hasMeaningfulProgress(session), false);

    const serialized = contract.serializeSession({ session, elapsedSeconds: 33 });
    const restored = contract.restoreSession(serialized);

    assert.equal(serialized.gameId, gameId);
    assert.equal(serialized.elapsedSeconds, 33);
    assert.equal(contract.canResume(serialized), true);
    assert.equal(restored.elapsedSeconds, 33);
    assert.deepEqual(contract.serializeSession({
      session: restored.session,
      elapsedSeconds: restored.elapsedSeconds,
    }), serialized);

    const solvedState = contract.getSolvedState({
      session: makeSolvedSession(session),
      elapsedSeconds: 33,
    });

    assert.ok(solvedState, `${gameId} should report solved state`);
    assert.equal(solvedState.gameId, gameId);
    assert.equal(solvedState.status, 'solved');
    assert.equal(solvedState.elapsedSeconds, 33);
  });
}

function registerTests() {
  registerContractSuite('takuzu', takuzuPlayContract, (session) => ({
    ...session,
    board: session.solution.map((row) => [...row]),
  }));

  registerContractSuite('minesweeper', minesweeperPlayContract, (session) => ({
    ...session,
    board: {
      ...session.board,
      status: 'won',
    },
  }));

  registerContractSuite('nonogram', nonogramPlayContract, (session) => ({
    ...session,
    board: toSolvedNonogramBoard(session.solution),
  }));

  registerContractSuite('sudoku', sudokuPlayContract, (session) => ({
    ...session,
    board: session.puzzle.solution.map((row) => [...row]),
  }));

}

module.exports = { registerTests };
