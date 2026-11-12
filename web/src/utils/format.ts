export function formatPercent(value: number | undefined | null, digits = 1): string {
  if (value === undefined || value === null || Number.isNaN(value)) return '-'
  return `${value.toFixed(digits)}%`
}

export function formatBytes(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null || Number.isNaN(bytes)) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = Math.max(0, bytes)
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return `${value.toFixed(value >= 100 || index === 0 ? 0 : 1)} ${units[index]}`
}

export function formatRate(bytesPerSec: number | undefined | null): string {
  if (bytesPerSec === undefined || bytesPerSec === null || Number.isNaN(bytesPerSec)) return '-'
  return `${formatBytes(bytesPerSec)}/s`
}

/** 时间轴标签：跨度越大粒度越粗，避免 10 秒点挤满坐标轴 */
export function formatAxisTime(iso: string, spanSeconds: number): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (spanSeconds <= 600) {
    return `${time}:${pad(date.getSeconds())}`
  }
  if (spanSeconds <= 86400) {
    return time
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${time}`
}

export function formatDateTime(iso: string | undefined | null): string {
  if (!iso) return '-'
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
