-- ============================================================
-- V60: 采购订单管理（PO Management）
-- 本地管理表 + 审批流 + 状态跟踪
-- ============================================================

-- ---------- 1. 采购订单主表 ----------
CREATE TABLE IF NOT EXISTS `rx_purchase_order` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `po_no`         VARCHAR(32)     NOT NULL                 COMMENT '采购单号',
    `cono`          VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `vendor_code`   VARCHAR(32)     DEFAULT NULL             COMMENT '供应商编码',
    `vendor_name`   VARCHAR(200)    DEFAULT NULL             COMMENT '供应商名称',
    `order_date`    DATE            DEFAULT NULL             COMMENT '下单日期',
    `req_date`      DATE            DEFAULT NULL             COMMENT '要求交货日期',
    `total_amount`  DECIMAL(18,2)   DEFAULT 0.00             COMMENT '订单总金额',
    `currency`      VARCHAR(8)      DEFAULT 'CNY'            COMMENT '币种',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING_APPROVAL/APPROVED/SHIPPED/RECEIVED/PAID/CANCELLED',
    `approval_level`    TINYINT     DEFAULT 0                COMMENT '当前审批层级（0=未提交, 1=采购员, 2=经理, 3=财务）',
    `approval_status`   VARCHAR(20) DEFAULT 'PENDING'        COMMENT '审批状态：PENDING/APPROVED/REJECTED',
    `approved_by`       VARCHAR(64) DEFAULT NULL             COMMENT '最终审批人',
    `approved_time`     DATETIME    DEFAULT NULL             COMMENT '最终审批时间',
    `notes`             TEXT        DEFAULT NULL             COMMENT '备注',
    `as400_po_no`   VARCHAR(32)     DEFAULT NULL             COMMENT '关联 AS400 PO 号（同步标识）',
    `created_by`    VARCHAR(64)     NOT NULL                 COMMENT '创建人',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`    VARCHAR(64)     DEFAULT NULL             COMMENT '更新人',
    `updated_time`  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_po_no` (`po_no`),
    KEY `idx_status` (`status`),
    KEY `idx_vendor` (`vendor_code`),
    KEY `idx_order_date` (`order_date`),
    KEY `idx_as400_po` (`as400_po_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主表';

-- ---------- 2. 采购订单行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_purchase_order_item` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `po_id`         BIGINT          NOT NULL                 COMMENT '采购订单 ID',
    `line_no`       INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `item_code`     VARCHAR(32)     DEFAULT NULL             COMMENT '物料编码',
    `item_desc`     VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `uom`           VARCHAR(8)      DEFAULT 'EA'             COMMENT '单位',
    `qty_ordered`   DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '订购数量',
    `qty_received`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '已收数量',
    `qty_invoiced`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '已开票数量',
    `unit_price`    DECIMAL(14,4)   DEFAULT 0.0000           COMMENT '单价',
    `line_amount`   DECIMAL(18,2)   DEFAULT 0.00             COMMENT '行金额',
    `req_date`      DATE            DEFAULT NULL             COMMENT '要求到货日期',
    `received_date` DATE            DEFAULT NULL             COMMENT '实际到货日期',
    `notes`         VARCHAR(500)    DEFAULT NULL             COMMENT '行备注',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_po_id` (`po_id`),
    CONSTRAINT `fk_po_item_po` FOREIGN KEY (`po_id`) REFERENCES `rx_purchase_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单行项表';

-- ---------- 3. 采购审批记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_purchase_approval` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `po_id`         BIGINT          NOT NULL                 COMMENT '采购订单 ID',
    `level`         TINYINT         NOT NULL                 COMMENT '审批层级（1=采购员, 2=经理, 3=财务）',
    `approver`      VARCHAR(64)     NOT NULL                 COMMENT '审批人',
    `action`        VARCHAR(20)     NOT NULL                 COMMENT '动作：APPROVED/REJECTED/RETURNED',
    `comment`       VARCHAR(500)    DEFAULT NULL             COMMENT '审批意见',
    `action_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_po_id` (`po_id`),
    CONSTRAINT `fk_approval_po` FOREIGN KEY (`po_id`) REFERENCES `rx_purchase_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购审批记录表';

-- ---------- 4. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('PO_MANAGE', 'PO Management', 'procurement', '采购订单管理：查询/创建/编辑/审批'),
       ('PO_APPROVE', 'PO Approve', 'procurement', '采购订单审批：多级审批操作');

-- ---------- 5. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('PO_MANAGE', 'PO_APPROVE');

-- ---------- 6. 菜单：目录「采购管理」 ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '采购管理', 1, 'procurement', '/procurement', NULL, 'fa-solid fa-cart-shopping', 65, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurement' AND menu_type = 1);

-- ---------- 7. 叶子菜单「采购订单管理」 ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'procurement' AND menu_type = 1 LIMIT 1),
       '采购订单管理', 2, 'procurementPo', '/procurement/po', 'views/procurement/po/index.vue', 'PO_MANAGE', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementPo' AND menu_type = 2);

-- ---------- 8. 叶子菜单「采购审批」 ----------
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT (SELECT id FROM rx_menu WHERE title = 'procurement' AND menu_type = 1 LIMIT 1),
       '采购审批', 2, 'procurementApproval', '/procurement/approval', 'views/procurement/approval/index.vue', 'PO_APPROVE', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementApproval' AND menu_type = 2);

-- ---------- 9. ADMIN 角色挂载新菜单 ----------
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('procurement', 'procurementPo', 'procurementApproval');

-- ---------- 10. i18n 翻译种子 ----------
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
-- 菜单
('menu.procurement', 'zh-CN', '采购管理', 'procurement'),
('menu.procurement', 'en-US', 'Procurement', 'procurement'),
('menu.procurementPo', 'zh-CN', '采购订单管理', 'procurement'),
('menu.procurementPo', 'en-US', 'PO Management', 'procurement'),
('menu.procurementApproval', 'zh-CN', '采购审批', 'procurement'),
('menu.procurementApproval', 'en-US', 'PO Approval', 'procurement'),
-- PO 列表
('procurement.po.title', 'zh-CN', '采购订单', 'procurement'),
('procurement.po.title', 'en-US', 'Purchase Orders', 'procurement'),
('procurement.po.poNo', 'zh-CN', '采购单号', 'procurement'),
('procurement.po.poNo', 'en-US', 'PO Number', 'procurement'),
('procurement.po.vendorCode', 'zh-CN', '供应商编码', 'procurement'),
('procurement.po.vendorCode', 'en-US', 'Vendor Code', 'procurement'),
('procurement.po.vendorName', 'zh-CN', '供应商名称', 'procurement'),
('procurement.po.vendorName', 'en-US', 'Vendor Name', 'procurement'),
('procurement.po.orderDate', 'zh-CN', '下单日期', 'procurement'),
('procurement.po.orderDate', 'en-US', 'Order Date', 'procurement'),
('procurement.po.reqDate', 'zh-CN', '要求交货', 'procurement'),
('procurement.po.reqDate', 'en-US', 'Req. Delivery', 'procurement'),
('procurement.po.totalAmount', 'zh-CN', '总金额', 'procurement'),
('procurement.po.totalAmount', 'en-US', 'Total Amount', 'procurement'),
('procurement.po.currency', 'zh-CN', '币种', 'procurement'),
('procurement.po.currency', 'en-US', 'Currency', 'procurement'),
('procurement.po.status', 'zh-CN', '状态', 'procurement'),
('procurement.po.status', 'en-US', 'Status', 'procurement'),
('procurement.po.notes', 'zh-CN', '备注', 'procurement'),
('procurement.po.notes', 'en-US', 'Notes', 'procurement'),
('procurement.po.createdBy', 'zh-CN', '创建人', 'procurement'),
('procurement.po.createdBy', 'en-US', 'Created By', 'procurement'),
('procurement.po.createdTime', 'zh-CN', '创建时间', 'procurement'),
('procurement.po.createdTime', 'en-US', 'Created Time', 'procurement'),
('procurement.po.approvedBy', 'zh-CN', '审批人', 'procurement'),
('procurement.po.approvedBy', 'en-US', 'Approved By', 'procurement'),
('procurement.po.approvedTime', 'zh-CN', '审批时间', 'procurement'),
('procurement.po.approvedTime', 'en-US', 'Approved Time', 'procurement'),
-- PO 状态
('procurement.po.statusDraft', 'zh-CN', '草稿', 'procurement'),
('procurement.po.statusDraft', 'en-US', 'Draft', 'procurement'),
('procurement.po.statusPendingApproval', 'zh-CN', '待审批', 'procurement'),
('procurement.po.statusPendingApproval', 'en-US', 'Pending Approval', 'procurement'),
('procurement.po.statusApproved', 'zh-CN', '已批准', 'procurement'),
('procurement.po.statusApproved', 'en-US', 'Approved', 'procurement'),
('procurement.po.statusShipped', 'zh-CN', '已发运', 'procurement'),
('procurement.po.statusShipped', 'en-US', 'Shipped', 'procurement'),
('procurement.po.statusReceived', 'zh-CN', '已收货', 'procurement'),
('procurement.po.statusReceived', 'en-US', 'Received', 'procurement'),
('procurement.po.statusPaid', 'zh-CN', '已付款', 'procurement'),
('procurement.po.statusPaid', 'en-US', 'Paid', 'procurement'),
('procurement.po.statusCancelled', 'zh-CN', '已取消', 'procurement'),
('procurement.po.statusCancelled', 'en-US', 'Cancelled', 'procurement'),
-- PO 行项
('procurement.po.lineNo', 'zh-CN', '行号', 'procurement'),
('procurement.po.lineNo', 'en-US', 'Line', 'procurement'),
('procurement.po.itemCode', 'zh-CN', '物料编码', 'procurement'),
('procurement.po.itemCode', 'en-US', 'Item Code', 'procurement'),
('procurement.po.itemDesc', 'zh-CN', '物料描述', 'procurement'),
('procurement.po.itemDesc', 'en-US', 'Description', 'procurement'),
('procurement.po.uom', 'zh-CN', '单位', 'procurement'),
('procurement.po.uom', 'en-US', 'UOM', 'procurement'),
('procurement.po.qtyOrdered', 'zh-CN', '订购数量', 'procurement'),
('procurement.po.qtyOrdered', 'en-US', 'Ordered', 'procurement'),
('procurement.po.qtyReceived', 'zh-CN', '已收数量', 'procurement'),
('procurement.po.qtyReceived', 'en-US', 'Received', 'procurement'),
('procurement.po.qtyInvoiced', 'zh-CN', '已开票', 'procurement'),
('procurement.po.qtyInvoiced', 'en-US', 'Invoiced', 'procurement'),
('procurement.po.unitPrice', 'zh-CN', '单价', 'procurement'),
('procurement.po.unitPrice', 'en-US', 'Unit Price', 'procurement'),
('procurement.po.lineAmount', 'zh-CN', '行金额', 'procurement'),
('procurement.po.lineAmount', 'en-US', 'Line Amount', 'procurement'),
-- PO 操作
('procurement.po.create', 'zh-CN', '新建采购订单', 'procurement'),
('procurement.po.create', 'en-US', 'Create PO', 'procurement'),
('procurement.po.edit', 'zh-CN', '编辑', 'procurement'),
('procurement.po.edit', 'en-US', 'Edit', 'procurement'),
('procurement.po.submitApproval', 'zh-CN', '提交审批', 'procurement'),
('procurement.po.submitApproval', 'en-US', 'Submit for Approval', 'procurement'),
('procurement.po.receive', 'zh-CN', '收货', 'procurement'),
('procurement.po.receive', 'en-US', 'Receive', 'procurement'),
('procurement.po.cancel', 'zh-CN', '取消', 'procurement'),
('procurement.po.cancel', 'en-US', 'Cancel', 'procurement'),
('procurement.po.detail', 'zh-CN', '订单详情', 'procurement'),
('procurement.po.detail', 'en-US', 'PO Detail', 'procurement'),
('procurement.po.basicInfo', 'zh-CN', '基本信息', 'procurement'),
('procurement.po.basicInfo', 'en-US', 'Basic Info', 'procurement'),
('procurement.po.lineItems', 'zh-CN', '订单行项', 'procurement'),
('procurement.po.lineItems', 'en-US', 'Line Items', 'procurement'),
('procurement.po.approvalHistory', 'zh-CN', '审批历史', 'procurement'),
('procurement.po.approvalHistory', 'en-US', 'Approval History', 'procurement'),
-- 审批
('procurement.approval.title', 'zh-CN', '采购审批', 'procurement'),
('procurement.approval.title', 'en-US', 'PO Approval', 'procurement'),
('procurement.approval.approve', 'zh-CN', '批准', 'procurement'),
('procurement.approval.approve', 'en-US', 'Approve', 'procurement'),
('procurement.approval.reject', 'zh-CN', '驳回', 'procurement'),
('procurement.approval.reject', 'en-US', 'Reject', 'procurement'),
('procurement.approval.return', 'zh-CN', '退回修改', 'procurement'),
('procurement.approval.return', 'en-US', 'Return', 'procurement'),
('procurement.approval.comment', 'zh-CN', '审批意见', 'procurement'),
('procurement.approval.comment', 'en-US', 'Comment', 'procurement'),
('procurement.approval.confirmApprove', 'zh-CN', '确认批准此采购订单？', 'procurement'),
('procurement.approval.confirmApprove', 'en-US', 'Confirm approve this PO?', 'procurement'),
('procurement.approval.confirmReject', 'zh-CN', '确认驳回此采购订单？', 'procurement'),
('procurement.approval.confirmReject', 'en-US', 'Confirm reject this PO?', 'procurement'),
('procurement.approval.level1', 'zh-CN', '采购员审批', 'procurement'),
('procurement.approval.level1', 'en-US', 'Buyer Approval', 'procurement'),
('procurement.approval.level2', 'zh-CN', '经理审批', 'procurement'),
('procurement.approval.level2', 'en-US', 'Manager Approval', 'procurement'),
('procurement.approval.level3', 'zh-CN', '财务审批', 'procurement'),
('procurement.approval.level3', 'en-US', 'Finance Approval', 'procurement');