import { readdir } from 'node:fs/promises'
import { join, resolve } from 'node:path'
import { MemoryStorage, TelegramClient } from '@mtcute/node'

const apkDir = resolve(process.env.APK_DIR ?? 'out')
const apiId = Number(process.env.TELEGRAM_API_ID)
const apiHash = process.env.TELEGRAM_API_HASH
const botToken = process.env.TELEGRAM_BOT_TOKEN
const chatRaw = process.env.TELEGRAM_DM_CHAT?.trim()

if (!apiId || !apiHash || !botToken) {
  throw new Error('TELEGRAM_API_ID, TELEGRAM_API_HASH and TELEGRAM_BOT_TOKEN must be set')
}
if (!chatRaw) throw new Error('TELEGRAM_DM_CHAT_ID variable must be set')

const chat = /^-?\d+$/.test(chatRaw) ? Number(chatRaw) : chatRaw
const apks = (await readdir(apkDir)).filter(f => f.endsWith('.apk')).sort()
if (apks.length === 0) throw new Error(`no APK found in ${apkDir}`)

const tg = new TelegramClient({ apiId, apiHash, storage: new MemoryStorage() })

try {
  await tg.start({ botToken })
  for (const file of apks) {
    const abi = file.includes('armeabi-v7a') ? 'armeabi-v7a' : 'arm64-v8a'
    await tg.sendMedia(chat, {
      type: 'document',
      file: `file:${join(apkDir, file)}`,
      fileName: file,
      caption: `LumineGram v${process.env.VER_NAME} (build ${process.env.BUILD_DATE}) ${abi}`,
    })
    console.log(`sent ${file}`)
  }
} finally {
  await tg.destroy()
}
