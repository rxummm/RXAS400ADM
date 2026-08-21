/**
 * 前端密码策略 —— 与后端 PasswordPolicy 保持一致（P2-3 单一规则源）。
 * 后端：长度 ≥ 8 且同时包含字母与数字（common/security/PasswordPolicy.java）
 * 前端：改密 / 建户表单共用此工具，避免两处内联正则漂移。
 */
export const MIN_PASSWORD_LENGTH = 8

export interface PasswordCheckResult {
  valid: boolean
  reason: 'ok' | 'tooShort' | 'missingLetter' | 'missingDigit'
}

/** 校验密码复杂度；valid=false 时 reason 给出首个失败原因 */
export function checkPasswordStrength(password: string): PasswordCheckResult {
  if (!password || password.length < MIN_PASSWORD_LENGTH) {
    return { valid: false, reason: 'tooShort' }
  }
  if (!/[A-Za-z]/.test(password)) {
    return { valid: false, reason: 'missingLetter' }
  }
  if (!/[0-9]/.test(password)) {
    return { valid: false, reason: 'missingDigit' }
  }
  return { valid: true, reason: 'ok' }
}

/** 表单校验便捷函数：返回错误消息 key（i18n），合法返回空串 */
export function passwordRuleMessage(password: string, t: (key: string) => string): string {
  const r = checkPasswordStrength(password)
  if (r.valid) return ''
  const keys: Record<PasswordCheckResult['reason'], string> = {
    ok: '',
    tooShort: 'profile.newLength',
    missingLetter: 'profile.newComplexity',
    missingDigit: 'profile.newComplexity',
  }
  return t(keys[r.reason])
}
