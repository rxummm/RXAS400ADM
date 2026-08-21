<template>
  <el-pagination
    class="app-pagination"
    :layout="layout"
    :total="total"
    :page-size="size"
    :page-sizes="pageSizes"
    v-model:current-page="current"
    @current-change="emit('change')"
    @size-change="onSizeChange"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 统一分页：全站页脚分页风格一致（右对齐）。
 * - 默认即含每页条数选择 + 快速跳转：total, sizes, prev, pager, next, jumper
 * - showSizes=false 可回退为简洁模式：total, prev, pager, next
 * - size 支持 v-model 双向绑定，切换条数自动回到第一页
 * 用法：<AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" />
 *      <AppPagination :total="total" :show-sizes="false" v-model:current="current" @change="load" />
 */
const props = withDefaults(
  defineProps<{
    total: number
    showSizes?: boolean
    pageSizes?: number[]
  }>(),
  {
    showSizes: true,
    pageSizes: () => [10, 20, 50],
  },
)

const current = defineModel<number>('current', { required: true })
const size = defineModel<number>('size', { default: 10 })

const emit = defineEmits<{ change: []; sizeChange: [size: number] }>()

const layout = computed(() =>
  props.showSizes ? 'total, sizes, prev, pager, next, jumper' : 'total, prev, pager, next',
)

const onSizeChange = (newSize: number) => {
  size.value = newSize
  current.value = 1
  emit('sizeChange', newSize)
}
</script>

<style scoped>
.app-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
