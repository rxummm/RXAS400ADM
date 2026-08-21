import { describe, it, expect } from 'vitest'
import { checkPasswordStrength, passwordRuleMessage, MIN_PASSWORD_LENGTH } from '@/utils/passwordPolicy'

describe('passwordPolicy（与后端 PasswordPolicy 对齐：≥8 位 + 字母 + 数字）', () => {
  it('合法密码：8 位以上且含字母和数字', () => {
    expect(checkPasswordStrength('abc12345')).toEqual({ valid: true, reason: 'ok' })
    expect(checkPasswordStrength('Admin@2026')).toEqual({ valid: true, reason: 'ok' })
    expect(checkPasswordStrength('a1'.repeat(10))).toEqual({ valid: true, reason: 'ok' })
  })

  it('过短（<8）被拒', () => {
    expect(checkPasswordStrength('a1')).toEqual({ valid: false, reason: 'tooShort' })
    expect(checkPasswordStrength('abc1234')).toEqual({ valid: false, reason: 'tooShort' })
  })

  it('纯字母被拒（缺数字）', () => {
    expect(checkPasswordStrength('abcdefgh')).toEqual({ valid: false, reason: 'missingDigit' })
  })

  it('纯数字被拒（缺字母）', () => {
    expect(checkPasswordStrength('12345678')).toEqual({ valid: false, reason: 'missingLetter' })
  })

  it('空串/边界被拒', () => {
    expect(checkPasswordStrength('')).toEqual({ valid: false, reason: 'tooShort' })
    expect(checkPasswordStrength('a'.repeat(MIN_PASSWORD_LENGTH - 1))).toEqual({ valid: false, reason: 'tooShort' })
  })

  it('passwordRuleMessage 返回 i18n key（合法返回空串）', () => {
    const t = (k: string) => `[${k}]`
    expect(passwordRuleMessage('abc12345', t)).toBe('')
    expect(passwordRuleMessage('short', t)).toBe('[profile.newLength]')
    expect(passwordRuleMessage('abcdefgh', t)).toBe('[profile.newComplexity]')
  })
})
