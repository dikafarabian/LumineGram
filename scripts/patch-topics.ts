// lumine patches are grouped by topic under patches/lumine/<topic>/. Every lumine__ patch must be listed in patch-topics.json.
import topicsJson from './patch-topics.json'

export const LUMINE_TOPICS = [
  'accounts',
  'ai',
  'build',
  'calls',
  'chat',
  'feed',
  'media',
  'misc',
  'notifications',
  'premium',
  'privacy',
  'proxy',
  'restrictions',
  'settings',
  'tabs',
  'translator',
  'ui',
] as const

export type LumineTopic = typeof LUMINE_TOPICS[number]

const topics = topicsJson as Record<string, string>

export function topicOf(patchName: string): LumineTopic {
  const name = patchName.startsWith('lumine__') ? patchName.slice('lumine__'.length) : patchName
  const topic = topics[name]
  if (!topic) {
    throw new Error(`No topic for ${patchName}; add it to scripts/patch-topics.json`)
  }
  if (!(LUMINE_TOPICS as readonly string[]).includes(topic)) {
    throw new Error(`Unknown topic "${topic}" for ${patchName} in scripts/patch-topics.json`)
  }
  return topic as LumineTopic
}
