<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fetchAlertRules, updateAlertRule } from '../api/metrics'
import type { AlertRule } from '../api/types'

const props = defineProps<{ refreshKey: number }>()

interface EditState {
  threshold: number
  consecutiveSamples: number
  enabled: boolean
  saving: boolean
  saved: boolean
  error: string
}

const rules = ref<AlertRule[]>([])
const edits = ref<Record<string, EditState>>({})
const loading = ref(false)
const error = ref('')

function unit(rule: AlertRule): string {
  return rule.metric === 'COLLECT_AGE_SECONDS' ? '秒' : '%'
}

function toEdit(rule: AlertRule): EditState {
  return {
    threshold: rule.threshold,
    consecutiveSamples: rule.consecutiveSamples,
    enabled: rule.enabled,
    saving: false,
    saved: false,
    error: ''
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    rules.value = await fetchAlertRules()
    const next: Record<string, EditState> = {}
    for (const rule of rules.value) {
      next[rule.id] = toEdit(rule)
    }
    edits.value = next
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function save(rule: AlertRule) {
  const state = edits.value[rule.id]
  if (!state) return
  state.saving = true
  state.error = ''
  state.saved = false
  try {
    const updated = await updateAlertRule(rule.id, {
      threshold: state.threshold,
      consecutiveSamples: state.consecutiveSamples,
      enabled: state.enabled
    })
    rule.threshold = updated.threshold
    rule.consecutiveSamples = updated.consecutiveSamples
    rule.enabled = updated.enabled
    state.saved = true
    window.setTimeout(() => {
      state.saved = false
    }, 2000)
  } catch (e) {
    state.error = (e as Error).message
  } finally {
    state.saving = false
  }
}

function isDirty(rule: AlertRule): boolean {
  const state = edits.value[rule.id]
  if (!state) return false
  return (
    state.threshold !== rule.threshold ||
    state.consecutiveSamples !== rule.consecutiveSamples ||
    state.enabled !== rule.enabled
  )
}

onMounted(load)
watch(() => props.refreshKey, load)
</script>

<template>
  <section class="table-card">
    <header class="table-head">
      <h2>告警规则（可直接修改阈值）</h2>
      <span class="muted">保存后立即生效，无需重启</span>
    </header>

    <p v-if="error" class="banner error">{{ error }}</p>

    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>规则</th>
            <th>指标</th>
            <th>阈值</th>
            <th>连续次数</th>
            <th>级别</th>
            <th>启用</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id">
            <td class="name" :title="rule.message">{{ rule.name }}<span class="muted">（{{ rule.id }}）</span></td>
            <td class="muted">{{ rule.metric }}</td>
            <td>
              <input
                v-model.number="edits[rule.id].threshold"
                type="number"
                step="0.1"
                class="num-input"
                :min="rule.metric === 'COLLECT_AGE_SECONDS' ? 5 : 0"
                :max="rule.metric === 'COLLECT_AGE_SECONDS' ? 86400 : 100"
              />
              <span class="unit">{{ unit(rule) }}</span>
            </td>
            <td>
              <input v-model.number="edits[rule.id].consecutiveSamples" type="number" min="1" max="100" class="num-input" />
            </td>
            <td>
              <span class="tag" :class="rule.level.toLowerCase()">{{ rule.level }}</span>
            </td>
            <td>
              <input v-model="edits[rule.id].enabled" type="checkbox" />
            </td>
            <td>
              <button class="ghost" :disabled="edits[rule.id].saving || !isDirty(rule)" @click="save(rule)">
                {{ edits[rule.id].saving ? '保存中…' : edits[rule.id].saved ? '已保存' : '保存' }}
              </button>
              <span v-if="edits[rule.id].error" class="error-text">{{ edits[rule.id].error }}</span>
            </td>
          </tr>
          <tr v-if="rules.length === 0">
            <td colspan="7" class="empty">{{ loading ? '加载中…' : '暂无规则' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
