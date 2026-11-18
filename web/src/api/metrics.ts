import { apiGet, apiPut } from './client'
import type { AlertList, AlertRule, Health, Latest, ProcessSeries, SystemSeries } from './types'

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

export function fetchAlerts(params: {
  from?: string
  to?: string
  level?: string
  status?: string
  limit?: number
}): Promise<AlertList> {
  return apiGet<AlertList>('/api/v1/alerts', {
    from: params.from,
    to: params.to,
    level: params.level,
    status: params.status,
    limit: params.limit ?? 200
  })
}

export function fetchAlertRules(): Promise<AlertRule[]> {
  return apiGet<AlertRule[]>('/api/v1/alert-rules')
}

export function updateAlertRule(
  id: string,
  payload: { threshold: number; consecutiveSamples: number; enabled: boolean }
): Promise<AlertRule> {
  return apiPut<AlertRule>(`/api/v1/alert-rules/${id}`, payload)
}
