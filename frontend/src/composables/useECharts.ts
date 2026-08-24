/**
 * ECharts 生命周期封装（P1-4 抽取自 Monitor/serverCompare/topology/job-dependency 四页重复样板）
 *
 * 封装 init → setOption(baseOption) → resize 监听 → dispose 全流程：
 * - 挂载时若元素存在则初始化并应用基准配置（getBaseOption 在 init 时刻求值一次）
 * - 元素经 v-if 延迟挂载时，首次 setOption 兜底初始化（同样先应用基准配置）
 * - 暴露 setOption 用于增量更新（如 WS 推送追加数据点），可选透传 notMerge
 * - 自带 window resize 联动与卸载清理，调用方无需手写 add/removeEventListener/dispose
 *
 * @example
 *   const cpuChart = ref<HTMLDivElement>()
 *   const cpu = useECharts(cpuChart, () => ({
 *     title: { text: t('monitor.cpu'), left: 'center' },
 *     xAxis: { type: 'category', data: [] },
 *     series: [{ type: 'line', data: [] }],
 *   }))
 *   // 数据到达时：
 *   cpu.setOption({ series: [{ data: cpuData.value }] })
 */
import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import * as echarts from '@/utils/echarts'

type EChartsInstance = ReturnType<typeof echarts.init>
/** setOption 首参类型（跟随 @/utils/echarts 实际导出，避免硬绑 echarts 主包类型） */
export type ECOption = Parameters<EChartsInstance['setOption']>[0]

export function useECharts(elRef: Ref<HTMLDivElement | undefined>, getBaseOption: () => ECOption) {
  let instance: EChartsInstance | null = null

  /** 未初始化且元素已存在时立即 init 并应用基准配置；返回当前实例（可能为 null） */
  const ensureInit = (): EChartsInstance | null => {
    if (!instance && elRef.value) {
      instance = echarts.init(elRef.value)
      instance.setOption(getBaseOption())
    }
    return instance
  }

  /** 增量更新配置（合并语义，同 instance.setOption；notMerge 透传原生第二参） */
  const setOption = (partial: ECOption, notMerge?: boolean) => {
    ensureInit()?.setOption(partial, notMerge)
  }

  /** 底层实例（用于注册事件等高级场景）；未初始化时为 null */
  const getInstance = (): EChartsInstance | null => instance

  const onResize = () => {
    instance?.resize()
  }

  onMounted(ensureInit)
  window.addEventListener('resize', onResize)

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize)
    instance?.dispose()
    instance = null
  })

  return { setOption, resize: onResize, getInstance }
}
