/**
 * 图标体系（参照旧项目 system/menu 双图标源设计）：
 * - Element Plus 图标：menu.icon 直接存组件名（如 "Monitor"），全局注册于 main.ts
 * - Font Awesome 图标：menu.icon 存全名（如 "fa-solid fa-server"），图标对象注册到 FA library，
 *   经 <FontAwesomeIcon> 渲染；选择器/表格预览统一走 resolveIcon()。
 * 本模块被 main.ts 引用（副作用：注册 library），同时导出选择器与渲染辅助函数。
 */
import { library } from '@fortawesome/fontawesome-svg-core'
import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import {
  faHome, faUser, faUsers, faGear, faCog, faTools, faChartBar, faChartLine,
  faFile, faFolder, faFolderOpen, faTag, faBookmark, faStar, faHeart, faBell,
  faMessage, faEnvelope, faPaperPlane, faShareNodes, faLock, faKey, faShield,
  faEye, faEyeSlash, faPlus, faMinus, faXmark, faCheck, faEdit, faTrash,
  faSearch, faRefresh, faArrowRight, faArrowLeft, faArrowUp, faArrowDown,
  faGlobe, faExpand, faCompress, faCalendar, faClock, faDatabase, faServer,
  faCloud, faCode, faTerminal, faBug, faDesktop, faLaptop, faMobileScreen,
  faWifi, faPlug, faCircleInfo, faTriangleExclamation, faCircleCheck,
  faCircleXmark, faList, faTable, faGrip, faThLarge, faLayerGroup, faCamera,
  faImage, faFilm, faMapLocationDot, faLocationPin, faCompass, faBook,
  faBookOpen, faGraduationCap, faPenNib, faBasketShopping, faCreditCard,
  faDollarSign, faRocket, faBolt, faFire, faAward, faTrophy, faQuestion,
  faLightbulb, faWandMagicSparkles, faCircleHalfStroke, faDiagramProject,
  faClockRotateLeft, faSitemap, faSliders, faBullhorn, faPalette,
  faAddressBook, faBoxesStacked, faBriefcase, faCalendarDays, faClipboard,
  faClipboardCheck, faColumns, faCopy, faFileInvoice, faFlask, faGauge,
  faGaugeHigh, faHand, faListCheck, faPenToSquare, faPeopleArrows,
  faRankingStar, faRotateLeft, faRoute, faScaleBalanced, faShip,
  faStarHalfStroke, faStopwatch, faTableCells, faTruck, faTruckFast,
  faTruckLoading, faTruckRampBox, faUserTie, faWarehouse,
  faKitMedical, faFileLines, faMagnifyingGlass, faCube, faTicket,
  faUserShield, faScrewdriverWrench, faBars, faLocationDot,
  faHouse, faPersonWalkingWithCane, faArrowDownWideShort,
  faChartPie, faBorderAll, faShoePrints, faBarcode, faArrowsTurnToDots,
  faChartSimple, faRobot, faBrain, faCubes, faClipboardList,
  faTableColumns, faIdCard, faHeartbeat, faShieldHalved, faTowerObservation,
} from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

export { FontAwesomeIcon }

interface FaIconItem {
  /** 短名（库 key，如 "server"） */
  key: string
  /** 存储全名（如 "fa-solid fa-server"） */
  fullName: string
  icon: IconDefinition
}

const FA_LIST: FaIconItem[] = [
  { key: 'home', fullName: 'fa-solid fa-home', icon: faHome },
  { key: 'user', fullName: 'fa-solid fa-user', icon: faUser },
  { key: 'users', fullName: 'fa-solid fa-users', icon: faUsers },
  { key: 'gear', fullName: 'fa-solid fa-gear', icon: faGear },
  { key: 'cog', fullName: 'fa-solid fa-cog', icon: faCog },
  { key: 'tools', fullName: 'fa-solid fa-tools', icon: faTools },
  { key: 'chart-bar', fullName: 'fa-solid fa-chart-bar', icon: faChartBar },
  { key: 'chart-line', fullName: 'fa-solid fa-chart-line', icon: faChartLine },
  { key: 'file', fullName: 'fa-solid fa-file', icon: faFile },
  { key: 'folder', fullName: 'fa-solid fa-folder', icon: faFolder },
  { key: 'folder-open', fullName: 'fa-solid fa-folder-open', icon: faFolderOpen },
  { key: 'tag', fullName: 'fa-solid fa-tag', icon: faTag },
  { key: 'bookmark', fullName: 'fa-solid fa-bookmark', icon: faBookmark },
  { key: 'star', fullName: 'fa-solid fa-star', icon: faStar },
  { key: 'heart', fullName: 'fa-solid fa-heart', icon: faHeart },
  { key: 'bell', fullName: 'fa-solid fa-bell', icon: faBell },
  { key: 'message', fullName: 'fa-solid fa-message', icon: faMessage },
  { key: 'envelope', fullName: 'fa-solid fa-envelope', icon: faEnvelope },
  { key: 'paper-plane', fullName: 'fa-solid fa-paper-plane', icon: faPaperPlane },
  { key: 'share', fullName: 'fa-solid fa-share-nodes', icon: faShareNodes },
  { key: 'lock', fullName: 'fa-solid fa-lock', icon: faLock },
  { key: 'key', fullName: 'fa-solid fa-key', icon: faKey },
  { key: 'shield', fullName: 'fa-solid fa-shield', icon: faShield },
  { key: 'eye', fullName: 'fa-solid fa-eye', icon: faEye },
  { key: 'eye-slash', fullName: 'fa-solid fa-eye-slash', icon: faEyeSlash },
  { key: 'plus', fullName: 'fa-solid fa-plus', icon: faPlus },
  { key: 'minus', fullName: 'fa-solid fa-minus', icon: faMinus },
  { key: 'xmark', fullName: 'fa-solid fa-xmark', icon: faXmark },
  { key: 'check', fullName: 'fa-solid fa-check', icon: faCheck },
  { key: 'edit', fullName: 'fa-solid fa-edit', icon: faEdit },
  { key: 'trash', fullName: 'fa-solid fa-trash', icon: faTrash },
  { key: 'search', fullName: 'fa-solid fa-search', icon: faSearch },
  { key: 'refresh', fullName: 'fa-solid fa-refresh', icon: faRefresh },
  { key: 'arrow-right', fullName: 'fa-solid fa-arrow-right', icon: faArrowRight },
  { key: 'arrow-left', fullName: 'fa-solid fa-arrow-left', icon: faArrowLeft },
  { key: 'arrow-up', fullName: 'fa-solid fa-arrow-up', icon: faArrowUp },
  { key: 'arrow-down', fullName: 'fa-solid fa-arrow-down', icon: faArrowDown },
  { key: 'globe', fullName: 'fa-solid fa-globe', icon: faGlobe },
  { key: 'expand', fullName: 'fa-solid fa-expand', icon: faExpand },
  { key: 'compress', fullName: 'fa-solid fa-compress', icon: faCompress },
  { key: 'calendar', fullName: 'fa-solid fa-calendar', icon: faCalendar },
  { key: 'clock', fullName: 'fa-solid fa-clock', icon: faClock },
  { key: 'clock-history', fullName: 'fa-solid fa-clock-rotate-left', icon: faClockRotateLeft },
  // 'warning' is the DB icon name; 'triangle-exclamation' is the canonical FA name — keep both for byFull resolution
  { key: 'warning', fullName: 'fa-solid fa-warning', icon: faTriangleExclamation },
  // Register canonical FA name for menus that use it directly
  { key: 'triangle-exclamation', fullName: 'fa-solid fa-triangle-exclamation', icon: faTriangleExclamation },
  { key: 'database', fullName: 'fa-solid fa-database', icon: faDatabase },
  { key: 'server', fullName: 'fa-solid fa-server', icon: faServer },
  { key: 'cloud', fullName: 'fa-solid fa-cloud', icon: faCloud },
  { key: 'code', fullName: 'fa-solid fa-code', icon: faCode },
  { key: 'terminal', fullName: 'fa-solid fa-terminal', icon: faTerminal },
  { key: 'bug', fullName: 'fa-solid fa-bug', icon: faBug },
  { key: 'monitor', fullName: 'fa-solid fa-desktop', icon: faDesktop },
  { key: 'laptop', fullName: 'fa-solid fa-laptop', icon: faLaptop },
  { key: 'mobile', fullName: 'fa-solid fa-mobile-screen', icon: faMobileScreen },
  { key: 'wifi', fullName: 'fa-solid fa-wifi', icon: faWifi },
  { key: 'plug', fullName: 'fa-solid fa-plug', icon: faPlug },
  { key: 'info', fullName: 'fa-solid fa-circle-info', icon: faCircleInfo },
  { key: 'check-circle', fullName: 'fa-solid fa-circle-check', icon: faCircleCheck },
  { key: 'xmark-circle', fullName: 'fa-solid fa-circle-xmark', icon: faCircleXmark },
  { key: 'list', fullName: 'fa-solid fa-list', icon: faList },
  { key: 'table', fullName: 'fa-solid fa-table', icon: faTable },
  { key: 'grid', fullName: 'fa-solid fa-grip', icon: faGrip },
  { key: 'th-large', fullName: 'fa-solid fa-th-large', icon: faThLarge },
  { key: 'layer-group', fullName: 'fa-solid fa-layer-group', icon: faLayerGroup },
  { key: 'camera', fullName: 'fa-solid fa-camera', icon: faCamera },
  { key: 'image', fullName: 'fa-solid fa-image', icon: faImage },
  { key: 'film', fullName: 'fa-solid fa-film', icon: faFilm },
  { key: 'map-pin', fullName: 'fa-solid fa-map-location-dot', icon: faMapLocationDot },
  { key: 'location', fullName: 'fa-solid fa-location-pin', icon: faLocationPin },
  { key: 'compass', fullName: 'fa-solid fa-compass', icon: faCompass },
  { key: 'book', fullName: 'fa-solid fa-book', icon: faBook },
  { key: 'book-open', fullName: 'fa-solid fa-book-open', icon: faBookOpen },
  { key: 'graduation-cap', fullName: 'fa-solid fa-graduation-cap', icon: faGraduationCap },
  { key: 'pen-nib', fullName: 'fa-solid fa-pen-nib', icon: faPenNib },
  { key: 'cart', fullName: 'fa-solid fa-cart-shopping', icon: faBasketShopping },
  { key: 'credit-card', fullName: 'fa-solid fa-credit-card', icon: faCreditCard },
  { key: 'dollar', fullName: 'fa-solid fa-dollar-sign', icon: faDollarSign },
  { key: 'rocket', fullName: 'fa-solid fa-rocket', icon: faRocket },
  { key: 'bolt', fullName: 'fa-solid fa-bolt', icon: faBolt },
  { key: 'fire', fullName: 'fa-solid fa-fire', icon: faFire },
  { key: 'award', fullName: 'fa-solid fa-award', icon: faAward },
  { key: 'trophy', fullName: 'fa-solid fa-trophy', icon: faTrophy },
  { key: 'question', fullName: 'fa-solid fa-question', icon: faQuestion },
  { key: 'lightbulb', fullName: 'fa-solid fa-lightbulb', icon: faLightbulb },
  { key: 'wand', fullName: 'fa-solid fa-wand-magic-sparkles', icon: faWandMagicSparkles },
  { key: 'contrast', fullName: 'fa-solid fa-circle-half-stroke', icon: faCircleHalfStroke },
  { key: 'diagram', fullName: 'fa-solid fa-diagram-project', icon: faDiagramProject },
  { key: 'history', fullName: 'fa-solid fa-clock-rotate-left', icon: faClockRotateLeft },
  { key: 'sitemap', fullName: 'fa-solid fa-sitemap', icon: faSitemap },
  { key: 'sliders', fullName: 'fa-solid fa-sliders', icon: faSliders },
  { key: 'bullhorn', fullName: 'fa-solid fa-bullhorn', icon: faBullhorn },
  { key: 'palette', fullName: 'fa-solid fa-palette', icon: faPalette },
  { key: 'address-book', fullName: 'fa-solid fa-address-book', icon: faAddressBook },
  { key: 'boxes-stacked', fullName: 'fa-solid fa-boxes-stacked', icon: faBoxesStacked },
  { key: 'briefcase', fullName: 'fa-solid fa-briefcase', icon: faBriefcase },
  { key: 'calendar-days', fullName: 'fa-solid fa-calendar-days', icon: faCalendarDays },
  { key: 'clipboard', fullName: 'fa-solid fa-clipboard', icon: faClipboard },
  { key: 'clipboard-check', fullName: 'fa-solid fa-clipboard-check', icon: faClipboardCheck },
  { key: 'clock-rotate-list', fullName: 'fa-solid fa-clock-rotate-list', icon: faClockRotateLeft },
  { key: 'columns', fullName: 'fa-solid fa-columns', icon: faColumns },
  { key: 'copy', fullName: 'fa-solid fa-copy', icon: faCopy },
  { key: 'file-invoice', fullName: 'fa-solid fa-file-invoice', icon: faFileInvoice },
  { key: 'flask', fullName: 'fa-solid fa-flask', icon: faFlask },
  { key: 'gauge', fullName: 'fa-solid fa-gauge', icon: faGauge },
  { key: 'gears', fullName: 'fa-solid fa-gears', icon: faGear },
  { key: 'gauge-high', fullName: 'fa-solid fa-gauge-high', icon: faGaugeHigh },
  { key: 'hand', fullName: 'fa-solid fa-hand', icon: faHand },
  { key: 'list-check', fullName: 'fa-solid fa-list-check', icon: faListCheck },
  { key: 'pen-to-square', fullName: 'fa-solid fa-pen-to-square', icon: faPenToSquare },
  { key: 'people-arrows', fullName: 'fa-solid fa-people-arrows', icon: faPeopleArrows },
  { key: 'ranking-star', fullName: 'fa-solid fa-ranking-star', icon: faRankingStar },
  { key: 'rotate-left', fullName: 'fa-solid fa-rotate-left', icon: faRotateLeft },
  { key: 'route', fullName: 'fa-solid fa-route', icon: faRoute },
  { key: 'scale-balanced', fullName: 'fa-solid fa-scale-balanced', icon: faScaleBalanced },
  { key: 'ship', fullName: 'fa-solid fa-ship', icon: faShip },
  { key: 'star-half-stroke', fullName: 'fa-solid fa-star-half-stroke', icon: faStarHalfStroke },
  { key: 'stopwatch', fullName: 'fa-solid fa-stopwatch', icon: faStopwatch },
  { key: 'table-cells', fullName: 'fa-solid fa-table-cells', icon: faTableCells },
  { key: 'truck', fullName: 'fa-solid fa-truck', icon: faTruck },
  { key: 'truck-fast', fullName: 'fa-solid fa-truck-fast', icon: faTruckFast },
  { key: 'truck-loading', fullName: 'fa-solid fa-truck-loading', icon: faTruckLoading },
  { key: 'truck-ramp-box', fullName: 'fa-solid fa-truck-ramp-box', icon: faTruckRampBox },
  { key: 'user-tie', fullName: 'fa-solid fa-user-tie', icon: faUserTie },
  { key: 'warehouse', fullName: 'fa-solid fa-warehouse', icon: faWarehouse },
  { key: 'chart-pie', fullName: 'fa-solid fa-chart-pie', icon: faChartPie },
  { key: 'border-all', fullName: 'fa-solid fa-border-all', icon: faBorderAll },
  { key: 'shoe-prints', fullName: 'fa-solid fa-shoe-prints', icon: faShoePrints },
  { key: 'barcode', fullName: 'fa-solid fa-barcode', icon: faBarcode },
  { key: 'arrows-turn-to-dots', fullName: 'fa-solid fa-arrows-turn-to-dots', icon: faArrowsTurnToDots },
  { key: 'chart-simple', fullName: 'fa-solid fa-chart-simple', icon: faChartSimple },
  { key: 'robot', fullName: 'fa-solid fa-robot', icon: faRobot },
  { key: 'brain', fullName: 'fa-solid fa-brain', icon: faBrain },
  { key: 'cubes', fullName: 'fa-solid fa-cubes', icon: faCubes },
  { key: 'clipboard-list', fullName: 'fa-solid fa-clipboard-list', icon: faClipboardList },
  { key: 'table-columns', fullName: 'fa-solid fa-table-columns', icon: faTableColumns },
  { key: 'id-card', fullName: 'fa-solid fa-id-card', icon: faIdCard },
  { key: 'heartbeat', fullName: 'fa-solid fa-heartbeat', icon: faHeartbeat },
  { key: 'shield-halved', fullName: 'fa-solid fa-shield-halved', icon: faShieldHalved },
  { key: 'tower-observation', fullName: 'fa-solid fa-tower-observation', icon: faTowerObservation },
  // V90: unified menu icons — new FA solid equivalents for EP icons
  { key: 'kit-medical', fullName: 'fa-solid fa-kit-medical', icon: faKitMedical },
  { key: 'file-lines', fullName: 'fa-solid fa-file-lines', icon: faFileLines },
  { key: 'magnifying-glass', fullName: 'fa-solid fa-magnifying-glass', icon: faMagnifyingGlass },
  { key: 'cube', fullName: 'fa-solid fa-cube', icon: faCube },
  { key: 'ticket', fullName: 'fa-solid fa-ticket', icon: faTicket },
  { key: 'user-shield', fullName: 'fa-solid fa-user-shield', icon: faUserShield },
  { key: 'screwdriver-wrench', fullName: 'fa-solid fa-screwdriver-wrench', icon: faScrewdriverWrench },
  { key: 'bars', fullName: 'fa-solid fa-bars', icon: faBars },
  { key: 'location-dot', fullName: 'fa-solid fa-location-dot', icon: faLocationDot },
  // V90: V69 WMS EP icons → FA solid
  { key: 'house', fullName: 'fa-solid fa-house', icon: faHouse },
  { key: 'person-walking-with-cane', fullName: 'fa-solid fa-person-walking-with-cane', icon: faPersonWalkingWithCane },
  { key: 'arrow-down-wide-short', fullName: 'fa-solid fa-arrow-down-wide-short', icon: faArrowDownWideShort },
]

// 注册全部精选图标到 FA library（SubMenu 等可按字符串/短名解析渲染）
// 防御性注册：跳过 undefined 图标，避免单个失败导致全部图标丢失
FA_LIST.forEach((item) => {
  if (item.icon) {
    library.add(item.icon)
  } else {
    console.warn('[icons] Skipping undefined icon:', item.key, item.fullName)
  }
})

const byShort = new Map(FA_LIST.map((i) => [i.key, i.icon]))
const byFull = new Map(FA_LIST.map((i) => [i.fullName, i.icon]))

/** 判断值是否为 Font Awesome 图标全名 */
export function isFaIcon(value?: string): boolean {
  return !!value && value.startsWith('fa-')
}

/** 解析 FA 图标全名 → 图标对象（供 <FontAwesomeIcon> 使用） */
export function resolveFaIcon(value?: string): IconDefinition | null {
  if (!value) return null
  return byFull.get(value) || byShort.get(value.replace(/^fa-solid\s+fa-/, '')) || null
}

/** 解析 FA 图标，未收录时回退到问号图标（保证渲染永不传 null） */
export function faIconOr(value?: string): IconDefinition {
  return resolveFaIcon(value) ?? faQuestion
}

export interface ResolvedIcon {
  kind: 'ep' | 'fa'
  value: string | IconDefinition
}

/**
 * 统一图标解析：返回渲染方式。
 * - EP：menu.icon 为 EP 组件名（如 "Monitor"）
 * - FA：menu.icon 为 "fa-solid fa-xxx"
 */
export function resolveIcon(value?: string): ResolvedIcon | null {
  if (!value) return null
  if (isFaIcon(value)) {
    const iconDef = resolveFaIcon(value)
    return iconDef ? { kind: 'fa', value: iconDef } : null
  }
  return { kind: 'ep', value }
}

/** 图标选择器列表：EP 名称 + FA 条目（含搜索关键字） */
export const FA_ICON_OPTIONS = FA_LIST.map((i) => ({
  name: i.key,
  fullName: i.fullName,
  icon: i.icon,
}))
