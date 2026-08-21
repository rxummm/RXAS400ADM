<template>
  <MdEditor
    v-model="inner"
    :language="editorLang"
    :placeholder="placeholder"
    :style="{ height: `${height}px` }"
    class="markdown-editor"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { useI18n } from 'vue-i18n'

const props = withDefaults(
  defineProps<{
    modelValue: string
    height?: number
    placeholder?: string
  }>(),
  { height: 360, placeholder: '' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const { locale } = useI18n()

const editorLang = computed(() => (locale.value === 'en-US' ? 'en-US' : 'zh-CN'))

const inner = ref(props.modelValue)

watch(
  () => props.modelValue,
  (v) => {
    if (v !== inner.value) inner.value = v
  },
)

watch(inner, (v) => emit('update:modelValue', v))
</script>

<style scoped>
.markdown-editor {
  border: 1px solid var(--border-color);
  border-radius: 4px;
  overflow: hidden;
}
</style>