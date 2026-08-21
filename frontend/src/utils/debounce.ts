/** 简单防抖（P2-32）：timeout 内连续调用只执行最后一次，用于下拉/输入 @change 直发查询的场景 */
export function debounce<A extends unknown[]>(fn: (...args: A) => void, delay = 300) {
  let timer: ReturnType<typeof setTimeout> | undefined
  return (...args: A) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}
