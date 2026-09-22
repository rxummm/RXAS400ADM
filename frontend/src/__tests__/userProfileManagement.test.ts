import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  fetchUserProfileList,
  getUserProfileDetail,
  createUserProfile,
  updateUserProfile,
  deleteUserProfile,
  batchDeleteUserProfiles,
  getDeleteStats,
  type UserProfileListRow,
  type UserProfileDetail,
  type UserProfileCreateDTO,
  type UserProfileUpdateDTO,
  type UserProfileDeleteStats,
  type UserProfileBatchDeleteResult,
} from '@/api/userProfileManagement'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import request from '@/api/request'
const mockedRequest = vi.mocked(request)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('userProfileManagement API', () => {
  describe('fetchUserProfileList', () => {
    it('调用 GET /api/v1/as400/user-profiles 并传分页参数', async () => {
      const mockData = { records: [] as UserProfileListRow[], total: 0 }
      mockedRequest.get.mockResolvedValue(mockData)

      const result = await fetchUserProfileList({ current: 1, size: 20 })

      expect(mockedRequest.get).toHaveBeenCalledWith('/api/v1/as400/user-profiles', {
        params: { current: 1, size: 20 },
      })
      expect(result).toBe(mockData)
    })

    it('支持 keyword 筛选参数', async () => {
      mockedRequest.get.mockResolvedValue({ records: [], total: 0 })

      await fetchUserProfileList({ current: 1, size: 10, keyword: 'admin' })

      expect(mockedRequest.get).toHaveBeenCalledWith('/api/v1/as400/user-profiles', {
        params: { current: 1, size: 10, keyword: 'admin' },
      })
    })
  })

  describe('getUserProfileDetail', () => {
    it('调用 GET /api/v1/as400/user-profiles/{userName}', async () => {
      const mockDetail: UserProfileDetail = {
        userName: 'TESTUSER',
        status: '*ENABLED',
        groupProfile: '*NONE',
        description: 'Test user',
        initialMenu: '*SIGNOFF',
        specialAuthorities: [],
        lastUsedDate: '20260101',
        passwordExpireDate: '20261231',
      }
      mockedRequest.get.mockResolvedValue(mockDetail)

      const result = await getUserProfileDetail('TESTUSER')

      expect(mockedRequest.get).toHaveBeenCalledWith('/api/v1/as400/user-profiles/TESTUSER')
      expect(result).toBe(mockDetail)
    })

    it('userName 包含特殊字符时进行 URI 编码', async () => {
      mockedRequest.get.mockResolvedValue({} as UserProfileDetail)

      await getUserProfileDetail('user@test')

      expect(mockedRequest.get).toHaveBeenCalledWith('/api/v1/as400/user-profiles/user%40test')
    })
  })

  describe('createUserProfile', () => {
    it('调用 POST /api/v1/as400/user-profiles 并传递 DTO', async () => {
      const dto: UserProfileCreateDTO = {
        userName: 'NEWUSER',
        password: 'Pass1234',
        description: 'New user',
        groupProfile: '*NONE',
        initialMenu: '*SIGNOFF',
        specialAuthorities: ['*ALLOBJ'],
      }
      mockedRequest.post.mockResolvedValue({ userName: 'NEWUSER' })

      const result = await createUserProfile(dto)

      expect(mockedRequest.post).toHaveBeenCalledWith('/api/v1/as400/user-profiles', dto)
      expect(result).toEqual({ userName: 'NEWUSER' })
    })
  })

  describe('updateUserProfile', () => {
    it('调用 PUT /api/v1/as400/user-profiles/{userName}', async () => {
      const dto: UserProfileUpdateDTO = {
        description: 'Updated',
        status: '*DISABLED',
      }
      mockedRequest.put.mockResolvedValue(undefined)

      await updateUserProfile('TESTUSER', dto)

      expect(mockedRequest.put).toHaveBeenCalledWith('/api/v1/as400/user-profiles/TESTUSER', dto)
    })
  })

  describe('deleteUserProfile', () => {
    it('调用 DELETE /api/v1/as400/user-profiles/{userName} 并传递查询参数', async () => {
      mockedRequest.delete.mockResolvedValue(undefined)

      await deleteUserProfile('TESTUSER', {
        deleteReason: '离职',
        deletionType: 'RESIGNED',
      })

      expect(mockedRequest.delete).toHaveBeenCalledWith('/api/v1/as400/user-profiles/TESTUSER', {
        params: { deleteReason: '离职', deletionType: 'RESIGNED' },
      })
    })

    it('无参数时也正常调用', async () => {
      mockedRequest.delete.mockResolvedValue(undefined)

      await deleteUserProfile('TESTUSER')

      expect(mockedRequest.delete).toHaveBeenCalledWith('/api/v1/as400/user-profiles/TESTUSER', {
        params: undefined,
      })
    })
  })

  describe('batchDeleteUserProfiles', () => {
    it('调用 POST /api/v1/as400/user-profiles/batch-delete', async () => {
      const dto = {
        userNames: ['USER1', 'USER2'],
        deleteReason: '批量清理',
        deletionType: 'MANUAL',
      }
      const mockResult: UserProfileBatchDeleteResult = {
        total: 2,
        successCount: 2,
        failCount: 0,
        failDetails: '',
      }
      mockedRequest.post.mockResolvedValue(mockResult)

      const result = await batchDeleteUserProfiles(dto)

      expect(mockedRequest.post).toHaveBeenCalledWith('/api/v1/as400/user-profiles/batch-delete', dto)
      expect(result).toBe(mockResult)
    })
  })

  describe('getDeleteStats', () => {
    it('调用 GET /api/v1/as400/user-profiles/delete-stats', async () => {
      const mockStats: UserProfileDeleteStats = {
        totalDeletes: 10,
        manualDeletes: 3,
        inactiveDeletes: 5,
        resignedDeletes: 2,
        recentRecords: [],
      }
      mockedRequest.get.mockResolvedValue(mockStats)

      const result = await getDeleteStats()

      expect(mockedRequest.get).toHaveBeenCalledWith('/api/v1/as400/user-profiles/delete-stats')
      expect(result).toBe(mockStats)
    })
  })
})
