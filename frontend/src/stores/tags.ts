import { defineStore } from 'pinia'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'

export interface TagView {
  path: string
  title: string
  icon?: string
  affix?: boolean
  /** 缓存组件名（对应路由 name / 页面 defineOptions name），用于 keep-alive 缓存 */
  cacheName?: string
}

const tagsStore = useStorage(STORAGE_KEYS.TAGS)

/** 固定标签：始终存在、不可关闭（参照旧项目 layout onMounted 无条件播种总览） */
const AFFIX_VIEWS: TagView[] = [{ path: '/dashboard', title: 'menu.dashboard', affix: true }]

/**
 * 多标签页状态（参照旧项目 tags store 精简）：
 * - affix 固定标签（如总览）由 AFFIX_VIEWS 无条件重建，始终排最前、不可关闭，刷新不丢失
 * - 业务标签持久化到 localStorage，刷新后恢复（affix 除外）
 * - cachedViews：keep-alive 缓存名单，与打开的标签同步（关闭标签即移除缓存）
 * - refreshKeys 用于强制重建组件（右键「刷新」）
 */
export const useTagsStore = defineStore('tags', {
  state: () => {
    const saved: TagView[] = []
    const raw = tagsStore.getJson<TagView[]>([])
    if (Array.isArray(raw)) {
      raw.forEach((v) => {
        // 固定标签由 AFFIX_VIEWS 无条件重建；残留的旧版无 affix 总览一并丢弃，避免它变成可关闭的普通标签
        if (v && v.path && !v.affix && v.path !== '/dashboard') {
          saved.push({ path: v.path, title: v.title || '', icon: v.icon, cacheName: v.cacheName })
        }
      })
    }
    return {
      visitedViews: [...AFFIX_VIEWS.map((v) => ({ ...v })), ...saved] as TagView[],
      cachedViews: (saved.filter((v) => v.cacheName).map((v) => v.cacheName as string)) as string[],
      refreshKeys: {} as Record<string, number>,
    }
  },

  getters: {},

  actions: {
    persist() {
      tagsStore.setJson(this.visitedViews)
    },

    /** 添加标签；affix 标签插入到现有 affix 之后，普通标签追加 */
    addView(view: TagView) {
      const existing = this.visitedViews.find((v) => v.path === view.path)
      if (existing) {
        // 已在打开列表中：补齐 icon / cacheName；若新标签声明 affix（如总览）则升级为固定，防历史数据残留非固定
        let changed = false
        if (view.affix && !existing.affix) {
          existing.affix = true
          changed = true
        }
        if (view.icon && !existing.icon) {
          existing.icon = view.icon
          changed = true
        }
        if (view.cacheName && !existing.cacheName) {
          existing.cacheName = view.cacheName
          changed = true
        }
        if (changed) {
          this.syncCache()
          this.persist()
        }
        return
      }
      const newView = { ...view }
      if (newView.affix) {
        let insertIndex = 0
        for (let i = this.visitedViews.length - 1; i >= 0; i--) {
          if (this.visitedViews[i].affix) {
            insertIndex = i + 1
            break
          }
        }
        this.visitedViews.splice(insertIndex, 0, newView)
      } else {
        this.visitedViews.push(newView)
      }
      this.syncCache()
      this.persist()
    },

    /** cachedViews 与 visitedViews 中带 cacheName 的标签同步（keep-alive include 名单） */
    syncCache() {
      this.cachedViews = this.visitedViews
        .filter((v) => v.cacheName)
        .map((v) => v.cacheName as string)
    },

    removeView(view: TagView) {
      const index = this.visitedViews.findIndex((v) => v.path === view.path)
      if (index === -1) return
      // 固定标签不可关闭（UI 已隐藏关闭按钮，这里兜底防止误删/程序误调）
      if (this.visitedViews[index].affix) return
      this.visitedViews.splice(index, 1)
      this.syncCache()
      this.persist()
    },

    /** 关闭标签；若关闭的是当前激活标签，返回下一个应跳转的路径（固定标签返回 null） */
    closeView(view: TagView): string | null {
      const index = this.visitedViews.findIndex((v) => v.path === view.path)
      if (index === -1 || this.visitedViews[index]?.affix) return null
      this.removeView(view)
      const remaining = this.visitedViews
      if (remaining.length > 0) {
        const nextIndex = Math.min(index, remaining.length - 1)
        return remaining[nextIndex].path
      }
      return '/dashboard'
    },

    removeOtherViews(view: TagView) {
      this.visitedViews = this.visitedViews.filter((v) => v.path === view.path || v.affix)
      this.syncCache()
      this.persist()
    },

    removeAllViews() {
      this.visitedViews = this.visitedViews.filter((v) => v.affix)
      this.syncCache()
      this.persist()
    },

    /** 刷新标签：递增 key 使 router-view 重建组件（重新走 onMounted 拉数据） */
    refreshView(view: TagView) {
      const key = view.path
      this.refreshKeys[key] = (this.refreshKeys[key] || 0) + 1
    },
  },
})
