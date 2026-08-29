/**
 * 命名空间：bpcs —— AS400 业务增强（BPCS/Infor LX 数据只读视图）
 * 用途：客户订单时间轴、客户档案、库存可用量、发运看板等
 * 使用方式：$t('bpcs.searchHint') / $t('bpcs.stage.created') / $t('bpcs.line.qtyOrdered')
 */
export default {
  bpcs: {
    searchHint: '输入公司码与订单号查询 BPCS 客户订单',
    searchRequired: '请填写公司码与订单号',
    notFound: '未找到该订单（可能已归档至历史库或单号有误）',

    label: {
      cono: '公司码',
      orno: '订单号',
      orderNo: '订单号',
      customer: '客户号',
      shipTo: '收货点',
      orderDate: '订购日期',
      reqDate: '要求交期',
      lineCount: '订单行数',
      rawStatus: '原始状态位',
      wh: '仓库',
    },

    line: {
      orln: '行号',
      item: '物料号',
      itemDesc: '物料描述',
      wh: '仓库',
      qtyOrdered: '订购数量',
      qtyAllocated: '分货数量',
      qtyShipped: '发货数量',
      qtyInvoiced: '开票数量',
      price: '单价',
      discPct: '折扣%',
      reqDate: '要求交期',
      status: '行状态',
    },

    detail: {
      title: '订单行详情',
      statusGroup: '状态',
      itemGroup: '物料信息',
      qtyGroup: '数量信息',
      allocationGroup: '分配摘要',
      priceGroup: '价格与交期',
      qtyHint: '提示：各数量随流程节点逐步产生，未到达阶段的数量显示为 "—"。',
      ordered: '订购量',
      allocated: '已分配',
      pending: '待分配',
      fulfillRate: '分配率',
      lineAmount: '行金额',
    },

    stage: {
      created: '订单录入',
      pick_released: '拣货释放',
      pick_confirmed: '拣货确认',
      billed: '已开票',
      closed: '已关闭',
    },

    timeline: {
      title: '订单进程',
      pending: '待处理',
    },

    // ---- P2: 客户档案 ----
    customer: {
      searchPlaceholder: '输入客户号或名称搜索',
      selectHint: '← 请从左侧选择客户查看详情',
      address: '地址',
      city: '城市/邮编',
      phone: '电话',
      contact: '联系人',
      creditLimit: '信用额度',
      termsCode: '付款条件',
      taxCode: '税务码',
      salesArea: '销售区域',
      shipToTitle: '收货点',
      shipName: '收货点名称',
    },

    // ---- P2: 库存可用量 ----
    inventory: {
      itemPlaceholder: '输入物料号搜索',
      descPlaceholder: '输入物料描述搜索',
      totalOnHand: '在手总量',
      totalAllocated: '已分配总量',
      totalOnOrder: '在途总量',
      totalAvailable: '可用总量',
      onHand: '在手',
      allocated: '已分配',
      onOrder: '在途',
      available: '可用',
      unitCost: '单位成本',
      uom: '单位',
      whCount: '仓库数',
      location: '库位',
    },

    // ---- P2: 发运看板 ----
    shipping: {
      lhnoPlaceholder: '输入载荷号搜索',
      hint: '看板按状态分列展示：计划中 → 已确认 → 已放行 → 已发运',
      detailTitle: '载荷详情',
      lhno: '载荷号',
      status: '状态',
      carrier: '承运人',
      shipDate: '发运日期',
      weight: '重量',
      orders: '订单',
      ordersTitle: '承载订单',
      lines: '行',
    },

    // 载荷状态
    loadStatus: {
      planned: '计划中',
      firmed: '已确认',
      released: '已放行',
      dispatched: '已发运',
    },

    // ---- P2: 发票轨迹 ----
    invoice: {
      activeTab: '在制发票',
      historyTab: '开票历史',
      invNo: '发票号',
      status: '状态',
      invDate: '发票日期',
      totalAmount: '总金额',
      taxAmount: '税额',
      lineCount: '行数',
      invoiceCount: '发票数',
      totalTax: '税额合计',
      detailTitle: '发票详情',
      linesTitle: '发票行明细',
      lineAmount: '行金额',
    },
    invoiceStatus: {
      active: '在制',
      posted: '已过账',
      cancelled: '已冲销',
    },

    // ---- P2: 销售趋势 ----
    sales: {
      fromYm: '起始月 YYYYMM',
      toYm: '结束月 YYYYMM',
      totalRevenue: '总销售额',
      totalOrders: '总订单数',
      totalLines: '总行数',
      months: '月份数',
      month: '月份',
      revenue: '销售额',
      orderCount: '订单数',
      lineCount: '行数',
      avgOrderValue: '平均客单价',
    },

    // ---- P2: 采购订单 ----
    purchase: {
      ponoPlaceholder: '输入采购单号搜索',
      vendorPlaceholder: '输入供应商名搜索',
      pono: '采购单号',
      vendor: '供应商',
      orderDate: '订购日期',
      totalAmount: '总金额',
      lineCount: '行数',
      status: '状态',
      qtyOrdered: '订购量',
      qtyReceived: '收货量',
      detailTitle: '采购订单详情',
      linesTitle: '采购订单行',
    },
    // ---- P2: 物料主档 ----
    item: {
      searchPlaceholder: '输入物料号或描述搜索',
      selectHint: '← 请从左侧选择物料查看详情',
      tabInventory: '库存',
      tabPurchase: '采购历史',
      tabSales: '销售历史',
      tabBasic: '基本信息',
      uom: '单位',
      category: '分类',
      unitCost: '标准成本',
      listPrice: '列表价',
      weight: '重量',
      shelfLife: '保质期',
    },

    // ---- 供应链增强：订单列表 ----
    orderList: {
      fromDate: '起始日期 YYYYMMDD',
      toDate: '结束日期 YYYYMMDD',
    },

    // ---- 供应链增强：库存预警 ----
    inventoryAlert: {
      hint: '可用量低于安全库存的物料预警列表',
      alertCount: '预警物料数',
      totalDeficit: '总缺口量',
      safetyStock: '安全库存',
      deficit: '缺口',
      allGood: '所有物料库存充足，无预警',
    },

    // ---- 供应链增强：销售分析 ----
    salesAnalysis: {
      topCustomers: 'Top 客户',
      topItems: 'Top 物料',
      totalRevenue: '总销售额',
      totalOrders: '总订单数',
      orderCount: '订单数',
      totalQty: '总数量',
      totalAmount: '总金额',
    },

    // ---- Phase 2: 库存变动历史 ----
    inventoryHistory: {
      itemNo: '物料号',
      itemNoPlaceholder: '输入物料号查询',
      fromDate: '起始 YYYYMMDD',
      toDate: '结束 YYYYMMDD',
      dateRange: '日期范围',
      totalTransactions: '交易总数',
      totalInbound: '总入库',
      totalOutbound: '总出库',
      refType: '参考类型',
      refNo: '参考号',
      transactionType: '交易类型',
      quantity: '数量',
      beforeQty: '变更前',
      afterQty: '变更后',
      noData: '请输入物料号查询库存变动历史',
      userId: '操作人',
    },
    txType: {
      receipt: '入库',
      issue: '出库',
      transfer: '调拨',
      adjustment: '调整',
      cycleCount: '盘点',
    },

    // ---- Phase 2: 采购收货管理 ----
    purchaseReceiving: {
      poNo: '采购单号',
      poNoPlaceholder: '输入采购单号查询',
      vendorName: '供应商',
      orderQty: '订单数量',
      receivedQty: '已收数量',
      openQty: '待收数量',
      receiveRate: '收货率',
      lastReceiptDate: '最近收货日期',
      complete: '已收完',
      pending: '待收货',
      noData: '请输入采购单号查询收货记录',
    },

    // ---- Phase 2: 发运列表视图 ----
    shippingList: {
      toggleKanban: '看板视图',
      toggleList: '列表视图',
      lhno: '载荷号',
      status: '状态',
      carrier: '承运人',
      shipDate: '发运日期',
      weight: '重量',
      orderCount: '订单数',
      lineCount: '行数',
      shipToCity: '目的地',
      statusPlanned: '计划中',
      statusFirmed: '已确认',
      statusReleased: '已放行',
      statusDispatched: '已发运',
      statusUnknown: '未知',
    },

    // ---- Phase 3: ABC 分析 ----
    abcAnalysis: {
      title: 'ABC 库存价值分类分析',
      hint: '按库存价值从高到低排序，A 类占 80% 价值，B 类占 15%，C 类占 5%',
      classA: 'A 类（高价值）',
      classB: 'B 类（中价值）',
      classC: 'C 类（低价值）',
      itemNo: '物料号',
      description: '描述',
      onHandQty: '在手数量',
      unitCost: '单价',
      totalValue: '总价值',
      valuePercent: '价值占比',
      cumulativePercent: '累计占比',
      totalItems: '物料总数',
      aCount: 'A 类数',
      bCount: 'B 类数',
      cCount: 'C 类数',
    },

    // ---- Phase 3: 供应商绩效 ----
    supplierPerf: {
      title: '供应商绩效分析',
      hint: '基于采购订单和收货记录计算供应商绩效指标',
      vendorCode: '供应商代码',
      vendorName: '供应商名称',
      totalOrders: '总订单数',
      totalAmount: '总采购额',
      avgLeadTime: '平均提前期（天）',
      onTimeRate: '准时交货率',
      avgPrice: '平均单价',
      priceTrend: '价格趋势',
      rising: '上升',
      falling: '下降',
      stable: '稳定',
    },

    // ---- Phase 4: 供应链 KPI ----
    kpi: {
      title: '供应链 KPI 仪表盘',
      totalOrders: '总订单数',
      closedOrders: '已完成订单',
      completionRate: '订单完成率',
      totalItems: '库存物料数',
      inventoryValue: '库存总价值',
      onTimeDeliveryRate: '准时交货率',
      otdTitle: '准时交货率（OTD）',
      otdHint: '按时发运的订单占总订单比例',
      turnoverTitle: '库存周转率',
      turnoverHint: '销售成本 ÷ 平均库存',
      fillRateTitle: '订单满足率',
      fillRateHint: '完整交付的订单占总订单比例',
      avgLeadTimeTitle: '平均提前期',
      avgLeadTimeHint: '从下单到发运的平均天数',
      period: '统计周期',
      last30Days: '近 30 天',
      last90Days: '近 90 天',
      lastYear: '近一年',
    },

    // ---- Phase 4: 订单全链路追踪 ----
    orderTracking: {
      title: '订单全链路追踪',
      hint: '输入订单号查看从下单→库存分配→拣货→发运→签收的完整链路',
      orderNoPlaceholder: '输入订单号查询',
      orderPrefix: '订单：',
      orderInfo: '订单信息',
      allocationInfo: '库存分配',
      pickInfo: '拣货信息',
      shippingInfo: '发运信息',
      receiptInfo: '签收信息',
      noData: '请输入订单号开始追踪',
      stepOrder: '订单创建',
      stepAllocate: '库存分配',
      stepPick: '拣货确认',
      stepShip: '发运',
      stepInvoice: '开票',
      stepReceive: '签收',
      notStarted: '未开始',
      inProgress: '进行中',
      completed: '已完成',
      closed: '已关闭',
    },

    poStatus: {
      open: '待收货',
      partial: '部分收货',
      complete: '已收完',
      closed: '已关闭',
    },
  },
}