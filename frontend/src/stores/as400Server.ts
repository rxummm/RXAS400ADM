import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchSystems, fetchEnabledServers as fetchEnabledServersApi, type IbmiSystem } from '@/api/as400'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'

// P3-2：统一走 STORAGE_KEYS.AS400_SERVER（blobClient 下载等复用同一 key 注入 X-AS400-Server）
const currentServerStorage = useStorage(STORAGE_KEYS.AS400_SERVER)

/** 与资产清单/登录下拉共用的服务器类型（rx_ibmi_system） */
export type As400Server = IbmiSystem

export const useAs400ServerStore = defineStore('as400Server', () => {
  const currentServerId = ref<number>(Number(currentServerStorage.get()) || 0)
  const serverList = ref<As400Server[]>([])
  /** 已成功拉取过服务器列表（避免 7+ 个页面 onMounted 并发重复请求 /as400/servers） */
  const loaded = ref(false)
  /** 共享进行中的请求：并发调用复用同一次网络请求 */
  let inflight: Promise<As400Server[]> | null = null

  const currentServer = computed(() =>
    serverList.value.find((s) => s.id === currentServerId.value) || null,
  )

  async function fetchServers() {
    if (loaded.value) {
      return serverList.value
    }
    if (!inflight) {
      // noDedupe：Dashboard 等页面并发直连 /as400/systems 时，避免本请求被去重逻辑 abort（P2-23）
      inflight = fetchSystems(true)
        .then((data) => {
          serverList.value = data.filter((s) => s.enabled !== false)
          loaded.value = true
          return serverList.value
        })
        .finally(() => {
          inflight = null
        })
    }
    const list = await inflight
    if (!currentServer.value) {
      const def = list.find((s) => s.defaultServer) || list[0]
      if (def) setCurrentServer(def.id)
    }
    return serverList.value
  }

  /** 强制刷新（服务器配置变更后调用，重新拉取并重置缓存） */
  async function refreshServers() {
    loaded.value = false
    return fetchServers()
  }

  /**
   * 登录前公开拉取（免 token 的 /as400/servers/enabled）。
   * 与 fetchServers 共用 serverList/loaded 缓存，统一走 store action（F8：避免 Login 直连 API 手动拼装）。
   */
  async function fetchEnabledServers() {
    if (loaded.value) {
      return serverList.value
    }
    if (!inflight) {
      inflight = fetchEnabledServersApi()
        .then((data) => {
          serverList.value = data.filter((s) => s.enabled !== false)
          loaded.value = true
          return serverList.value
        })
        .finally(() => {
          inflight = null
        })
    }
    const list = await inflight
    if (!currentServer.value) {
      const def = list.find((s) => s.defaultServer) || list[0]
      if (def) setCurrentServer(def.id)
    }
    return serverList.value
  }

  function setCurrentServer(id: number) {
    currentServerId.value = id
    currentServerStorage.set(String(id))
  }

  /** P3-2：登出时重置服务器选择，避免下次登录沿用上一会话的选择 */
  function reset() {
    currentServerId.value = 0
    currentServerStorage.remove()
    loaded.value = false
  }

  return { currentServerId, serverList, currentServer, fetchServers, fetchEnabledServers, refreshServers, setCurrentServer, reset }
})
