<script setup lang="ts">
import { computed } from 'vue'
import type { LatestSystem } from '../api/types'
import { formatBytes, formatPercent, formatRate } from '../utils/format'

const props = defineProps<{ system: LatestSystem | null }>()

interface Card {
  title: string
  value: string
  sub: string
  ratio: number | null
  color: string
}

const cards = computed<Card[]>(() => {
  const s = props.system
  if (!s) {
    return [
      { title: 'CPU', value: '-', sub: '暂无数据', ratio: null, color: '#38bdf8' },
      { title: '内存', value: '-', sub: '暂无数据', ratio: null, color: '#a78bfa' },
      { title: '磁盘', value: '-', sub: '暂无数据', ratio: null, color: '#34d399' },
      { title: '网络', value: '-', sub: '暂无数据', ratio: null, color: '#fbbf24' }
    ]
  }
  return [
    {
      title: 'CPU 使用率',
      value: formatPercent(s.cpuUsagePct),
      sub: `采集于 ${new Date(s.ts).toLocaleTimeString()}`,
      ratio: s.cpuUsagePct,
      color: '#38bdf8'
    },
    {
      title: '内存使用率',
      value: formatPercent(s.memUsagePct),
      sub: `${formatBytes(s.memUsedBytes)} / ${formatBytes(s.memTotalBytes)}`,
      ratio: s.memUsagePct,
      color: '#a78bfa'
    },
    {
      title: '磁盘使用率',
      value: formatPercent(s.diskUsagePct),
      sub: `${formatBytes(s.diskUsedBytes)} / ${formatBytes(s.diskTotalBytes)}`,
      ratio: s.diskUsagePct,
      color: '#34d399'
    },
    {
      title: '网络吞吐',
      value: `↑ ${formatRate(s.netTxBytesPerSec)}`,
      sub: `↓ ${formatRate(s.netRxBytesPerSec)}`,
      ratio: null,
      color: '#fbbf24'
    }
  ]
})
</script>

<template>
  <section class="kpi-grid">
    <article v-for="card in cards" :key="card.title" class="kpi-card">
      <header>
        <span class="kpi-title">{{ card.title }}</span>
        <span class="dot" :style="{ background: card.color }"></span>
      </header>
      <p class="kpi-value">{{ card.value }}</p>
      <div class="bar" v-if="card.ratio !== null">
        <i :style="{ width: Math.min(100, Math.max(0, card.ratio)) + '%', background: card.color }"></i>
      </div>
      <p class="kpi-sub">{{ card.sub }}</p>
    </article>
  </section>
</template>
