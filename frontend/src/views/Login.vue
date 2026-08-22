<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1 class="brand">{{ $t('login.title') }}</h1>
      <p class="subtitle">{{ $t('login.subtitle') }}</p>

      <el-radio-group v-model="mode" size="small" class="mode-switch">
        <el-radio-button value="platform">{{ $t('login.modePlatform') }}</el-radio-button>
        <el-radio-button value="as400">{{ $t('login.modeAs400') }}</el-radio-button>
      </el-radio-group>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        class="mt16"
        @keyup.enter="onSubmit"
      >
        <el-form-item v-if="mode === 'as400'" prop="serverId">
          <el-select
            v-model="form.serverId"
            :placeholder="$t('login.selectServer')"
            class="w-full"
            @focus="loadServers"
          >
            <el-option
              v-for="s in servers"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="$t('login.username')" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="$t('login.password')"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">
          {{ $t('login.submit') }}
        </el-button>
      </el-form>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
        class="tip"
      />
      <p class="tip">{{ $t('login.demo') }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { AxiosError } from 'axios'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'

const router = useRouter()
const userStore = useUserStore()
const as400ServerStore = useAs400ServerStore()
const { t } = useI18n()
const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')
const mode = ref<'platform' | 'as400'>('platform')
const servers = ref<As400Server[]>([])

const form = reactive({
  username: 'admin',
  password: 'admin123',
  serverId: 0 as number,
})
const rules: FormRules = {
  serverId: [{ required: true, message: () => t('login.selectServer'), trigger: 'change' }],
  username: [{ required: true, message: () => t('login.username'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.password'), trigger: 'blur' }],
}

const loadServers = async () => {
  try {
    // 统一走 store action（F8）：登录前用免 token 的公开接口 /as400/servers/enabled
    servers.value = (await as400ServerStore.fetchEnabledServers()) as As400Server[]
    // 参照旧项目 getDefaultServerId：优先默认服务器，否则第一个
    if (!form.serverId && servers.value.length > 0) {
      const def = servers.value.find((s) => s.defaultServer) || servers.value[0]
      form.serverId = def.id
    }
  } catch {
    servers.value = []
  }
}

const onSubmit = async () => {
  await formRef.value?.validate()
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'as400') {
      await userStore.as400Login(form.serverId, form.username, form.password)
      as400ServerStore.setCurrentServer(form.serverId)
    } else {
      await userStore.login(form.username, form.password)
    }
    ElMessage.success(t('login.success'))
    router.push('/')
  } catch (e: unknown) {
    const err = e as AxiosError<{ message?: string }>
    // P3：失败回退用明确的登录失败文案（原 "加载中…" 语义错误）
    error.value = err.response?.data?.message || t('login.failed')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--login-bg);
}
.login-card {
  width: 400px;
  padding: 12px 8px;
}
.brand {
  text-align: center;
  margin: 8px 0 0;
  font-size: 28px;
}
.subtitle {
  text-align: center;
  color: var(--text-secondary);
  margin: 4px 0 16px;
  font-size: 13px;
}
.mode-switch {
  display: flex;
  justify-content: center;
  width: 100%;
}
.submit {
  width: 100%;
}
.tip {
  margin-top: 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>