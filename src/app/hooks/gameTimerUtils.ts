export function getElapsedSeconds(
  sessionStartedAt: number | null,
  elapsedBeforeSession: number,
  now = Date.now(),
): number {
  if (sessionStartedAt === null) {
    return elapsedBeforeSession;
  }

  return elapsedBeforeSession + Math.floor((now - sessionStartedAt) / 1000);
}
