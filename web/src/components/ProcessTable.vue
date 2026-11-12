<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProcessPoint } from '../api/types'
import { formatBytes, formatPercent } from '../utils/format'

const props = defineProps<{ processes: ProcessPoint[] }>()

type SortKey = 'cpuUsagePct' | 'memRssBytes' | 'memUsagePct'
const sortKey = ref<SortKey>('cpuUsagePct')

const rows = computed(() =>
  [...props.processes].sort((a, b) => b[sortKey.value] - a[sortKey.value]).slice(0, 20)
)

function sortBy(key: SortKey) {
  sortKey.value = key
}
</script>

<template>
  <section class="table-card">
    <header class="table-head">
      <h2>Top 进程</h2>
      <span class="muted">按 {{ sortKey === 'cpuUsagePct' ? 'CPU' : '内存' }} 排序，最多显示 20 行</span>
    </header>
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>进程</th>
            <th>PID</th>
            <th class="clickable" @click="sortBy('cpuUsagePct')">
              CPU {{ sortKey === 'cpuUsagePct' ? '▾' : '' }}
            </th>
            <th class="clickable" @click="sortBy('memRssBytes')">
              内存 {{ sortKey === 'memRssBytes' ? '▾' : '' }}
            </th>
            <th class="clickable" @click="sortBy('memUsagePct')">
              内存占比 {{ sortKey === 'memUsagePct' ? '▾' : '' }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="`${row.pid}-${row.name}`">
            <td class="name" :title="row.name">{{ row.name }}</td>
            <td class="num">{{ row.pid }}</td>
            <td class="num">{{ formatPercent(row.cpuUsagePct, 2) }}</td>
            <td class="num">{{ formatBytes(row.memRssBytes) }}</td>
            <td class="num">{{ formatPercent(row.memUsagePct, 2) }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="5" class="empty">暂无进程数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
