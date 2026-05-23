export interface GroupedItems<T> {
  key: string;
  items: T[];
}

export function groupItemsByKey<T>(
  items: readonly T[],
  getKey: (item: T) => string,
): GroupedItems<T>[] {
  const groups = new Map<string, T[]>();

  items.forEach((item) => {
    const key = getKey(item);
    const existing = groups.get(key);
    if (existing) {
      existing.push(item);
      return;
    }

    groups.set(key, [item]);
  });

  return Array.from(groups.entries()).map(([key, groupedItems]) => ({
    key,
    items: groupedItems,
  }));
}
