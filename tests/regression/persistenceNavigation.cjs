const path = require('node:path');
const fs = require('node:fs');
const { assert, clearProjectModules, test, withMocks } = require('./testHarness.cjs');

const ROOT = path.resolve(__dirname, '..', '..');

function loadActiveSessionStateModule(storageState) {
  clearProjectModules(ROOT, [
    path.join('src', 'app', 'utils', 'activeSessionStateStorage.ts'),
    path.join('src', 'app', 'shell', 'storage', 'activeSessionStorage.ts'),
  ]);

  const asyncStorageMock = {
    async getItem() {
      return storageState.raw;
    },
    async setItem(_key, value) {
      storageState.raw = value;
    },
    async removeItem() {
      storageState.raw = null;
      storageState.removed += 1;
    },
  };

  return withMocks(
    { '@react-native-async-storage/async-storage': asyncStorageMock },
    () => require(path.join(ROOT, 'src', 'app', 'utils', 'activeSessionStateStorage.ts')),
  );
}

function createLegacyTakuzuPayload() {
  return {
    puzzle: {
      id: 'legacy-takuzu',
      size: 6,
      difficulty: 'easy',
      solution: '000000000',
      mask: '000000000',
    },
    board: Array.from({ length: 6 }, () => Array.from({ length: 6 }, () => null)),
    elapsedSeconds: 12,
    accuracyDrops: 1,
  };
}

function createWordSearchActiveSession() {
  return {
    gameId: 'wordsearch',
    puzzle: {
      id: 'ws-1',
      difficulty: 'medium',
      rows: 2,
      cols: 2,
      language: 'en',
      themeId: 'animals',
      grid: [['C', 'A'], ['T', 'S']],
      words: [
        {
          id: 'cat',
          word: 'CAT',
          positions: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 1, col: 0 }],
        },
      ],
      hiddenWord: {
        word: 'AS',
        clue: 'Plural helper',
        positions: [{ row: 0, col: 1 }, { row: 1, col: 1 }],
      },
    },
    foundWordIds: ['cat'],
    tempSelection: {
      start: { row: 0, col: 0 },
      end: { row: 1, col: 0 },
      path: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 1, col: 0 }],
    },
    hiddenWordMode: true,
    hiddenWordProgress: [{ row: 0, col: 1 }],
    hiddenWordSolved: false,
    elapsedSeconds: 42,
  };
}

function registerTests() {
  test('active session storage migrates legacy takuzu payloads', async () => {
    const storageState = {
      raw: JSON.stringify({
        gameId: 'takuzu',
        version: 1,
        payload: createLegacyTakuzuPayload(),
      }),
      removed: 0,
    };
    const { loadActiveSessionState } = loadActiveSessionStateModule(storageState);

    const session = await loadActiveSessionState();

    assert.equal(session.gameId, 'takuzu');
    assert.equal(session.puzzle.rows, 6);
    assert.equal(session.puzzle.cols, 6);
    assert.equal(session.finishedCells.length, 6);
    assert.deepEqual(session.penalizedLineKeys, []);
    assert.equal(storageState.removed, 0);
  });

  test('active session storage round-trips normalized envelopes', async () => {
    const storageState = { raw: null, removed: 0 };
    const {
      loadActiveSessionState,
      saveActiveSessionState,
    } = loadActiveSessionStateModule(storageState);
    const activeSession = createWordSearchActiveSession();

    await saveActiveSessionState(activeSession);
    activeSession.foundWordIds.push('mutated-after-save');

    const persistedEnvelope = JSON.parse(storageState.raw);
    const restored = await loadActiveSessionState();

    assert.equal(persistedEnvelope.version, 1);
    assert.equal(persistedEnvelope.gameId, 'wordsearch');
    assert.deepEqual(persistedEnvelope.payload.foundWordIds, ['cat']);
    assert.equal(restored.gameId, 'wordsearch');
    assert.deepEqual(restored.foundWordIds, ['cat']);
    assert.equal(restored.hiddenWordMode, true);
  });

  test('active session storage clears corrupted payloads', async () => {
    const storageState = {
      raw: JSON.stringify({
        gameId: 'sudoku',
        version: 1,
        payload: { invalid: true },
      }),
      removed: 0,
    };
    const { loadActiveSessionState } = loadActiveSessionStateModule(storageState);

    const restored = await loadActiveSessionState();

    assert.equal(restored, null);
    assert.equal(storageState.removed, 1);
    assert.equal(storageState.raw, null);
  });

  test('navigation seam keeps shell route names stable', () => {
    const gameNavigationSource = fs.readFileSync(
      path.join(ROOT, 'src', 'app', 'utils', 'gameNavigation.ts'),
      'utf8',
    );
    const returnToHomeSource = fs.readFileSync(
      path.join(ROOT, 'src', 'app', 'navigation', 'returnToHome.ts'),
      'utf8',
    );

    assert.match(gameNavigationSource, /navigation\.navigate\('PuzzlePlay', params\)/);
    assert.match(gameNavigationSource, /StackActions\.replace\('PuzzlePlay', params\)/);
    assert.match(gameNavigationSource, /resume: true/);
    assert.match(returnToHomeSource, /navigation\.navigate\('MainTabs', \{ screen: 'Games' \}\)/);
  });
}

module.exports = { registerTests };
