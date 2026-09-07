<template>
  <el-card v-loading="loading" shadow="never">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="8" :md="6">
        <el-tree
          :data="tree"
          :props="{ label: 'label', children: 'children' }"
          highlight-current
          node-key="key"
          @node-click="onNodeClick"
        />
      </el-col>
      <el-col :xs="24" :sm="16" :md="18">
        <pre v-if="content" class="code">{{ content }}</pre>
        <el-empty v-else :description="$t('source.empty')" />
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'SourceManager' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { fetchLibraries, fetchMember, fetchMembers, fetchSourceFiles } from '@/api/source'

const { t } = useI18n()

interface TreeNode {
  key: string
  label: string
  type: 'library' | 'file' | 'member'
  children?: TreeNode[]
}

const tree = ref<TreeNode[]>([])
const content = ref('')
const loading = ref(false)

const onNodeClick = async (node: TreeNode) => {
  if (node.type === 'member') {
    try {
      const parts = node.key.split('/')
      const data: Record<string, unknown> = await fetchMember(parts[0], parts[1], parts[2])
      content.value = String(data.content ?? '')
    } catch {
      ElMessage.error(t('common.loadFailed'))
    }
  }
}

const load = async () => {
  loading.value = true
  try {
    const libraries = (await fetchLibraries()) as string[]
    // 库级并行；每库内文件级再并行（Promise.all 保序，树结构与串行版一致）
    tree.value = await Promise.all(
      libraries.map(async (lib) => {
        const files = (await fetchSourceFiles(lib)) as string[]
        const fileNodes: TreeNode[] = await Promise.all(
          files.map(async (file) => {
            const members = (await fetchMembers(lib, file)) as string[]
            return {
              key: `${lib}/${file}`,
              label: file,
              type: 'file' as const,
              children: members.map((member) => ({
                key: `${lib}/${file}/${member}`,
                label: member,
                type: 'member' as const,
              })),
            }
          }),
        )
        return { key: lib, label: lib, type: 'library' as const, children: fileNodes }
      }),
    )
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* mb12 已收敛至 src/styles/common.css */
.code {
  margin-top: 12px;
  background: var(--bg-page);
  color: var(--text-primary);
  padding: 16px;
  border-radius: 8px;
  overflow: auto;
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 560px;
}
</style>
