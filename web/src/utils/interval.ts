export interface TimeRange {
  label: string
  seconds: number
}

export const RANGE_PRESETS: TimeRange[] = [
  { label: '最近 15 分钟', seconds: 15 * 60 },
  { label: '最近 1 小时', seconds: 60 * 60 },
  { label: '最近 6 小时', seconds: 6 * 60 * 60 },
  { label: '最近 24 小时', seconds: 24 * 60 * 60 },
  { label: '最近 7 天', seconds: 7 * 24 * 60 * 60 }
]

const CANDIDATE_INTERVALS = [10, 30, 60, 300, 900, 1800, 3600, 7200, 36000]

/** 根据跨度选一个间隔，保证点数不超过 targetPoints（后端还有一层兜底） */
export function pickInterval(spanSeconds: number, targetPoints = 400): string {
  const wanted = Math.ceil(spanSeconds / targetPoints)
  const chosen = CANDIDATE_INTERVALS.find((candidate) => candidate >= wanted) ?? CANDIDATE_INTERVALS[CANDIDATE_INTERVALS.length - 1]
  return `${chosen}s`
}

export function toIso(date: Date): string {
  return date.toISOString()
}
