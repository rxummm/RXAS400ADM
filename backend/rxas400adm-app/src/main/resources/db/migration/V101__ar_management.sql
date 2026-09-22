-- ============================================================
-- V101: 应收账款管理（AR Management）
-- 客户账单 + 收款核销 + 账龄分析
-- ============================================================

-- ---------- 1. 应收账款发票表 ----------
CREATE TABLE IF NOT EXISTS `rx_ar_invoice` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `invoice_no`      VARCHAR(32)     NOT NULL                 COMMENT '账单号（AR + yyyyMMdd + 4位序号）',
    `cono`           VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `customer_code`   VARCHAR(32)     NOT NULL                 COMMENT '客户编码',
    `customer_name`   VARCHAR(200)    DEFAULT NULL             COMMENT '客户名称',
    `invoice_date`    DATE           NOT NULL                 COMMENT '开票日期',
    `due_date`        DATE           NOT NULL                 COMMENT '到期日期',
    `origin_po_no`    VARCHAR(32)     DEFAULT NULL             COMMENT '来源采购单号（关联 PO）',
    `origin_so_no`    VARCHAR(32)     DEFAULT NULL             COMMENT '来源销售单号',
    `subtotal`        DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '小计',
    `tax_rate`        DECIMAL(6,4)    DEFAULT 0.0000           COMMENT '税率',
    `tax_amount`      DECIMAL(18,2)   DEFAULT 0.00             COMMENT '税额',
    `total_amount`    DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '总金额',
    `paid_amount`     DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '已付金额',
    `balance`         DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '未结余额',
    `currency`        VARCHAR(8)      DEFAULT 'CNY'            COMMENT '币种',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/OPEN/PARTIAL/PAID/OVERDUE/WRITE_OFF',
    `aging_bucket`    VARCHAR(20)     DEFAULT NULL             COMMENT '账龄区间：0-30/31-60/61-90/90+',
    `notes`           TEXT            DEFAULT NULL             COMMENT '备注',
    `created_by`      VARCHAR(64)     NOT NULL                 COMMENT '创建人',
    `created_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    `updated_time`    DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    KEY `idx_customer` (`customer_code`),
    KEY `idx_status` (`status`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_invoice_date` (`invoice_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款发票表';

-- ---------- 2. 收款记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_ar_payment` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `payment_no`      VARCHAR(32)     NOT NULL                 COMMENT '收款单号（PMT + yyyyMMdd + 4位序号）',
    `cono`           VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `invoice_id`      BIGINT          NOT NULL                 COMMENT '关联发票 ID',
    `payment_date`    DATE           NOT NULL                 COMMENT '收款日期',
    `amount`          DECIMAL(18,2)   NOT NULL                 COMMENT '收款金额',
    `payment_method`  VARCHAR(20)     DEFAULT 'BANK_TRANSFER' COMMENT '付款方式：BANK_TRANSFER/CASH/CHECK/CREDIT_CARD/OTHER',
    `reference_no`    VARCHAR(64)     DEFAULT NULL             COMMENT '银行流水号/参考号',
    `received_by`     VARCHAR(64)     DEFAULT NULL             COMMENT '收款人',
    `notes`           VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`      VARCHAR(64)     NOT NULL                 COMMENT '创建人',
    `created_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_invoice` (`invoice_id`),
    KEY `idx_payment_date` (`payment_date`),
    CONSTRAINT `fk_ar_payment_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `rx_ar_invoice` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款记录表';

-- ---------- 3. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('AR_MANAGE', 'AR Management', 'ar', '应收账款管理：账单查看/创建/收款核销/账龄分析');

-- ---------- 4. ADMIN 授权 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'AR_MANAGE'
  AND NOT EXISTS (SELECT 1 FROM rx_role_permission WHERE role_id = r.id AND permission_id = p.id);

-- ---------- 5. 菜单：目录「财务管理」 ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '财务管理', 1, 'ar', '/ar', NULL, 'fa-solid fa-landmark', 70, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ar' AND menu_type = 1);

-- ---------- 6. 叶子菜单「应收账款」 ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'ar' AND menu_type = 1 LIMIT 1),
       '应收账款管理', 2, 'arInvoice', '/ar/invoice', 'views/finance/ar/index.vue', 'AR_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'arInvoice' AND menu_type = 2);

-- ---------- 7. ADMIN 角色挂载新菜单 ----------
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('ar', 'arInvoice');

-- ---------- 8. i18n 翻译种子 ----------
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
-- 菜单
('menu.ar', 'zh-CN', '财务管理', 'ar'),
('menu.ar', 'en-US', 'Finance', 'ar'),
('menu.arInvoice', 'zh-CN', '应收账款管理', 'ar'),
('menu.arInvoice', 'en-US', 'Accounts Receivable', 'ar'),
-- 列表
('ar.invoice.title', 'zh-CN', '应收账款列表', 'ar'),
('ar.invoice.title', 'en-US', 'Accounts Receivable', 'ar'),
('ar.invoice.invoiceNo', 'zh-CN', '账单号', 'ar'),
('ar.invoice.invoiceNo', 'en-US', 'Invoice No.', 'ar'),
('ar.invoice.customerCode', 'zh-CN', '客户编码', 'ar'),
('ar.invoice.customerCode', 'en-US', 'Customer Code', 'ar'),
('ar.invoice.customerName', 'zh-CN', '客户名称', 'ar'),
('ar.invoice.customerName', 'en-US', 'Customer Name', 'ar'),
('ar.invoice.originPoNo', 'zh-CN', '来源 PO', 'ar'),
('ar.invoice.originPoNo', 'en-US', 'Source PO', 'ar'),
('ar.invoice.originSoNo', 'zh-CN', '来源 SO', 'ar'),
('ar.invoice.originSoNo', 'en-US', 'Source SO', 'ar'),
('ar.invoice.invoiceDate', 'zh-CN', '开票日期', 'ar'),
('ar.invoice.invoiceDate', 'en-US', 'Invoice Date', 'ar'),
('ar.invoice.dueDate', 'zh-CN', '到期日期', 'ar'),
('ar.invoice.dueDate', 'en-US', 'Due Date', 'ar'),
('ar.invoice.subtotal', 'zh-CN', '小计', 'ar'),
('ar.invoice.subtotal', 'en-US', 'Subtotal', 'ar'),
('ar.invoice.taxRate', 'zh-CN', '税率', 'ar'),
('ar.invoice.taxRate', 'en-US', 'Tax Rate', 'ar'),
('ar.invoice.taxAmount', 'zh-CN', '税额', 'ar'),
('ar.invoice.taxAmount', 'en-US', 'Tax Amount', 'ar'),
('ar.invoice.totalAmount', 'zh-CN', '总金额', 'ar'),
('ar.invoice.totalAmount', 'en-US', 'Total Amount', 'ar'),
('ar.invoice.paidAmount', 'zh-CN', '已付金额', 'ar'),
('ar.invoice.paidAmount', 'en-US', 'Paid Amount', 'ar'),
('ar.invoice.balance', 'zh-CN', '未结余额', 'ar'),
('ar.invoice.balance', 'en-US', 'Balance', 'ar'),
('ar.invoice.currency', 'zh-CN', '币种', 'ar'),
('ar.invoice.currency', 'en-US', 'Currency', 'ar'),
('ar.invoice.status', 'zh-CN', '状态', 'ar'),
('ar.invoice.status', 'en-US', 'Status', 'ar'),
('ar.invoice.agingBucket', 'zh-CN', '账龄', 'ar'),
('ar.invoice.agingBucket', 'en-US', 'Aging', 'ar'),
('ar.invoice.notes', 'zh-CN', '备注', 'ar'),
('ar.invoice.notes', 'en-US', 'Notes', 'ar'),
('ar.invoice.createdBy', 'zh-CN', '创建人', 'ar'),
('ar.invoice.createdBy', 'en-US', 'Created By', 'ar'),
('ar.invoice.createdTime', 'zh-CN', '创建时间', 'ar'),
('ar.invoice.createdTime', 'en-US', 'Created Time', 'ar'),
-- 账龄区间
('ar.aging0_30', 'zh-CN', '0-30天', 'ar'),
('ar.aging0_30', 'en-US', '0-30 days', 'ar'),
('ar.aging31_60', 'zh-CN', '31-60天', 'ar'),
('ar.aging31_60', 'en-US', '31-60 days', 'ar'),
('ar.aging61_90', 'zh-CN', '61-90天', 'ar'),
('ar.aging61_90', 'en-US', '61-90 days', 'ar'),
('ar.aging90plus', 'zh-CN', '90天以上', 'ar'),
('ar.aging90plus', 'en-US', '90+ days', 'ar'),
-- 状态
('ar.statusDraft', 'zh-CN', '草稿', 'ar'),
('ar.statusDraft', 'en-US', 'Draft', 'ar'),
('ar.statusOpen', 'zh-CN', '未付款', 'ar'),
('ar.statusOpen', 'en-US', 'Open', 'ar'),
('ar.statusPartial', 'zh-CN', '部分付款', 'ar'),
('ar.statusPartial', 'en-US', 'Partially Paid', 'ar'),
('ar.statusPaid', 'zh-CN', '已结清', 'ar'),
('ar.statusPaid', 'en-US', 'Paid', 'ar'),
('ar.statusOverdue', 'zh-CN', '已逾期', 'ar'),
('ar.statusOverdue', 'en-US', 'Overdue', 'ar'),
('ar.statusWriteOff', 'zh-CN', '已核销', 'ar'),
('ar.statusWriteOff', 'en-US', 'Write Off', 'ar'),
-- 操作
('ar.invoice.create', 'zh-CN', '新建账单', 'ar'),
('ar.invoice.create', 'en-US', 'Create Invoice', 'ar'),
('ar.invoice.edit', 'zh-CN', '编辑账单', 'ar'),
('ar.invoice.edit', 'en-US', 'Edit Invoice', 'ar'),
('ar.invoice.receive', 'zh-CN', '收款核销', 'ar'),
('ar.invoice.receive', 'en-US', 'Record Payment', 'ar'),
('ar.invoice.detail', 'zh-CN', '账单详情', 'ar'),
('ar.invoice.detail', 'en-US', 'Invoice Detail', 'ar'),
('ar.invoice.payments', 'zh-CN', '收款记录', 'ar'),
('ar.invoice.payments', 'en-US', 'Payment History', 'ar'),
('ar.invoice.aging', 'zh-CN', '账龄分析', 'ar'),
('ar.invoice.aging', 'en-US', 'Aging Analysis', 'ar'),
('ar.payment.paymentNo', 'zh-CN', '收款单号', 'ar'),
('ar.payment.paymentNo', 'en-US', 'Payment No.', 'ar'),
('ar.payment.paymentDate', 'zh-CN', '收款日期', 'ar'),
('ar.payment.paymentDate', 'en-US', 'Payment Date', 'ar'),
('ar.payment.amount', 'zh-CN', '收款金额', 'ar'),
('ar.payment.amount', 'en-US', 'Amount', 'ar'),
('ar.payment.paymentMethod', 'zh-CN', '付款方式', 'ar'),
('ar.payment.paymentMethod', 'en-US', 'Payment Method', 'ar'),
('ar.payment.referenceNo', 'zh-CN', '参考号', 'ar'),
('ar.payment.referenceNo', 'en-US', 'Reference No.', 'ar'),
('ar.payment.receivedBy', 'zh-CN', '收款人', 'ar'),
('ar.payment.receivedBy', 'en-US', 'Received By', 'ar'),
('ar.paymentMethod.bankTransfer', 'zh-CN', '银行转账', 'ar'),
('ar.paymentMethod.bankTransfer', 'en-US', 'Bank Transfer', 'ar'),
('ar.paymentMethod.cash', 'zh-CN', '现金', 'ar'),
('ar.paymentMethod.cash', 'en-US', 'Cash', 'ar'),
('ar.paymentMethod.check', 'zh-CN', '支票', 'ar'),
('ar.paymentMethod.check', 'en-US', 'Check', 'ar'),
('ar.paymentMethod.creditCard', 'zh-CN', '信用卡', 'ar'),
('ar.paymentMethod.creditCard', 'en-US', 'Credit Card', 'ar'),
('ar.paymentMethod.other', 'zh-CN', '其他', 'ar'),
('ar.paymentMethod.other', 'en-US', 'Other', 'ar'),
-- 弹框
('ar.confirmReceive', 'zh-CN', '确认收到款项？', 'ar'),
('ar.confirmReceive', 'en-US', 'Confirm receiving payment?', 'ar'),
('ar.agingTitle', 'zh-CN', '账龄分析报表', 'ar'),
('ar.agingTitle', 'en-US', 'Aging Analysis Report', 'ar'),
('ar.agingTotal', 'zh-CN', '合计', 'ar'),
('ar.agingTotal', 'en-US', 'Total', 'ar'),
('ar.balanceZero', 'zh-CN', '已结清', 'ar'),
('ar.balanceZero', 'en-US', 'Fully Paid', 'ar'),
('ar.balanceDue', 'zh-CN', '未结', 'ar'),
('ar.balanceDue', 'en-US', 'Due', 'ar'),
('ar.noInvoice', 'zh-CN', '暂无账单数据', 'ar'),
('ar.noInvoice', 'en-US', 'No invoices yet', 'ar'),
('ar.agingOverdue', 'zh-CN', '逾期', 'ar'),
('ar.agingOverdue', 'en-US', 'Overdue', 'ar'),
('ar.agingCurrent', 'zh-CN', '当前', 'ar'),
('ar.agingCurrent', 'en-US', 'Current', 'ar');
