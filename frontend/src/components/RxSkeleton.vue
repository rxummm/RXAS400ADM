<template>
  <transition name="rx-skeleton-fade" mode="out-in">
    <div v-if="loading" :key="'skeleton'" :class="['rx-skeleton-wrap', `rx-skeleton--${type}`]">
      <template v-if="type === 'table'">
        <div class="rx-skeleton-table">
          <div
            v-for="i in rows"
            :key="i"
            class="rx-skeleton rx-skeleton-row"
            :class="{ 'rx-skeleton-row--short': i % 2 === 0 }"
          />
        </div>
      </template>
      <template v-else-if="type === 'card'">
        <div class="rx-skeleton-card">
          <div class="rx-skeleton rx-skeleton-row rx-skeleton-row--title" />
          <div class="rx-skeleton rx-skeleton-row" />
          <div class="rx-skeleton rx-skeleton-row rx-skeleton-row--short" />
        </div>
      </template>
      <template v-else-if="type === 'list'">
        <div class="rx-skeleton-list">
          <div v-for="i in rows" :key="i" class="rx-skeleton-list-item">
            <div class="rx-skeleton rx-skeleton-avatar" />
            <div class="rx-skeleton-list-content">
              <div class="rx-skeleton rx-skeleton-row rx-skeleton-row--title" />
              <div class="rx-skeleton rx-skeleton-row rx-skeleton-row--short" />
            </div>
          </div>
        </div>
      </template>
    </div>
    <div v-else :key="'content'">
      <slot />
    </div>
  </transition>
</template>

<script setup lang="ts">
/**
 * 骨架屏加载占位组件
 *
 * @example（表格骨架）
 * <RxSkeleton type="table" :rows="5" :loading="loading">
 *   <el-table :data="rows">...</el-table>
 * </RxSkeleton>
 *
 * @example（卡片骨架）
 * <RxSkeleton type="card" :loading="loading">
 *   <el-card>...</el-card>
 * </RxSkeleton>
 */
withDefaults(
  defineProps<{
    /** 骨架屏类型 */
    type?: 'table' | 'card' | 'list'
    /** 骨架行数（默认 5） */
    rows?: number
    /** 是否加载中 */
    loading?: boolean
  }>(),
  {
    type: 'table',
    rows: 5,
    loading: false,
  },
)
</script>

<style scoped>
.rx-skeleton-wrap {
  padding: 8px 0;
}
.rx-skeleton-table {
  padding: 12px;
  background: var(--bg-container);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.rx-skeleton-card {
  padding: 16px;
  background: var(--bg-container);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.rx-skeleton-list {
  padding: 4px 0;
}
.rx-skeleton-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}
.rx-skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}
.rx-skeleton-list-content {
  flex: 1;
  min-width: 0;
}
.rx-skeleton-row--short {
  width: 60% !important;
}
.rx-skeleton-row--title {
  height: 18px !important;
  margin-bottom: 10px !important;
  width: 40% !important;
}
.rx-skeleton-row--title {
  height: 18px !important;
  margin-bottom: 10px !important;
  width: 40% !important;
}
/* 骨架屏→内容淡入淡出过渡 */
.rx-skeleton-fade-enter-active,
.rx-skeleton-fade-leave-active {
  transition: opacity 0.25s ease;
}
.rx-skeleton-fade-enter-from,
.rx-skeleton-fade-leave-to {
  opacity: 0;
}
</style>