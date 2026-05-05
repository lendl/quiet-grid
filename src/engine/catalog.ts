import fs from 'fs';

const DEFAULT_CHUNK_SIZE = 500;

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

function parseEntryBody<TEntry>(body: string, options: CatalogFileOptions<TEntry>): TEntry[] {
  const trimmedBody = body.trim();
  if (trimmedBody.length === 0) {
    return [];
  }

  const normalizedJson = `[${trimmedBody}]`
    .replace(/(\w+):/g, '"$1":')
    .replace(/'/g, '"')
    .replace(/,\s*}/g, '}')
    .replace(/,\s*]/g, ']');
  const entries = JSON.parse(normalizedJson) as TEntry[];
  return entries.map((entry) => options.normalizeParsedEntry(entry));
}

export function readChunkedCatalog<TEntry>(
  filePath: string,
  options: CatalogFileOptions<TEntry>,
): TEntry[] {
  if (!fs.existsSync(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const entryTypeName = escapeRegex(options.entryTypeName);
  const chunkPattern = new RegExp(
    `const chunk\\d+: ${entryTypeName}\\[] = \\[(?<body>[\\s\\S]*?)\\];`,
    'g',
  );
  const chunkMatches = [...content.matchAll(chunkPattern)];
  if (chunkMatches.length > 0) {
    return chunkMatches.flatMap((match) => parseEntryBody(match.groups?.body ?? '', options));
  }

  const allEntriesPattern = new RegExp(
    `const \\w+: ${entryTypeName}\\[] = \\[(?<body>[\\s\\S]*?)\\];`,
  );
  const allEntriesMatch = content.match(allEntriesPattern);
  if (allEntriesMatch?.groups?.body !== undefined) {
    return parseEntryBody(allEntriesMatch.groups.body, options);
  }

  throw new Error(`Cannot parse catalog at ${filePath}`);
}

export function writeChunkedCatalog<TEntry>(
  filePath: string,
  entries: TEntry[],
  options: CatalogFileOptions<TEntry>,
): void {
  fs.writeFileSync(filePath, buildChunkedCatalogContent(entries, options), 'utf-8');
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
