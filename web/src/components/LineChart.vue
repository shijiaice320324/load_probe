<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { formatAxisTime } from '../utils/format'

interface LineSeries {
  name: string
  data: number[]
  color: string
  area?: boolean
}

const props = defineProps<{
  title: string
  xData: string[]
  series: LineSeries[]
  unit: string
  yMax?: number
  spanSeconds: number
}>()

const el = ref<HTMLDivElement | null>(null)
let chart: ReturnType<typeof echarts.init> | null = null
let resizeObserver: ResizeObserver | null = null

function render() {
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value)
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(el.value)
  }
  const timestamps = props.xData.map((iso) => Date.parse(iso))
  // 轴标签粒度按"实际数据跨度"决定，否则刚启动只有两分钟数据时会重复成 15:44 15:44
  const actualSpan =
    timestamps.length > 1 ? (timestamps[timestamps.length - 1] - timestamps[0]) / 1000 : props.spanSeconds
  chart.setOption(
    {
      animation: false,
      grid: { containLabel: true, left: 8, right: 20, top: 44, bottom: 62 },
      legend: { top: 8, textStyle: { color: '#94a3b8' }, itemWidth: 12, itemHeight: 8 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.94)',
        borderColor: '#334155',
        textStyle: { color: '#e2e8f0' },
        valueFormatter: (value: unknown) =>
          typeof value === 'number' ? `${value.toFixed(2)} ${props.unit}` : String(value)
      },
      xAxis: {
        type: 'time',
        axisLabel: {
          color: '#94a3b8',
          formatter: (value: number) => formatAxisTime(new Date(value).toISOString(), actualSpan)
        },
        axisLine: { lineStyle: { color: '#334155' } },
        splitLine: { show: false }
      },
      yAxis: {
        type: 'value',
        max: props.yMax,
        axisLabel: { color: '#94a3b8', formatter: `{value} ${props.unit}` },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      dataZoom: [
        { type: 'inside' },
        {
          type: 'slider',
          height: 16,
          bottom: 12,
          borderColor: '#334155',
          backgroundColor: '#0f1b2d',
          fillerColor: 'rgba(56, 189, 248, 0.18)',
          handleStyle: { color: '#38bdf8' },
          moveHandleStyle: { color: '#38bdf8' },
          textStyle: { color: '#64748b' },
          dataBackground: {
            lineStyle: { color: '#1e293b' },
            areaStyle: { color: '#1e293b' }
          },
          selectedDataBackground: {
            lineStyle: { color: '#38bdf8' },
            areaStyle: { color: 'rgba(56, 189, 248, 0.15)' }
          }
        }
      ],
      series: props.series.map((item) => ({
        name: item.name,
        type: 'line',
        showSymbol: false,
        smooth: true,
        lineStyle: { width: 2, color: item.color },
        itemStyle: { color: item.color },
        areaStyle: item.area ? { opacity: 0.14, color: item.color } : undefined,
        data: timestamps.map((ts, index) => [ts, item.data[index] ?? 0])
      }))
    },
    true
  )
}

onMounted(render)
watch(() => [props.xData, props.series, props.unit, props.spanSeconds], render, { deep: true })
onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <section class="chart-card">
    <h2>{{ title }}</h2>
    <div ref="el" class="chart"></div>
  </section>
</template>
