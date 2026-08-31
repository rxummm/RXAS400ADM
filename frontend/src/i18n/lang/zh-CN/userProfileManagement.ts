/**
 * 命名空间：userProfileManagement
 * 用途：AS400用户Profile管理
 * 使用方式：$t('userProfileManagement.userName')
 */
export default {
  userProfileManagement: {
    // 列表页
    searchPlaceholder: '搜索用户名...',
    create: '创建用户',
    detail: '详情',
    edit: '编辑',
    delete: '删除',
    deleteConfirm: '确认删除该用户？此操作不可恢复。',
    deleteSuccess: '删除成功',

    // 通用字段
    userName: '用户名',
    status: '状态',
    groupProfile: '组Profile',
    description: '描述',
    lastUsed: '最后使用',
    initialMenu: '初始菜单',
    specialAuthorities: '特殊权限',

    // 创建向导
    createTitle: '创建用户Profile',
    step1Title: '基本信息',
    step2Title: '权限配置',
    step3Title: '完成',
    userNamePlaceholder: '请输入用户名',
    descriptionPlaceholder: '请输入描述',
    password: '初始密码',
    passwordPlaceholder: '请输入初始密码',
    passwordRequired: '请输入初始密码',
    userNameRequired: '请输入用户名',
    mainMenu: '主菜单',
    emailNotification: '邮件通知',
    recipientEmail: '收件人邮箱',
    recipientEmailPlaceholder: '请输入收件人邮箱',
    preview: '创建预览',
    createSuccess: '创建成功',

    // 详情页
    detailTitle: '用户Profile详情',

    // 编辑页
    editTitle: '编辑用户Profile',
    newPassword: '新密码',
    newPasswordPlaceholder: '留空则不修改密码',
    editSuccess: '更新成功',
  },
}
