import { onUnmounted, ref } from 'vue'

/**
 * 通用闪烁提示：trigger() 后短暂高亮，默认 1.4s 后自动熄灭。
 * 配合 common.css 的 .flash-pop 动画使用，供数据来源提示（QueryBar 缓存命中/刷新）、
 * 标签页刷新、报告生成完成等「数据已更新」反馈复用。
 */
export function useFlash(duration = 1400) {
  const flashing = ref(false)
  let timer: number | undefined

  const trigger = () => {
    flashing.value = true
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      flashing.value = false
    }, duration)
  }

  onUnmounted(() => {
    if (timer) window.clearTimeout(timer)
  })

  return { flashing, trigger }
}
