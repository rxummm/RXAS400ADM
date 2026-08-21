/**
 * Namespace: assets / topology / as400Server
 * Purpose: Server management (instance management, topology analysis, server selector)
 * Usage: $t('assets.title') / $t('topology.title') / $t('as400Server.selectorTitle')
 */
export default {
  assets: {
    title: 'Server Management (AS400 Instances)',
    name: 'Server',
    host: 'Host',
    port: 'Port',
    environment: 'Environment',
    level: 'Level',
    status: 'Status',
    haGroup: 'HA Group',
    username: 'Connection Account',
    region: 'Region',
    test: 'Test Connection',
    add: 'Add Server',
    edit: 'Edit Server',
    password: 'Connection Password',
    passwordHint: 'Password is encrypted with AES-256-GCM; leave empty to keep current password',
    passwordPlaceholder: 'Leave empty to keep current password',
    passwordRequired: 'Please enter connection password',
    defaultLibraries: 'Default Libraries',
    sortOrder: 'Sort Order',
    description: 'Description',
    enabled: 'Enabled',
    defaultServer: 'Default',
    required: 'Server name and host are required',
    deleteConfirm: 'Confirm to delete server "{name}"? Related monitoring data will lose server association.',
    command: 'Execute Command',
    commandResult: 'Execution Result',
    commandHint: 'Enter CL command and click Execute (e.g. DSPLIB QGPL / WRKACTJOB)',
    execute: 'Execute',
    serverSyncHint: 'Maintained servers are synced in real-time to the top bar server selector / login page AS400 mode dropdown / business page server dropdowns',
  },
  topology: {
    title: 'Call Topology Analysis (DSPPGMREF)',
    library: 'Library (default APP)',
    hint: 'Click nodes to view details and references; drag/scroll to zoom',
    empty: 'No topology data (library unreachable or no references in library)',
  },
  as400Server: {
    selectorTitle: 'Select AS400 Server',
    defaultTag: 'Default',
    hint: 'All operations will target the selected server after switching',
    latency: 'Latency',
  },
}