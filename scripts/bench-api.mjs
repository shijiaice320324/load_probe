// 简易并发压测：node scripts/bench-api.mjs <base> <apiKey> <path> [total] [concurrency]
import https from 'node:https'

const [, , base = 'https://127.0.0.1:8443', key = '', path = '/api/v1/metrics/latest', totalArg = '200', concArg = '10'] = process.argv
const total = Number(totalArg)
const concurrency = Number(concArg)
const url = new URL(path, base)

const agent = new https.Agent({ keepAlive: true, rejectUnauthorized: false, maxSockets: concurrency })

function once() {
  return new Promise((resolve) => {
    const started = process.hrtime.bigint()
    const req = https.request(
      { hostname: url.hostname, port: url.port, path: url.pathname + url.search, method: 'GET', agent,
        headers: key ? { 'X-API-Key': key } : {} },
      (res) => {
        let bytes = 0
        res.on('data', (chunk) => { bytes += chunk.length })
        res.on('end', () => {
          const ms = Number(process.hrtime.bigint() - started) / 1e6
          resolve({ ok: res.statusCode === 200, status: res.statusCode, ms, bytes })
        })
      }
    )
    req.on('error', () => {
      const ms = Number(process.hrtime.bigint() - started) / 1e6
      resolve({ ok: false, status: 0, ms, bytes: 0 })
    })
    req.end()
  })
}

async function main() {
  const results = []
  let issued = 0
  const startedAt = Date.now()
  async function worker() {
    while (issued < total) {
      issued++
      results.push(await once())
    }
  }
  await Promise.all(Array.from({ length: concurrency }, worker))
  const elapsed = (Date.now() - startedAt) / 1000

  const lat = results.map((r) => r.ms).sort((a, b) => a - b)
  const pct = (p) => lat[Math.min(lat.length - 1, Math.floor((lat.length * p) / 100))]
  const okCount = results.filter((r) => r.ok).length
  const bytes = results.reduce((s, r) => s + r.bytes, 0)

  console.log(JSON.stringify({
    path,
    total,
    concurrency,
    ok: okCount,
    failed: total - okCount,
    rps: Number((total / elapsed).toFixed(1)),
    elapsedSec: Number(elapsed.toFixed(2)),
    latencyMs: {
      min: Number(lat[0].toFixed(1)),
      p50: Number(pct(50).toFixed(1)),
      p95: Number(pct(95).toFixed(1)),
      p99: Number(pct(99).toFixed(1)),
      max: Number(lat[lat.length - 1].toFixed(1)),
      avg: Number((lat.reduce((a, b) => a + b, 0) / lat.length).toFixed(1))
    },
    avgResponseKB: Number((bytes / total / 1024).toFixed(1))
  }, null, 2))
  agent.destroy()
}

main()
