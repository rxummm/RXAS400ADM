/**
 * 可取消防抖工具
 *
 * 与 debounce.ts 互补，不替换。用于需要从外部主动清除定时器的场景：
 * - 用户输入 → 启动防抖（前端过滤）
 * - 用户点击查询/回车 → cancel() 清除防抖 + 直接走后端
 * - 防止前端过滤结果覆盖后端返回的最新数据
 */
export function createCancellableDebounce(delay = 500) {
  let timer: ReturnType<typeof setTimeout> | undefined

  const schedule = (fn: () => void) => {
    cancel()
    timer = setTimeout(fn, delay)
  }

  const cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = undefined
    }
  }

  const flush = (fn: () => void) => {
    cancel()
    fn()
  }

  return { schedule, cancel, flush }
}