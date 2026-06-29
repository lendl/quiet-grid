// Minimal stub used by the engine CLI (tsx + engine tsconfig) to satisfy
// imports of react-native in game platform code without pulling in the real package.
export const Dimensions = {
  get: (_dim: string) => ({ width: 0, height: 0, scale: 1, fontScale: 1 }),
  addEventListener: () => ({ remove: () => {} }),
  removeEventListener: () => {},
};

export const Platform = { OS: 'ios', select: <T>(obj: { ios?: T; android?: T; default?: T }) => obj.ios ?? obj.default };
export const StyleSheet = { create: <T>(styles: T) => styles, flatten: <T>(style: T) => style };
export const View = {};
export const Text = {};
