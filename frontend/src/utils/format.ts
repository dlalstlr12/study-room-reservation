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

/** 예약·홀딩은 30분 단위로만 가능하다 (백엔드 SlotValidator와 일치). */
export const SLOT_MINUTES = 30
export const MAX_SLOT_HOURS = 4

/** "08:00" … "23:30" — 하루 30분 슬롯의 시작 시각 목록. */
export function halfHourStarts(): string[] {
  const out: string[] = []
  for (let h = DAY_TRACK.start; h < DAY_TRACK.end; h++) {
    out.push(`${pad(h)}:00`, `${pad(h)}:30`)
  }
  return out
}

/** date('YYYY-MM-DD') + time('HH:mm') → 'YYYY-MM-DDTHH:mm:00' (백엔드가 받는 로컬 형식) */
export function toSlotIso(date: string, time: string): string {
  return `${date}T${time}:00`
}

/** 'YYYY-MM-DDTHH:mm:00' 에 분을 더해 같은 형식으로 반환. */
export function addMinutesIso(iso: string, minutes: number): string {
  const d = new Date(iso)
  d.setMinutes(d.getMinutes() + minutes)
  return toLocalInputValue(d) + ':00'
}

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

/** 240 → "4시간", 90 → "1시간 30분", 25 → "25분", 0 → "0분" */
export function formatMinutes(minutes: number): string {
  if (minutes <= 0) return '0분'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}분`
  if (m === 0) return `${h}시간`
  return `${h}시간 ${m}분`
}
