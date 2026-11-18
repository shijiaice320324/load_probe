<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fetchAlerts } from '../api/metrics'
import type { AlertItem } from '../api/types'
import { formatDateTime, formatPercent } from '../utils/format'

const props = defineProps<{ refreshKey: number }>()

const items = ref<AlertItem[]>([])
const status = ref('')
const level = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const to = new Date()
    const from = new Date(to.getTime() - 7 * 24 * 3600 * 1000)
    const data = await fetchAlerts({
      from: from.toISOString(),
      to: to.toISOString(),
      status: status.value || undefined,
      level: level.value || undefined,
      limit: 200
    })
    items.value = data.items
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function duration(item: AlertItem): string {
  const start = new Date(item.startedAt).getTime()
  const end = item.resolvedAt ? new Date(item.resolvedAt).getTime() : Date.now()
  const seconds = Math.max(0, Math.round((end - start) / 1000))
  if (seconds < 60) return `${seconds} 秒`
  if (seconds < 3600) return `${Math.round(seconds / 60)} 分钟`
  return `${(seconds / 3600).toFixed(1)} 小时`
}

onMounted(load)
watch(() => props.refreshKey, load)
watch([status, level], load)
</script>

<template>
  <section class="table-card">
    <header class="table-head">
      <h2>告警历史（近 7 天）</h2>
      <div class="alert-filters">
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="FIRING">触发中</option>
          <option value="RESOLVED">已恢复</option>
        </select>
        <select v-model="level">
          <option value="">全部级别</option>
          <option value="CRITICAL">CRITICAL</option>
          <option value="WARNING">WARNING</option>
        </select>
        <span class="muted">{{ items.length }} 条</span>
      </div>
    </header>

    <p v-if="error" class="banner error">{{ error }}</p>

    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>触发时间</th>
            <th>规则</th>
            <th>级别</th>
            <th>状态</th>
            <th>指标值 / 阈值</th>
            <th>持续</th>
            <th>恢复时间</th>
            <th>通知</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ formatDateTime(item.startedAt) }}</td>
            <td class="name" :title="item.message">{{ item.ruleId }}</td>
            <td>
              <span class="tag" :class="item.level.toLowerCase()">{{ item.level }}</span>
            </td>
            <td>
              <span class="tag" :class="item.status === 'FIRING' ? 'firing' : 'resolved'">
                {{ item.status === 'FIRING' ? '触发中' : '已恢复' }}
              </span>
            </td>
            <td class="num">{{ formatPercent(item.value) }} / {{ formatPercent(item.threshold) }}</td>
            <td class="num">{{ duration(item) }}</td>
            <td>{{ item.resolvedAt ? formatDateTime(item.resolvedAt) : '-' }}</td>
            <td class="muted">{{ item.notifyChannel ?? '未发送' }}</td>
          </tr>
          <tr v-if="items.length === 0">
            <td colspan="8" class="empty">{{ loading ? '加载中…' : '暂无告警记录' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
