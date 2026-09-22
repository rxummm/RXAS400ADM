import common from './common'
import menu from './menu'
import quality from './quality'
import cost from './cost'
import mrp from './mrp'
import tpm from './tpm'
import edi from './edi'
import olap from './olap'
import approval from './approval'

/**
 * Simplified static translations: only login/layout/theme/tokenRefresh/status/connection/error/common/menu
 * Business module translations are now in rx_i18n, merged via loadDbTranslations() after login
 */
export default {
  ...common,
  ...menu,
  ...quality,
  ...cost,
  ...mrp,
  ...tpm,
  ...edi,
  ...olap,
  ...approval,
}