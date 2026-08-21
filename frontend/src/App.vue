<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useNetworkStatus } from '@/composables/useNetworkStatus'

const { locale } = useI18n()
useNetworkStatus()
// P2-27：Element Plus 内置文案随 i18n 语言实时切换（此前在 main.ts 启动时冻结，切语言后 EP 组件仍是旧语言）
const elLocale = computed(() => (locale.value === 'en-US' ? en : zhCn))
</script>
