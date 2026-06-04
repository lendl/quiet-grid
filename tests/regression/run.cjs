const { register } = require('ts-node');
const { runAllTests } = require('./testHarness.cjs');

register({
  skipProject: true,
  transpileOnly: true,
  compilerOptions: {
    target: 'ES2022',
    module: 'CommonJS',
    moduleResolution: 'Node',
    esModuleInterop: true,
    jsx: 'react-jsx',
  },
});

require('./sessionResults.cjs').registerTests();
require('./shellContracts.cjs').registerTests();
require('./persistenceNavigation.cjs').registerTests();

void runAllTests();
