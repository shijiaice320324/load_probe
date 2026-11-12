import { apiGet } from './client'
import type { Health, Latest, ProcessSeries, SystemSeries } from './types'

export function fetchHealth(): Promise<Health> {
  return apiGet<Health>('/api/v1/health')
}

export function fetchLatest(): Promise<Latest> {
  return apiGet<Latest>('/api/v1/metrics/latest')
}

export function fetchSystemSeries(from: string, to: string, interval: string): Promise<SystemSeries> {
  return apiGet<SystemSeries>('/api/v1/metrics/system', { from, to, interval })
}

export function fetchProcesses(from: string, to: string, limit: number): Promise<ProcessSeries> {
  return apiGet<ProcessSeries>('/api/v1/metrics/processes', { from, to, limit })
}
