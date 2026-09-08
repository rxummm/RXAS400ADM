import common from './common'
import menu from './menu'
import operation from './operation'

/**
 * 精简后的静态翻译：仅保留 login/layout/theme/tokenRefresh/status/connection/error/common/menu
 * 业务模块翻译已全量入库（rx_i18n），登录后由 loadDbTranslations() merge 覆盖
 */
export default {
  ...common,
  ...menu,
  ...operation,
}
