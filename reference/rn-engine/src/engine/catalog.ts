import fs from 'fs';

const DEFAULT_CHUNK_SIZE = 500;
const WRITE_RETRY_CODES = new Set(['EBUSY', 'EPERM', 'UNKNOWN']);
const WRITE_RETRY_DELAYS_MS = [50, 100, 200, 400, 800];
const SLEEP_BUFFER = new Int32Array(new SharedArrayBuffer(4));

export interface CatalogFileOptions<TEntry> {
  importTypePath: string;
  entryTypeName: string;
  chunkSize?: number;
  formatEntry(entry: TEntry): string;
  normalizeParsedEntry(entry: TEntry): TEntry;
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function buildChunkedCatalogContent<TEntry>(
  entries: TEntry[],
  options: CatalogFileOptions<TEntry>,
): string {
  const chunkSize = options.chunkSize ?? DEFAULT_CHUNK_SIZE;
  const chunkBlocks: string[] = [];
  const chunkRefs: string[] = [];

  for (let index = 0; index < entries.length; index += chunkSize) {
    const chunkNumber = Math.floor(index / chunkSize) + 1;
    const chunkName = `chunk${chunkNumber}`;
    const chunk = entries.slice(index, index + chunkSize);
    const chunkBody = chunk.map((entry) => options.formatEntry(entry)).join('\n');
    const block = chunkBody.length > 0
      ? `const ${chunkName}: ${options.entryTypeName}[] = [\n${chunkBody}\n];`
      : `const ${chunkName}: ${options.entryTypeName}[] = [\n];`;

    chunkBlocks.push(block);
    chunkRefs.push(`...${chunkName}`);
  }

  const allEntriesDefinition = chunkRefs.length > 0
    ? `const allEntries: ${options.entryTypeName}[] = [${chunkRefs.join(', ')}];`
    : `const allEntries: ${options.entryTypeName}[] = [];`;

  return `import type { ${options.entryTypeName} } from '${options.importTypePath}';

${chunkBlocks.join('\n\n')}
${chunkBlocks.length > 0 ? '\n\n' : ''}${allEntriesDefinition}

export default allEntries;
`;
}

function readCatalogFromModule<TEntry>(
  filePath: string,
  options: CatalogFileOptions<TEntry>,
): TEntry[] {
  const resolvedPath = require.resolve(filePath);
  delete require.cache[resolvedPath];
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const moduleExports = require(resolvedPath) as { default?: unknown };
  const raw = moduleExports.default ?? moduleExports;

  if (Array.isArray(raw)) {
    return raw.map((entry) => options.normalizeParsedEntry(entry as TEntry));
  }

  if (raw && typeof raw === 'object') {
    const arrays = Object.values(raw as Record<string, unknown>)
      .filter((value): value is unknown[] => Array.isArray(value));
    return arrays.flatMap((items) => items)
      .map((entry) => options.normalizeParsedEntry(entry as TEntry));
  }

  throw new Error(`Unsupported catalog module shape at ${filePath}`);
}

export function readChunkedCatalog<TEntry>(
  filePath: string,
  options: CatalogFileOptions<TEntry>,
): TEntry[] {
  if (!fs.existsSync(filePath)) {
    return [];
  }

  return readCatalogFromModule(filePath, options);
}

export function writeChunkedCatalog<TEntry>(
  filePath: string,
  entries: TEntry[],
  options: CatalogFileOptions<TEntry>,
): void {
  const content = buildChunkedCatalogContent(entries, options);
  const contentBuffer = Buffer.from(content, 'utf-8');

  for (let attempt = 0; attempt <= WRITE_RETRY_DELAYS_MS.length; attempt += 1) {
    let fileDescriptor: number | null = null;
    try {
      fileDescriptor = fs.openSync(filePath, 'w');

      let written = 0;
      while (written < contentBuffer.length) {
        written += fs.writeSync(
          fileDescriptor,
          contentBuffer,
          written,
          contentBuffer.length - written,
          written,
        );
      }

      fs.closeSync(fileDescriptor);
      fileDescriptor = null;
      return;
    } catch (error) {
      if (fileDescriptor !== null) {
        fs.closeSync(fileDescriptor);
      }

      const code = error instanceof Error && 'code' in error ? String(error.code) : null;
      if (code === null || !WRITE_RETRY_CODES.has(code) || attempt === WRITE_RETRY_DELAYS_MS.length) {
        throw error;
      }

      Atomics.wait(SLEEP_BUFFER, 0, 0, WRITE_RETRY_DELAYS_MS[attempt]);
    }
  }
}

export function getNextId(filePath: string, prefix: string): string {
  if (!fs.existsSync(filePath)) {
    return `${prefix}1`;
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const pattern = new RegExp(`id:\\s*'${escapeRegex(prefix)}(\\d+)'`, 'g');
  let max = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(content)) !== null) {
    max = Math.max(max, parseInt(match[1], 10));
  }

  return `${prefix}${max + 1}`;
}
