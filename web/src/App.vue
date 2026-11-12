<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import LoginPanel from './components/LoginPanel.vue'
import KpiCards from './components/KpiCards.vue'
import LineChart from './components/LineChart.vue'
import ProcessTable from './components/ProcessTable.vue'
import { UNAUTHORIZED_EVENT, clearApiKey, getApiKey } from './api/client'
import { fetchHealth, fetchLatest, fetchSystemSeries } from './api/metrics'
import type { Health, Latest, ProcessPoint, SystemPoint } from './api/types'
import { RANGE_PRESETS, pickIntervalSeconds, toIso } from './utils/interval'
import { formatDateTime, formatDuration } from './utils/format'

const loggedIn = ref(Boolean(getApiKey()))
const health = ref<Health | null>(null)
const latest = ref<Latest | null>(null)
const processes = ref<ProcessPoint[]>([])
const points = ref<SystemPoint[]>([])
const rangeSeconds = ref(RANGE_PRESETS[1].seconds)
const refreshSeconds = ref(30)
const loading = ref(false)
const error = ref('')
const lastUpdated = ref('')
let timer: number | undefined

const rangeLabel = computed(
  () => RANGE_PRESETS.find((preset) => preset.seconds === rangeSeconds.value)?.label ?? '自定义'
)
const bucketSeconds = ref(60)
const bucketLabel = computed(() => formatDuration(bucketSeconds.value))
const collectIntervalLabel = computed(() =>
  health.value ? `${health.value.intervalSeconds} 秒` : '-'
)
const hasData = computed(() => points.value.length > 0)

const cpuSeries = computed(() => [
  { name: 'CPU %', data: points.value.map((p) => p.cpuUsagePct), color: '#38bdf8', area: true },
  { name: '内存 %', data: points.value.map((p) => p.memUsagePct), color: '#a78bfa' }
])
const netSeries = computed(() => [
  {
    name: '接收 KB/s',
    data: points.value.map((p) => Math.round(p.netRxBytesPerSec / 1024 * 10) / 10),
    color: '#34d399',
    area: true
  },
  {
    name: '发送 KB/s',
    data: points.value.map((p) => Math.round(p.netTxBytesPerSec / 1024 * 10) / 10),
    color: '#fbbf24'
  }
])
const diskSeries = computed(() => [
  { name: '磁盘 %', data: points.value.map((p) => p.diskUsagePct), color: '#f472b6' }
])
const xData = computed(() => points.value.map((p) => p.ts))

async function load() {
  if (!loggedIn.value) return
  loading.value = true
  error.value = ''
  try {
    const to = new Date()
    const from = new Date(to.getTime() - rangeSeconds.value * 1000)
    // 桶不能小于采集周期，否则一个桶里连一个样本都没有，图上会出现断点
    const collectSeconds = health.value?.intervalSeconds ?? 60
    const bucket = Math.max(pickIntervalSeconds(rangeSeconds.value), collectSeconds)
    bucketSeconds.value = bucket
    const [series, latestData, healthData] = await Promise.all([
      fetchSystemSeries(toIso(from), toIso(to), `${bucket}s`),
      fetchLatest(),
      fetchHealth()
    ])
    points.value = series.points
    latest.value = latestData
    processes.value = latestData.processes
    health.value = healthData
    lastUpdated.value = formatDateTime(new Date().toISOString())
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function stopTimer() {
  if (timer !== undefined) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function startTimer() {
  stopTimer()
  if (refreshSeconds.value > 0) {
    timer = window.setInterval(load, refreshSeconds.value * 1000)
  }
}

function logout() {
  clearApiKey()
  loggedIn.value = false
  stopTimer()
  points.value = []
  latest.value = null
  processes.value = []
}

function onLoginSuccess() {
  loggedIn.value = true
  load()
  startTimer()
}

watch(refreshSeconds, startTimer)
watch(rangeSeconds, () => load())

onMounted(() => {
  if (loggedIn.value) {
    load()
    startTimer()
  }
  window.addEventListener(UNAUTHORIZED_EVENT, () => {
    loggedIn.value = false
    stopTimer()
  })
})

onBeforeUnmount(() => {
  stopTimer()
  window.removeEventListener(UNAUTHORIZED_EVENT, () => undefined)
})
</script>

<template>
  <LoginPanel v-if="!loggedIn" @success="onLoginSuccess" />

  <div v-else class="page">
    <header class="topbar">
      <div class="brand">
        <h1>load_probe</h1>
        <span class="host" v-if="health">主机 {{ health.host }}</span>
      </div>

      <div class="controls">
        <label>
          时间范围
          <select v-model.number="rangeSeconds">
            <option v-for="preset in RANGE_PRESETS" :key="preset.label" :value="preset.seconds">
              {{ preset.label }}
            </option>
          </select>
        </label>
        <label>
          自动刷新
          <select v-model.number="refreshSeconds">
            <option :value="0">关闭</option>
            <option :value="10">10 秒</option>
            <option :value="30">30 秒</option>
            <option :value="60">60 秒</option>
          </select>
        </label>
        <button class="ghost" :disabled="loading" @click="load">
          {{ loading ? '刷新中…' : '立即刷新' }}
        </button>
        <button class="ghost danger" @click="logout">退出</button>
      </div>
    </header>

    <p class="statusline">
      <span>{{ rangeLabel }}</span>
      <span class="badge">采集周期 {{ collectIntervalLabel }}</span>
      <span>图表聚合 {{ bucketLabel }}</span>
      <span>数据点 {{ points.length }}</span>
      <span v-if="lastUpdated">更新于 {{ lastUpdated }}</span>
    </p>

    <p v-if="error" class="banner error">{{ error }}</p>
    <p v-if="!error && !loading && !hasData" class="banner">该时间段暂无采集数据，稍后再试或扩大时间范围。</p>

    <KpiCards :system="latest?.system ?? null" />

    <LineChart
      title="CPU / 内存使用率"
      :x-data="xData"
      :series="cpuSeries"
      unit="%"
      :y-max="100"
      :span-seconds="rangeSeconds"
    />
    <LineChart
      title="网络吞吐"
      :x-data="xData"
      :series="netSeries"
      unit="KB/s"
      :span-seconds="rangeSeconds"
    />
    <LineChart
      title="磁盘使用率"
      :x-data="xData"
      :series="diskSeries"
      unit="%"
      :y-max="100"
      :span-seconds="rangeSeconds"
    />

    <ProcessTable :processes="processes" />
  </div>
</template>
