/**
 * Namespace: userProfileManagement
 * Purpose: AS400 User Profile Management
 * Usage: $t('userProfileManagement.userName')
 */
export default {
  userProfileManagement: {
    // List page
    searchPlaceholder: 'Search username...',
    create: 'Create User',
    detail: 'Detail',
    edit: 'Edit',
    delete: 'Delete',
    deleteConfirm: 'Confirm to delete this user? This action cannot be undone.',
    deleteSuccess: 'Delete successful',

    // Common fields
    userName: 'Username',
    status: 'Status',
    groupProfile: 'Group Profile',
    description: 'Description',
    lastUsed: 'Last Used',
    initialMenu: 'Initial Menu',
    specialAuthorities: 'Special Authorities',

    // Create wizard
    createTitle: 'Create User Profile',
    step1Title: 'Basic Info',
    step2Title: 'Authority Config',
    step3Title: 'Complete',
    userNamePlaceholder: 'Enter username',
    descriptionPlaceholder: 'Enter description',
    password: 'Initial Password',
    passwordPlaceholder: 'Enter initial password',
    passwordRequired: 'Please enter initial password',
    userNameRequired: 'Please enter username',
    mainMenu: 'Main Menu',
    emailNotification: 'Email Notification',
    recipientEmail: 'Recipient Email',
    recipientEmailPlaceholder: 'Enter recipient email',
    preview: 'Create Preview',
    createSuccess: 'Create successful',

    // Detail page
    detailTitle: 'User Profile Detail',

    // Edit page
    editTitle: 'Edit User Profile',
    newPassword: 'New Password',
    newPasswordPlaceholder: 'Leave empty to keep current password',
    editSuccess: 'Update successful',
  },
}
