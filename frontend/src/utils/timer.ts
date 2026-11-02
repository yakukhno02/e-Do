export function formatMinutesAndSeconds(totalSeconds: number): string {
  const safeSeconds = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

/**
 * Midpoint latency clock skew formula from docs/ARCHITECTURE.md Section 6.1:
 * clientMidpoint = (t0 + t1) / 2
 * clockSkew = serverTime_response - clientMidpoint
 */
export function calculateClockSkew(t0: number, t1: number, serverTimeIso: string): number {
  const clientMidpoint = (t0 + t1) / 2;
  const serverTime = Date.parse(serverTimeIso);
  if (!Number.isFinite(serverTime)) return 0;
  return serverTime - clientMidpoint;
}
