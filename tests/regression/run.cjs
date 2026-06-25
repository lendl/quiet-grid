const Module = require('node:module');
const { register } = require('ts-node');
const { runAllTests } = require('./testHarness.cjs');

// react-native cannot be loaded in a plain Node.js environment; provide a minimal
// stub so source modules that import it at the top level can be required by ts-node.
const _origLoad = Module._load;
Module._load = function stubReactNative(request, parent, isMain) {
  if (request === 'react-native') {
    return {
      Dimensions: { get: () => ({ width: 800, height: 1200 }) },
    };
  }
  return _origLoad.call(this, request, parent, isMain);
};

register({
  skipProject: true,
  transpileOnly: true,
  compilerOptions: {
    target: 'ES2022',
    module: 'CommonJS',
    moduleResolution: 'Node',
    ignoreDeprecations: '6.0',
    esModuleInterop: true,
    jsx: 'react-jsx',
  },
});

require('./sessionResults.cjs').registerTests();
require('./shellContracts.cjs').registerTests();
require('./persistenceNavigation.cjs').registerTests();

void runAllTests();
