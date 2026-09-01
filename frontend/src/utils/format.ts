const pad = (n: number) => String(n).padStart(2, '0')

/** "2026-09-05T10:00:00" → "2026-09-05 10:00" */
export function formatDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${formatDate(iso)} ${formatTime(iso)}`
}

/** "2026-09-05T10:00:00" → "2026-09-05" */
export function formatDate(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** "2026-09-05T10:00:00" → "10:00" */
export function formatTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** datetime-local input 기본값용: Date → "YYYY-MM-DDTHH:mm" */
export function toLocalInputValue(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

/** The day track the reservation time-bar sits on. */
export const DAY_TRACK = { start: 8, end: 24 }

/**
 * Position a [start, end) interval on the {@link DAY_TRACK} as left/width percentages.
 * Intervals outside the window are clamped so the bar stays inside the track.
 */
export function dayTrackSpan(startIso: string, endIso: string): { left: number; width: number } {
  const span = DAY_TRACK.end - DAY_TRACK.start
  const hours = (iso: string) => {
    const d = new Date(iso)
    return d.getHours() + d.getMinutes() / 60
  }
  const clamp = (v: number) => Math.min(Math.max(v, 0), 100)
  const left = clamp(((hours(startIso) - DAY_TRACK.start) / span) * 100)
  const right = clamp(((hours(endIso) - DAY_TRACK.start) / span) * 100)
  return { left, width: Math.max(right - left, 0) }
}
