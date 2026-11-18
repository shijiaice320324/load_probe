export interface SystemPoint {
  ts: string
  cpuUsagePct: number
  memUsagePct: number
  diskUsagePct: number
  netRxBytesPerSec: number
  netTxBytesPerSec: number
  sampleCount: number
}

export interface SystemSeries {
  from: string
  to: string
  intervalSeconds: number
  host: string
  points: SystemPoint[]
}

export interface ProcessPoint {
  ts: string
  pid: number
  name: string
  cpuUsagePct: number
  memRssBytes: number
  memUsagePct: number
}

export interface ProcessSeries {
  from: string
  to: string
  limit: number
  points: ProcessPoint[]
}

export interface LatestSystem {
  ts: string
  host: string
  cpuUsagePct: number
  memTotalBytes: number
  memUsedBytes: number
  memUsagePct: number
  diskTotalBytes: number
  diskUsedBytes: number
  diskUsagePct: number
  netRxBytesPerSec: number
  netTxBytesPerSec: number
}

export interface Latest {
  system: LatestSystem | null
  processes: ProcessPoint[]
}

export interface Health {
  status: string
  host: string
  intervalSeconds: number
  topProcessCount: number
  retentionDays: number
  now: string
}

export interface AlertItem {
  id: number
  ruleId: string
  metric: string
  level: 'WARNING' | 'CRITICAL'
  status: 'FIRING' | 'RESOLVED'
  host: string
  value: number
  threshold: number
  message: string
  startedAt: string
  resolvedAt: string | null
  notifiedAt: string | null
  notifyChannel: string | null
}

export interface AlertList {
  from: string
  to: string
  limit: number
  items: AlertItem[]
}

export interface AlertRule {
  id: string
  name: string
  metric: 'CPU' | 'MEM' | 'DISK' | 'COLLECT_AGE_SECONDS'
  threshold: number
  consecutiveSamples: number
  level: 'WARNING' | 'CRITICAL'
  message: string
  enabled: boolean
}
