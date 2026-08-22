<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="page-container page-container--fit">
    <div class="table-wrapper">
      <el-result icon="warning" :title="$t('common.notFound')" sub-title="404 - Page Not Found">
        <template #extra>
          <el-button type="primary" @click="router.push('/dashboard')">
            {{ $t('common.backToHome') }}
          </el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>
