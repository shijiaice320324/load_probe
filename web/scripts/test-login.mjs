// 验证登录流程：输入 API Key → 点击进入看板 → 断言顶栏出现
// 用法： node scripts/test-login.mjs <url> <apiKey> <输出png>
import puppeteer from 'puppeteer-core'
import path from 'node:path'

const [, , url = 'http://127.0.0.1:5173/', key = '', out = 'shots/login-flow.png'] = process.argv
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 1200 })

await page.goto(url, { waitUntil: 'networkidle0', timeout: 60000 })
await page.waitForSelector('input[type="password"]')
await page.type('input[type="password"]', key)
await page.click('button[type="submit"]')

await page.waitForSelector('.topbar', { timeout: 30000 })
const hostText = await page.$eval('.host', (el) => el.textContent?.trim() ?? '')
const kpiCount = await page.$$eval('.kpi-card', (els) => els.length)
const chartCount = await page.$$eval('.chart', (els) => els.length)
await new Promise((r) => setTimeout(r, 1500))
await page.screenshot({ path: path.resolve(out), fullPage: true })
console.log(JSON.stringify({ ok: true, hostText, kpiCount, chartCount, shot: out }))
await browser.close()
