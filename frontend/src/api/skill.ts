import { SKILLS, SKILL_CATEGORIES } from '@/mock/skills'
import type { SkillCategory, SkillDetail, SkillItem, SkillQuery } from './types'

/**
 * Skill 商城的数据访问层。
 *
 * <p>目前读的是 `src/mock/skills.ts`。后端有了 Skill 表与接口之后，
 * 只需要把下面几个函数体换成 `api.get(...)`，视图层一行都不用改——
 * 这也是把这些函数写成 async 的原因。</p>
 */

/** 与内容库的 `ContentAccessService.decide()` 保持同一种口径：锁住时只说原因，不给内容 */
const LOCK_REASON = '这个 Skill 需要会员解锁，解锁后可查看完整的功能说明、快速上手步骤与安装命令。'

/** 详情裁成列表项，避免把正文带进列表响应 */
function toItem(skill: SkillDetail): SkillItem {
  return {
    id: skill.id,
    name: skill.name,
    icon: skill.icon,
    categoryId: skill.categoryId,
    summary: skill.summary,
    author: skill.author,
    repo: skill.repo,
    stars: skill.stars,
    version: skill.version,
    accessType: skill.accessType,
    platforms: skill.platforms,
    tags: skill.tags,
    updatedAt: skill.updatedAt,
  }
}

export async function listSkillCategories(): Promise<SkillCategory[]> {
  return SKILL_CATEGORIES
}

/**
 * 按分类 / 关键词筛选，并按指定字段排序。
 *
 * <p>默认按 GitHub 星数倒序——商城的核心排序依据。</p>
 */
export async function pageSkills(
  query: SkillQuery = {}
): Promise<{ list: SkillItem[]; total: number }> {
  const { categoryId, keyword, sort = 'stars' } = query
  let list = SKILLS.slice()

  if (categoryId && categoryId !== 'all') {
    list = list.filter((s) => s.categoryId === categoryId)
  }

  const k = (keyword || '').trim().toLowerCase()
  if (k) {
    list = list.filter((s) =>
      [s.name, s.summary, s.author, s.repo, ...s.tags].join(' ').toLowerCase().includes(k)
    )
  }

  list.sort((a, b) => {
    if (sort === 'name') return a.name.localeCompare(b.name)
    if (sort === 'updated') return b.updatedAt.localeCompare(a.updatedAt)
    // 星数相同时用名称兜底，保证顺序稳定
    return b.stars - a.stars || a.name.localeCompare(b.name)
  })

  return { list: list.map(toItem), total: list.length }
}

/**
 * 取 Skill 详情。
 *
 * <p>`isMember` 由调用方（会员 store）提供。付费 Skill 对非会员只下发
 * 功能特点与收录说明这类介绍性内容，安装命令与快速上手步骤不下发——
 * 和内容库「未解锁只给试读片段」是同一套做法。</p>
 */
export async function getSkill(id: string, isMember: boolean): Promise<SkillDetail> {
  const skill = SKILLS.find((s) => s.id === id)
  if (!skill) {
    throw new Error('这个 Skill 不存在或已下架')
  }

  const locked = skill.accessType === 'MEMBER' && !isMember
  const detail: SkillDetail = { ...skill, locked, lockReason: locked ? LOCK_REASON : null }

  if (locked) {
    detail.installCommand = ''
    detail.quickStart = []
  }

  return detail
}

/** 星数展示：过万折成 12.4k */
export function formatStars(stars: number): string {
  if (stars >= 1000) {
    return `${(stars / 1000).toFixed(1)}k`
  }
  return String(stars)
}
