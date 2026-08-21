<template>
  <el-card shadow="never">
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
        <el-input
          v-model="compileForm.member"
          :placeholder="$t('source.empty')"
          readonly
          class="mb12"
        />
        <el-button
          type="primary"
          :disabled="!compileForm.member"
          :loading="compiling"
          @click="compile"
        >
          {{ $t('source.compile') }}
        </el-button>
        <pre v-if="content" class="code">{{ content }}</pre>
        <el-empty v-else :description="$t('source.empty')" />
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { fetchLibraries, fetchMember, fetchMembers, fetchSourceFiles } from '@/api/source'
import { compileMember } from '@/api/compile'

interface TreeNode {
  key: string
  label: string
  type: 'library' | 'file' | 'member'
  children?: TreeNode[]
}

const { t } = useI18n()
const tree = ref<TreeNode[]>([])
const content = ref('')
const compiling = ref(false)

const compileForm = reactive({ member: '' })

const onNodeClick = async (node: TreeNode) => {
  if (node.type === 'member') {
    const parts = node.key.split('/')
    const data: Record<string, unknown> = await fetchMember(parts[0], parts[1], parts[2])
    content.value = String(data.content ?? '')
    compileForm.member = parts[2]
  }
}

const compile = async () => {
  compiling.value = true
  try {
    const target = tree.value
      .flatMap((l) => (l.children || []).map((f) => ({ library: l.label, file: f })))
      .flatMap(({ library, file }) =>
        (file.children || []).map((m) => ({ library, sourceFile: file.label, member: m.label })),
      )
      .find((t) => t.member === compileForm.member)
    if (!target) return
    const record: Record<string, unknown> = await compileMember({ ...target, command: 'CRTBNDRPG' })
    ElMessage.success(
      record.status === 'SUCCESS' ? t('source.compileSuccess') : `${t('source.compileFailed')}: ${record.message}`,
    )
  } finally {
    compiling.value = false
  }
}

const load = async () => {
  const libraries = (await fetchLibraries()) as string[]
  tree.value = []
  for (const lib of libraries) {
    const files = (await fetchSourceFiles(lib)) as string[]
    const fileNodes: TreeNode[] = []
    for (const file of files) {
      const members = (await fetchMembers(lib, file)) as string[]
      fileNodes.push({
        key: `${lib}/${file}`,
        label: file,
        type: 'file',
        children: members.map((member) => ({
          key: `${lib}/${file}/${member}`,
          label: member,
          type: 'member',
        })),
      })
    }
    tree.value.push({ key: lib, label: lib, type: 'library', children: fileNodes })
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