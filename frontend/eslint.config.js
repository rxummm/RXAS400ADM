// ESLint 9 flat config（M3：接入前端工程化）
// 存量代码存在大量历史问题（142 处 any、未用变量等），本轮以 warning 级别接入：
// 清零 warning 后再把关键规则升为 error 并挂到 build。
import js from '@eslint/js'
import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import vueParser from 'vue-eslint-parser'

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**', 'vite.config.*', 'eslint.config.js'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  ...tseslint.configs.recommended,
  {
    files: ['scripts/**/*.mjs'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      globals: { ...globals.browser },
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser,
        sourceType: 'module',
      },
    },
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      globals: { ...globals.browser },
      parser: tseslint.parser,
      sourceType: 'module',
    },
  },
  {
    rules: {
      // 已清零 warning（2026-08-15 轮次 6），语义规则升为 error 防止回归
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          // 解构剔除字段约定：const { id: _id, ...payload } = form（服务端字段不入请求体）
          varsIgnorePattern: '^_',
          argsIgnorePattern: '^_',
        },
      ],
      '@typescript-eslint/no-empty-object-type': 'off',
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'error',
      'no-unused-vars': 'off', // 由 @typescript-eslint/no-unused-vars 覆盖

      // C1：内联 style 门禁（防止回归，AGENTS.md「硬编码红线」）
      'vue/no-static-inline-styles': 'error',

      // 纯格式化噪音（flat/recommended 默认单属性换行，与既有紧凑写法冲突）：
      // 关闭后由 prettier/IDE 格式化兜底，保持模板可读性即可，不强制风格
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-indent': 'off',
      'vue/attributes-order': 'off',
      'vue/html-self-closing': 'off',
      'vue/first-attribute-linebreak': 'off',
      'vue/html-closing-bracket-newline': 'off',
    },
  },
)