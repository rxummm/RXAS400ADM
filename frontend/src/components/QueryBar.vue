<template>
  <div class="search-bar">
    <!-- 默认插槽：关键词左侧的筛选控件（库/文件/状态等） -->
    <slot />
    <el-input
      v-model="model"
      :placeholder="placeholder"
      clearable
      :style="keywordWidth ? { width: keywordWidth + 'px' } : undefined"
      @keyup.enter="onForceSearch"
      @clear="onForceSearch"
    />
    <el-button type="primary" :icon="Search" @click="onForceSearch">{{ $t('common.search') }}</el-button>
    <!-- 重置按钮（替代原刷新按钮位置） -->
    <el-button @click="emit('reset')">{{ $t('common.reset') }}</el-button>
    <!-- 数据来源提示：tag 绝对定位悬浮，不参与布局、不挤动其他按钮 -->
    <span class="qb-refresh-wrap">
      <transition name="qb-pop">
        <el-tag
          v-if="fromCache || flashing"
          :key="flashTick ?? 0"
          size="small"
          :type="fromCache ? 'info' : 'success'"
          class="querybar-source"
          :class="{ 'flash-pop': flashing }"
        >
          {{ fromCache ? $t('common.fromCache') : $t('common.refreshDone') }}
        </el-tag>
      </transition>
    </span>
    <!-- 前端过滤为空时的引导性提示 -->
    <el-tag v-if="localFilterEmpty" type="warning" size="small" class="qb-local-empty">
      {{ $t('common.localFilterEmpty') }}
    </el-tag>
    <!-- 弹性空间 -->
    <div class="flex-1" />
    <!-- right 插槽：操作按钮（新增、导出等） -->
    <slot name="right" />
    <!-- 刷新图标：纯图标按钮，与查询按钮同源触发 forceSearch，大数据量页面显示 -->
    <el-button
      v-if="showRefresh"
      :icon="Refresh"
      circle
      :title="$t('common.refresh')"
      @click="onForceSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useFlash } from '@/composables/useFlash'

const props = withDefaults(
  defineProps<{
    /** 关键词（v-model），驱动 useTablePage.keyword 实时前端匹配 */
    keyword: string
    /** 关键词输入框 placeholder */
    placeholder?: string
    /** 是否显示「来自缓存」tag（useTablePage.isFromCache） */
    fromCache?: boolean
    /** 关键词输入框宽度（px），默认 200 */
    keywordWidth?: number
    /** 数据源变更信号（useTablePage.dataSourceTick）：每次 fetch 成功/缓存命中自增，触发闪烁 */
    flashTick?: number
    /** 是否显示右侧刷新图标（大数据量页面建议显示） */
    showRefresh?: boolean
    /** 前端过滤是否为空结果，用于显示引导提示 */
    localFilterEmpty?: boolean
  }>(),
  {
    placeholder: '',
    fromCache: false,
    keywordWidth: 200,
    flashTick: undefined,
    showRefresh: true,
    localFilterEmpty: false,
  },
)

const emit = defineEmits<{
  (e: 'update:keyword', value: string): void
  /** 强制后端查询（查询按钮/回车/刷新图标 同源） */
  (e: 'forceSearch'): void
  /** 重置输入条件 */
  (e: 'reset'): void
}>()

const model = computed({
  get: () => props.keyword,
  set: (v: string) => emit('update:keyword', v),
})

function onForceSearch() {
  emit('forceSearch')
}

/** 数据来源闪烁：flashTick 每次变化（跳过首次挂载 tick≤1）触发，1.4s 后熄灭 */
const { flashing, trigger: flash } = useFlash()

watch(
  () => props.flashTick,
  (val) => {
    if (val == null || val <= 1) return
    flash()
  },
)
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.flex-1 {
  flex: 1;
}
/* 刷新按钮相对定位容器，数据来源 tag 以它为锚点悬浮在右侧 */
.qb-refresh-wrap {
  position: relative;
  display: inline-flex;
}
.querybar-source {
  position: absolute;
  left: 0;
  top: -28px;
  white-space: nowrap;
  z-index: 20;
}
.qb-local-empty {
  white-space: nowrap;
}
/* 弹出/收起过渡：绝对定位不参与布局，不会挤动右侧按钮 */
.qb-pop-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}
.qb-pop-leave-active {
  transition: opacity 0.2s ease;
}
.qb-pop-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}
.qb-pop-leave-to {
  opacity: 0;
}
</style>