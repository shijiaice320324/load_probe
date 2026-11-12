/**
 * 方案 A：API Key 存在 sessionStorage（关闭标签页即失效），随请求头发送。
 * 收到 401 时清空本地 key 并广播事件，由 App 切回登录页。
 */
const STORAGE_KEY = 'loadprobe.apiKey'
export const UNAUTHORIZED_EVENT = 'loadprobe:unauthorized'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function getApiKey(): string {
  return sessionStorage.getItem(STORAGE_KEY) ?? ''
}

export function setApiKey(key: string): void {
  sessionStorage.setItem(STORAGE_KEY, key.trim())
}

export function clearApiKey(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}

export async function apiGet<T>(
  path: string,
  params?: Record<string, string | number | undefined>
): Promise<T> {
  const url = new URL(path, window.location.origin)
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== '') {
        url.searchParams.set(key, String(value))
      }
    }
  }

  let response: Response
  try {
    response = await fetch(url.toString(), {
      headers: { 'X-API-Key': getApiKey() }
    })
  } catch (e) {
    throw new ApiError(0, `无法连接后端：${(e as Error).message}`)
  }

  if (response.status === 401) {
    clearApiKey()
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    throw new ApiError(401, 'API Key 无效或已失效，请重新输入')
  }

  if (!response.ok) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const body = (await response.json()) as { message?: string }
      if (body?.message) {
        message = body.message
      }
    } catch {
      /* 响应体不是 JSON 时保留默认提示 */
    }
    throw new ApiError(response.status, message)
  }

  return (await response.json()) as T
}
