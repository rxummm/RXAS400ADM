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
 * 精简后的静态翻译：仅保留 login/layout/theme/tokenRefresh/status/connection/error/common/menu
 * 业务模块翻译已全量入库（rx_i18n），登录后由 loadDbTranslations() merge 覆盖
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