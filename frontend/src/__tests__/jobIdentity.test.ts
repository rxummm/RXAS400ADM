import { describe, expect, it } from 'vitest'
import { normalizeJobIdentity } from '@/utils/jobIdentity'

describe('normalizeJobIdentity', () => {
  it('作业表行（小写字段）原样归一化', () => {
    expect(normalizeJobIdentity({ jobName: 'JOB1', jobUser: 'USER', jobNumber: '123456' })).toEqual({
      jobName: 'JOB1',
      jobUser: 'USER',
      jobNumber: '123456',
    })
  })

  it('MSGW 抽屉行（大写字段）归一化为小写', () => {
    expect(normalizeJobIdentity({ JOB_NAME: 'JOB1', JOB_USER: 'USER', JOB_NUMBER: '123456' })).toEqual({
      jobName: 'JOB1',
      jobUser: 'USER',
      jobNumber: '123456',
    })
  })

  it('字段缺失时回退空串，小写优先', () => {
    expect(normalizeJobIdentity({})).toEqual({ jobName: '', jobUser: '', jobNumber: '' })
  })

  it('undefined 与混合写法安全处理', () => {
    const row = { jobName: undefined, JOB_NAME: 'JOB9' } as Record<string, unknown>
    expect(normalizeJobIdentity(row)).toEqual({ jobName: 'JOB9', jobUser: '', jobNumber: '' })
  })
})
