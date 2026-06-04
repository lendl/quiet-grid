const assert = require('node:assert/strict');
const Module = require('node:module');

const tests = [];

function test(name, run) {
  tests.push({ name, run });
}

async function runAllTests() {
  let failures = 0;

  for (const { name, run } of tests) {
    try {
      await run();
      console.log(`✓ ${name}`);
    } catch (error) {
      failures += 1;
      console.error(`✗ ${name}`);
      console.error(error instanceof Error ? error.stack : error);
    }
  }

  if (failures > 0) {
    console.error(`\n${failures} regression test(s) failed.`);
    process.exitCode = 1;
    return;
  }

  console.log(`\n${tests.length} regression test(s) passed.`);
}

function clearProjectModules(rootPath, fragments) {
  for (const cacheKey of Object.keys(require.cache)) {
    if (
      cacheKey.startsWith(rootPath)
      && fragments.some((fragment) => cacheKey.includes(fragment))
    ) {
      delete require.cache[cacheKey];
    }
  }
}

function withMocks(mocks, load) {
  const originalLoad = Module._load;

  Module._load = function patchedLoad(request, parent, isMain) {
    if (Object.prototype.hasOwnProperty.call(mocks, request)) {
      return mocks[request];
    }
    return originalLoad.call(this, request, parent, isMain);
  };

  try {
    return load();
  } finally {
    Module._load = originalLoad;
  }
}

module.exports = {
  assert,
  clearProjectModules,
  runAllTests,
  test,
  withMocks,
};
