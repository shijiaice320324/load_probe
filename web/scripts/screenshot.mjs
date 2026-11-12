// 用系统自带 Edge + puppeteer-core 截图，用于验证前端渲染效果
// 用法： node scripts/screenshot.mjs <url> <输出png> <apiKey>
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'
import path from 'node:path'

const [, , url = 'http://127.0.0.1:5173/', out = 'shots/dashboard.png', key = ''] = process.argv
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu', '--window-size=1440,1400']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 1400, deviceScaleFactor: 1 })

if (key) {
  await page.evaluateOnNewDocument((k) => sessionStorage.setItem('loadprobe.apiKey', k), key)
}
page.on('pageerror', (e) => console.log('[pageerror]', e.message))
page.on('console', (m) => {
  if (m.type() === 'error') console.log('[console.error]', m.text())
})

await page.goto(url, { waitUntil: 'networkidle0', timeout: 60000 })
await new Promise((resolve) => setTimeout(resolve, 2500))
fs.mkdirSync(path.dirname(out), { recursive: true })
await page.screenshot({ path: out, fullPage: true })
console.log('saved', out)
await browser.close()
