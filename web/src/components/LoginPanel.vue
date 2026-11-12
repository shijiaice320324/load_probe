<script setup lang="ts">
import { ref } from 'vue'
import { setApiKey } from '../api/client'
// 用受保护的查询接口校验 Key（/api/v1/health 是免鉴权的，不能用来验 Key）
import { fetchLatest } from '../api/metrics'

const emit = defineEmits<{ (e: 'success'): void }>()

const input = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  const key = input.value.trim()
  if (!key) {
    error.value = '请输入 API Key'
    return
  }
  busy.value = true
  error.value = ''
  setApiKey(key)
  try {
    await fetchLatest()
    emit('success')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <form class="login-card" @submit.prevent="submit">
      <h1>load_probe</h1>
      <p class="muted">本机资源监控 · 请输入查询接口的 API Key</p>
      <input
        v-model="input"
        type="password"
        placeholder="lpk_..."
        autocomplete="off"
        :disabled="busy"
      />
      <button type="submit" :disabled="busy || !input.trim()">
        {{ busy ? '验证中…' : '进入看板' }}
      </button>
      <p v-if="error" class="error">{{ error }}</p>
      <p class="hint">Key 保存在 sessionStorage，关闭标签页即失效。</p>
    </form>
  </div>
</template>
