import { onUnmounted } from 'vue'
import { Client, type IMessage, type StompHeaders, type StompSubscription } from '@stomp/stompjs'
import { useStorage, STORAGE_KEYS } from './useStorage'

/**
 * STOMP WebSocket 客户端复用 composable
 * 解决 Monitor.vue 与 NotificationBell.vue 各自独立创建 Client 的重复代码问题。
 *
 * 用法：
 *   const { connect, disconnect } = useStompClient()
 *   connect((client) => {
 *     client.subscribe('/topic/xxx', (msg) => { ... })
 *   })
 *
 * 特性：
 * - 协议自适应：HTTPS → wss，HTTP → ws
 * - 自动携带 JWT Authorization 头
 * - 5 秒自动重连
 * - P2-26：重连时先退订旧订阅再重新订阅（STOMP.js 重连后 onConnect 会再次触发，
 *   若不清理，订阅 map 会累积重复订阅 → 消息重复推送）
 * - 组件卸载时自动断开
 */
export function useStompClient() {
  let client: Client | null = null
  /** 当前连接的活跃订阅（重连时先退订，防止重复） */
  let subscriptions: StompSubscription[] = []

  const brokerURL = () =>
    `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`

  const headers = () => ({
    Authorization: `Bearer ${useStorage(STORAGE_KEYS.TOKEN).get() || ''}`,
  })

  const connect = (
    onConnected: (client: Client) => void,
    onDisconnect?: () => void,
  ) => {
    disconnect()
    client = new Client({
      brokerURL: brokerURL(),
      connectHeaders: headers(),
      reconnectDelay: 5000,
      onDisconnect: () => {
        onDisconnect?.()
      },
      onConnect: () => {
        const current = client!
        // 重连场景：先退订上次连接的订阅，再执行订阅回调，避免同目的地订阅累积
        for (const sub of subscriptions) {
          try {
            sub.unsubscribe()
          } catch {
            /* 已失效订阅忽略 */
          }
        }
        subscriptions = []
        // 拦截 subscribe 记录订阅引用，供下次重连清理（P2-26）
        const originalSubscribe = current.subscribe.bind(current)
        current.subscribe = ((
          destination: string,
          callback: (message: IMessage) => void,
          subHeaders?: StompHeaders,
        ): StompSubscription => {
          const sub = originalSubscribe(destination, callback, subHeaders)
          subscriptions.push(sub)
          return sub
        }) as typeof current.subscribe
        onConnected(current)
      },
    })
    client.activate()
    return client
  }

  const disconnect = () => {
    for (const sub of subscriptions) {
      try {
        sub.unsubscribe()
      } catch {
        /* ignore */
      }
    }
    subscriptions = []
    client?.deactivate()
    client = null
  }

  onUnmounted(() => {
    disconnect()
  })

  return { connect, disconnect }
}
