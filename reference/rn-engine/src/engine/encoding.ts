/** Converts a complete solution grid to a hex string. */
export function gridToHex(grid: (0 | 1)[][]): string {
  const bits: number[] = [];
  for (const row of grid) for (const v of row) bits.push(v);
  return bitsToHex(bits);
}

/** Converts a boolean mask grid to a hex string (true = 1 revealed, false = 0 hidden). */
export function maskToHex(mask: boolean[][]): string {
  const bits: number[] = [];
  for (const row of mask) for (const v of row) bits.push(v ? 1 : 0);
  return bitsToHex(bits);
}

function bitsToHex(bits: number[]): string {
  if (bits.length % 4 !== 0) {
    throw new Error(`Bit count must be a multiple of 4 to encode as hex. Received ${bits.length}.`);
  }

  let hex = '';
  for (let i = 0; i < bits.length; i += 4) {
    const nibble =
      (bits[i] << 3) | (bits[i + 1] << 2) | (bits[i + 2] << 1) | bits[i + 3];
    hex += nibble.toString(16);
  }
  return hex;
}

/** Fisher-Yates in-place shuffle. */
export function shuffle<T>(arr: T[]): void {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
}
