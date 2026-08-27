/**
 * Namespace: bpcs — AS400 business enhancement (BPCS/Infor LX read-only views)
 * Usage: $t('bpcs.searchHint') / $t('bpcs.stage.created') / $t('bpcs.line.qtyOrdered')
 */
export default {
  bpcs: {
    searchHint: 'Enter company code and order number to query BPCS customer orders',
    searchRequired: 'Company code and order number are required',
    notFound: 'Order not found (may be purged to history, or the number is incorrect)',

    label: {
      cono: 'Company',
      orno: 'Order No.',
      orderNo: 'Order No.',
      customer: 'Customer No.',
      shipTo: 'Ship-To',
      orderDate: 'Order Date',
      reqDate: 'Requested Date',
      lineCount: 'Lines',
      rawStatus: 'Raw Status Flags',
      wh: 'Warehouse',
    },

    line: {
      orln: 'Line',
      item: 'Item',
      itemDesc: 'Description',
      wh: 'Warehouse',
      qtyOrdered: 'Qty Ordered',
      qtyAllocated: 'Qty Allocated',
      qtyShipped: 'Qty Shipped',
      qtyInvoiced: 'Qty Invoiced',
      price: 'Unit Price',
      discPct: 'Disc %',
      reqDate: 'Requested Date',
      status: 'Line Status',
    },

    detail: {
      title: 'Order Line Detail',
      statusGroup: 'Status',
      itemGroup: 'Item Info',
      qtyGroup: 'Quantities',
      allocationGroup: 'Allocation Summary',
      priceGroup: 'Price & Dates',
      qtyHint: 'Note: quantities are produced stage by stage; "—" means not yet reached.',
      ordered: 'Ordered',
      allocated: 'Allocated',
      pending: 'Pending',
      fulfillRate: 'Fill Rate',
      lineAmount: 'Line Amount',
    },

    stage: {
      created: 'Created',
      pick_released: 'Pick Released',
      pick_confirmed: 'Pick Confirmed',
      billed: 'Billed',
      closed: 'Closed',
    },

    timeline: {
      title: 'Order Progress',
      pending: 'Pending',
    },

    // ---- P2: Customer Master ----
    customer: {
      searchPlaceholder: 'Search by customer number or name',
      selectHint: '← Select a customer from the list to view details',
      address: 'Address',
      city: 'City / Zip',
      phone: 'Phone',
      contact: 'Contact',
      creditLimit: 'Credit Limit',
      termsCode: 'Payment Terms',
      taxCode: 'Tax Code',
      salesArea: 'Sales Area',
      shipToTitle: 'Ship-To Addresses',
      shipName: 'Ship-To Name',
    },

    // ---- P2: Inventory Availability ----
    inventory: {
      itemPlaceholder: 'Search by item number',
      descPlaceholder: 'Search by item description',
      totalOnHand: 'Total On Hand',
      totalAllocated: 'Total Allocated',
      totalOnOrder: 'Total On Order',
      totalAvailable: 'Total Available',
      onHand: 'On Hand',
      allocated: 'Allocated',
      onOrder: 'On Order',
      available: 'Available',
      unitCost: 'Unit Cost',
      uom: 'UOM',
      whCount: 'Warehouses',
      location: 'Location',
    },

    // ---- P2: Shipping Kanban ----
    shipping: {
      lhnoPlaceholder: 'Search by load number',
      hint: 'Kanban board: Planned → Firmed → Released → Dispatched',
      detailTitle: 'Load Detail',
      lhno: 'Load No.',
      status: 'Status',
      carrier: 'Carrier',
      shipDate: 'Ship Date',
      weight: 'Weight',
      orders: 'Orders',
      ordersTitle: 'Contained Orders',
      lines: 'lines',
    },

    loadStatus: {
      planned: 'Planned',
      firmed: 'Firmed',
      released: 'Released',
      dispatched: 'Dispatched',
    },

    // ---- P2: Invoice Trail ----
    invoice: {
      activeTab: 'In-Progress',
      historyTab: 'History',
      invNo: 'Invoice No.',
      status: 'Status',
      invDate: 'Invoice Date',
      totalAmount: 'Total Amount',
      taxAmount: 'Tax Amount',
      lineCount: 'Lines',
      invoiceCount: 'Invoices',
      totalTax: 'Total Tax',
      detailTitle: 'Invoice Detail',
      linesTitle: 'Invoice Lines',
      lineAmount: 'Line Amount',
    },
    invoiceStatus: {
      active: 'In Progress',
      posted: 'Posted',
      cancelled: 'Cancelled',
    },

    // ---- P2: Sales Trend ----
    sales: {
      fromYm: 'From YYYYMM',
      toYm: 'To YYYYMM',
      totalRevenue: 'Total Revenue',
      totalOrders: 'Total Orders',
      totalLines: 'Total Lines',
      months: 'Months',
      month: 'Month',
      revenue: 'Revenue',
      orderCount: 'Orders',
      lineCount: 'Lines',
      avgOrderValue: 'Avg Order Value',
    },

    // ---- P2: Purchase Orders ----
    purchase: {
      ponoPlaceholder: 'Search by PO number',
      vendorPlaceholder: 'Search by vendor name',
      pono: 'PO No.',
      vendor: 'Vendor',
      orderDate: 'Order Date',
      totalAmount: 'Total Amount',
      lineCount: 'Lines',
      status: 'Status',
      qtyOrdered: 'Qty Ordered',
      qtyReceived: 'Qty Received',
      detailTitle: 'Purchase Order Detail',
      linesTitle: 'PO Lines',
    },
    // ---- P2: Item Master ----
    item: {
      searchPlaceholder: 'Search by item number or description',
      selectHint: '← Select an item from the list to view details',
      tabInventory: 'Inventory',
      tabPurchase: 'Purchase History',
      tabSales: 'Sales History',
      tabBasic: 'Basic Info',
      uom: 'UOM',
      category: 'Category',
      unitCost: 'Std Cost',
      listPrice: 'List Price',
      weight: 'Weight',
      shelfLife: 'Shelf Life',
    },

    // ---- Supply Chain Enhancement: Order List ----
    orderList: {
      fromDate: 'From Date YYYYMMDD',
      toDate: 'To Date YYYYMMDD',
    },

    // ---- Supply Chain Enhancement: Inventory Alert ----
    inventoryAlert: {
      hint: 'Items where available quantity is below safety stock level',
      alertCount: 'Alert Items',
      totalDeficit: 'Total Deficit',
      safetyStock: 'Safety Stock',
      deficit: 'Deficit',
      allGood: 'All items have sufficient stock, no alerts',
    },

    // ---- Supply Chain Enhancement: Sales Analysis ----
    salesAnalysis: {
      topCustomers: 'Top Customers',
      topItems: 'Top Items',
      totalRevenue: 'Total Revenue',
      totalOrders: 'Total Orders',
      orderCount: 'Orders',
      totalQty: 'Total Qty',
      totalAmount: 'Total Amount',
    },

    // ---- Phase 2: Inventory History ----
    inventoryHistory: {
      itemNo: 'Item No.',
      itemNoPlaceholder: 'Enter item number to query',
      fromDate: 'From YYYYMMDD',
      toDate: 'To YYYYMMDD',
      dateRange: 'Date Range',
      totalTransactions: 'Total Transactions',
      totalInbound: 'Total Inbound',
      totalOutbound: 'Total Outbound',
      refType: 'Reference Type',
      refNo: 'Reference No.',
      transactionType: 'Transaction Type',
      quantity: 'Quantity',
      beforeQty: 'Before',
      afterQty: 'After',
      noData: 'Enter an item number to view inventory movement history',
      userId: 'User',
    },
    txType: {
      receipt: 'Receipt',
      issue: 'Issue',
      transfer: 'Transfer',
      adjustment: 'Adjustment',
      cycleCount: 'Cycle Count',
    },

    // ---- Phase 2: Purchase Receiving ----
    purchaseReceiving: {
      poNo: 'PO No.',
      poNoPlaceholder: 'Enter PO number to query',
      vendorName: 'Vendor',
      orderQty: 'Order Qty',
      receivedQty: 'Received Qty',
      openQty: 'Open Qty',
      receiveRate: 'Receive Rate',
      lastReceiptDate: 'Last Receipt Date',
      complete: 'Complete',
      pending: 'Pending',
      noData: 'Enter a PO number to view receiving records',
    },

    // ---- Phase 2: Shipping List ----
    shippingList: {
      toggleKanban: 'Kanban View',
      toggleList: 'List View',
      lhno: 'Load No.',
      status: 'Status',
      carrier: 'Carrier',
      shipDate: 'Ship Date',
      weight: 'Weight',
      orderCount: 'Orders',
      lineCount: 'Lines',
      shipToCity: 'Destination',
    },

    // ---- Phase 3: ABC Analysis ----
    abcAnalysis: {
      title: 'ABC Inventory Value Analysis',
      hint: 'Items sorted by inventory value: A-class = 80% value, B-class = 15%, C-class = 5%',
      classA: 'A Class (High Value)',
      classB: 'B Class (Medium Value)',
      classC: 'C Class (Low Value)',
      itemNo: 'Item No.',
      description: 'Description',
      onHandQty: 'On Hand Qty',
      unitCost: 'Unit Cost',
      totalValue: 'Total Value',
      valuePercent: 'Value %',
      cumulativePercent: 'Cumulative %',
      totalItems: 'Total Items',
      aCount: 'A Count',
      bCount: 'B Count',
      cCount: 'C Count',
    },

    // ---- Phase 3: Supplier Performance ----
    supplierPerf: {
      title: 'Supplier Performance Analysis',
      hint: 'Based on purchase orders and receiving records',
      vendorCode: 'Vendor Code',
      vendorName: 'Vendor Name',
      totalOrders: 'Total Orders',
      totalAmount: 'Total Purchases',
      avgLeadTime: 'Avg Lead Time (days)',
      onTimeRate: 'On-Time Delivery Rate',
      avgPrice: 'Avg Unit Price',
      priceTrend: 'Price Trend',
      rising: 'Rising',
      falling: 'Falling',
      stable: 'Stable',
    },

    // ---- Phase 4: Supply Chain KPI ----
    kpi: {
      title: 'Supply Chain KPI Dashboard',
      otdTitle: 'On-Time Delivery (OTD)',
      otdHint: 'Ratio of orders shipped on time',
      turnoverTitle: 'Inventory Turnover',
      turnoverHint: 'COGS / Average Inventory',
      fillRateTitle: 'Order Fill Rate',
      fillRateHint: 'Ratio of fully delivered orders',
      avgLeadTimeTitle: 'Avg Lead Time',
      avgLeadTimeHint: 'Average days from order to shipment',
      period: 'Period',
      last30Days: 'Last 30 Days',
      last90Days: 'Last 90 Days',
      lastYear: 'Last Year',
    },

    // ---- Phase 4: Order Full-Chain Tracking ----
    orderTracking: {
      title: 'Order Full-Chain Tracking',
      hint: 'Enter order number to track order → allocation → pick → ship → receipt',
      orderNoPlaceholder: 'Enter order number to track',
      orderInfo: 'Order Info',
      allocationInfo: 'Inventory Allocation',
      pickInfo: 'Picking Info',
      shippingInfo: 'Shipping Info',
      receiptInfo: 'Receipt Info',
      noData: 'Enter an order number to start tracking',
      stepOrder: 'Order Created',
      stepAllocate: 'Allocated',
      stepPick: 'Pick Confirmed',
      stepShip: 'Shipped',
      stepReceive: 'Received',
      notStarted: 'Not Started',
      inProgress: 'In Progress',
      completed: 'Completed',
    },

    poStatus: {
      open: 'Open',
      partial: 'Partial',
      complete: 'Complete',
      closed: 'Closed',
    },
  },
}
