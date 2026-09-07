import common from './common'
import menu from './menu'

/**
 * Simplified static translations: only login/layout/theme/tokenRefresh/status/connection/error/common/menu
 * Business module translations are now in rx_i18n, merged via loadDbTranslations() after login
 */
export default {
  ...common,
  ...menu,
}
