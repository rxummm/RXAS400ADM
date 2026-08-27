package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsOrderQueryDTO;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsOrderHeaderVO;
import com.rxas400adm.as400.vo.BpcsOrderLineVO;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.BpcsDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 【AS400 业务增强·P1】BPCS 客户订单查询实现。
 *
 * <p><b>执行通道</b>：查询随 {@code X-AS400-Server} 头路由到用户所选 AS400 服务器
 * （{@code clientProvider.current()} → per-host 连接池）；SQL 文本集中声明于
 * {@code classpath:sql/as400-bpcs.xml}（经 SqlStatementRegistry 加载），Java 零内联 SQL。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>SELECT * + 多候选取列</b>：BPCS 字段名存在版本/客户化差异（设计文档 §10①②），
 *       Java 端按候选键名容错提取，真机首跑成功率最大化；</li>
 *   <li><b>状态推导纯函数</b>：{@link #deriveStageIndex(boolean, boolean, boolean, boolean)} 独立可测；
 *       环境事实：CHSTS3/CHSTS4（发运释放/确认）恒为 0，时间轴节点已裁剪；</li>
 *   <li><b>库名配置化</b>：rx_config 键 {@code bpcs.library}（缺省 BPCSF，系统配置页可维护），
 *       拼接前经 IDENTIFIER 白名单校验（复用 S6 成果），仅标识符允许受控拼接；</li>
 *   <li><b>mock 档</b>：ProfileResolver 判定后短路返回 MockBpcsData 演示集
 *       （三张示例订单覆盖 已关闭/进行中/新录入 形态），并由内存过滤按单号精确匹配。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsOrderServiceImpl implements IBpcsOrderService {

    /** rx_config 键：BPCS 文件所在 Library（系统配置页可维护） */
    public static final String KEY_LIBRARY = "bpcs.library";
    private static final String DEFAULT_LIBRARY = "BPCSF";

    /** 时间轴阶段 key（顺序即推导顺序；发运两阶段因 CHSTS3/4 恒 0 不纳入） */
    static final String[] STAGE_KEYS = {
            "CREATED", "PICK_RELEASED", "PICK_CONFIRMED", "BILLED", "CLOSED"
    };

    private static final String SQL_HEADER = "bpcs.order.header";
    private static final String SQL_LINES = "bpcs.order.lines";
    private static final String SQL_LINE_COUNT = "bpcs.order.lineCount";

    /** 单订单行拉取上限（ECL 行数量级为几十，500 已远超业务上限） */
    private static final int MAX_LINES = 500;

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;

    /** 【分层】as400 模块不依赖 system，故以函数式接口读取 rx_config（bpcs.library）；
     *  由 app 模块装配：k -> sysConfigService.get(k, def)。 */
    public interface SysConfigServiceHolder {
        String get(String key, String defaultValue);
    }

    private final SysConfigServiceHolder configHolder;

    // ---------------- 对外接口 ----------------

    @Override
    public BpcsOrderHeaderVO getHeader(BpcsOrderQueryDTO query) {
        String cono = norm(query.getCono());
        String orno = norm(query.getOrno());
        List<Map<String, Object>> rows = queryRows(SQL_HEADER, 2, cono, orno);
        Map<String, Object> row = rows.stream()
                .filter(r -> match(r, "CONO", cono) && match(r, "ORNO", orno))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "订单不存在: " + cono + "/" + orno));

        boolean c1 = flag(row, "CLSTS1", "CHSTS1");
        boolean c2 = flag(row, "CLSTS2", "CHSTS2");
        // 终检事实：CHSTS3/CHSTS4 在目标环境恒为 0，不参与推导
        boolean c5 = flag(row, "CLSTS5", "CHSTS5");
        boolean closed = "CZ".equalsIgnoreCase(BpcsRowUtil.pickStr(row, "HID"));
        int current = currentStageIndex(new boolean[]{true, c1, c2, c5, closed});

        return new BpcsOrderHeaderVO(
                cono, orno,
                BpcsRowUtil.pickStr(row, "CUST", "CUNO"),
                BpcsRowUtil.pickStr(row, "SHIP"),
                BpcsRowUtil.dateStr(row, "ORDTE", "ENTDTE", "ODATE"),
                BpcsRowUtil.dateStr(row, "REQDTE", "REQDATE"),
                null, // 总额由前端按行汇总（头金额字段各环境差异大，避免脏数据）
                linesCount(cono, orno),
                current,
                buildTimeline(orderDate(row), new boolean[]{c1, c2, c5}, closed),
                List.of(), // holds 预留：ECH 冻结字段名待环境确认（设计文档 §10⑥）
                new BpcsOrderHeaderVO.Raw(
                        concatFlags(new boolean[]{flag(row, "CHSTS1"), flag(row, "CHSTS2"),
                                flag(row, "CHSTS3"), flag(row, "CHSTS4"), flag(row, "CHSTS5")}),
                        BpcsRowUtil.pickStr(row, "HSTAT"),
                        BpcsRowUtil.pickStr(row, "HID")));
    }

    @Override
    public List<BpcsOrderLineVO> getLines(BpcsOrderQueryDTO query) {
        String cono = norm(query.getCono());
        String orno = norm(query.getOrno());
        List<Map<String, Object>> rows = queryRows(SQL_LINES, MAX_LINES, cono, orno);

        List<BpcsOrderLineVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            // 内存侧二次过滤：真机为幂等兜底；mock（substitute 内联无谓词）依赖此过滤按单号精确匹配
            if (!match(row, "CONO", cono) || !match(row, "ORNO", orno)) {
                continue;
            }
            result.add(toLineVO(row));
        }
        return result;
    }

    // ---------------- 查询执行（mock 分支 + 服务器路由） ----------------

    /**
     * 统一查询入口：mock 档返回演示数据集（内存按单号过滤）；
     * 真机档取注册器中的 SQL 文本，替换受控 {lib} 后经当前所选 AS400 服务器执行。
     */
    private List<Map<String, Object>> queryRows(String statementId, int maxRows,
                                                String cono, String orno) {
        if (profileResolver.isMockMode()) {
            if (SQL_LINE_COUNT.equals(statementId)) {
                List<Map<String, Object>> all = MockBpcsData.orderLines();
                long count = all.stream()
                        .filter(r -> match(r, "CONO", cono) && match(r, "ORNO", orno))
                        .count();
                return List.of(Map.of("LINECNT", (int) count));
            }
            List<Map<String, Object>> all = MockBpcsData.orderHeaders();
            if (!SQL_HEADER.equals(statementId)) {
                all = MockBpcsData.orderLines();
            }
            List<Map<String, Object>> matched = new ArrayList<>();
            for (Map<String, Object> row : all) {
                if (match(row, "CONO", cono) && match(row, "ORNO", orno)) {
                    matched.add(row);
                }
            }
            return matched;
        }
        String lib = library();
        String sql = statements.get(statementId).replace("{lib}", lib);
        return clientProvider.current()
                .queryListCheckedBounded(sql, maxRows, cono, orno);
    }

    private int linesCount(String cono, String orno) {
        try {
            var rows = queryRows(SQL_LINE_COUNT, 10, cono, orno);
            if (!rows.isEmpty() && rows.get(0).get("LINECNT") instanceof Number n) {
                return n.intValue();
            }
        } catch (Exception e) {
            log.warn("订单行计数失败({}/{}): {}", cono, orno, e.getMessage());
        }
        return 0;
    }

    private String library() {
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, DEFAULT_LIBRARY);
        String v = lib == null ? "" : lib.trim().toUpperCase();
        if (!As400Identifiers.IDENTIFIER.matcher(v).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "bpcs.library 配置非法（仅允许字母/数字/_/$/#/@）: " + lib);
        }
        return v;
    }

    // ---------------- 状态推导与 VO 组装 ----------------

    /**
     * 行阶段推导：录入(恒) → CLSTS1 拣货释放 → CLSTS2 拣货确认 → CLSTS5 开票 → LID 关闭。
     * 返回最后达成的阶段下标。（CLSTS3/CLSTS4 本环境恒 0，不参与）
     */
    static int deriveStageIndex(boolean pickReleased, boolean pickConfirmed,
                                boolean billed, boolean closed) {
        if (closed) {
            return 4;
        }
        if (billed) {
            return 3;
        }
        if (pickConfirmed) {
            return 2;
        }
        if (pickReleased) {
            return 1;
        }
        return 0;
    }

    static int currentStageIndex(boolean[] reached) {
        int idx = 0;
        for (int i = 0; i < reached.length; i++) {
            if (reached[i]) {
                idx = i;
            }
        }
        return idx;
    }

    static String stageKey(int index) {
        return STAGE_KEYS[Math.max(0, Math.min(index, STAGE_KEYS.length - 1))];
    }

    private String orderDate(Map<String, Object> row) {
        return BpcsRowUtil.dateStr(row, "ORDTE", "ENTDTE", "ODATE");
    }

    private List<BpcsOrderHeaderVO.TimelineNodeVO> buildTimeline(
            String orderDate, boolean[] flags, boolean closed) {
        // flags: [c1, c2, c5] —— 发运释放/确认两节点在本环境恒 0，已按用户确认裁剪
        boolean c1 = flags[0];
        boolean c2 = flags[1];
        boolean c5 = flags[2];
        int current = currentStageIndex(new boolean[]{true, c1, c2, c5, closed});

        List<BpcsOrderHeaderVO.TimelineNodeVO> nodes = new ArrayList<>();
        nodes.add(node("CREATED", true, orderDate, current == 0));
        nodes.add(node("PICK_RELEASED", c1, null, current == 1));
        nodes.add(node("PICK_CONFIRMED", c2, null, current == 2));
        nodes.add(node("BILLED", c5, null, current == 3));
        nodes.add(node("CLOSED", closed, null, current == 4));
        return nodes;
    }

    private BpcsOrderHeaderVO.TimelineNodeVO node(String key, boolean reached, String ts, boolean current) {
        return new BpcsOrderHeaderVO.TimelineNodeVO(key,
                "bpcs.stage." + key.toLowerCase(), reached, ts, current);
    }

    private BpcsOrderLineVO toLineVO(Map<String, Object> row) {
        boolean c1 = flag(row, "CLSTS1");
        boolean c2 = flag(row, "CLSTS2");
        boolean c5 = flag(row, "CLSTS5");
        boolean closed = "ZL".equalsIgnoreCase(BpcsRowUtil.pickStr(row, "LID"));
        int stageIndex = deriveStageIndex(c1, c2, c5, closed);

        return new BpcsOrderLineVO(
                BpcsRowUtil.pickStr(row, "ORLN"),
                BpcsRowUtil.pickStr(row, "ITEM"),
                BpcsRowUtil.pickStr(row, "ITDSC"),
                BpcsRowUtil.pickStr(row, "WH"),
                BpcsRowUtil.intOrNull(row, "QTORD", "QTYORD"),
                BpcsRowUtil.intOrNull(row, "QTYALC", "QTALC", "ALLOCQTY"),
                BpcsRowUtil.intOrNull(row, "QTSHP", "QTYSHP", "SHPPQTY"),
                BpcsRowUtil.intOrNull(row, "QTINV", "QTYINV"),
                BpcsRowUtil.decOrNull(row, "PRICE", "UPRICE"),
                BpcsRowUtil.decOrNull(row, "DISC", "DISCPCT"),
                BpcsRowUtil.dateStr(row, "REQDTE", "DUEDTE"),
                stageIndex,
                stageKey(stageIndex),
                concatFlags(new boolean[]{c1, c2, false, false, c5}));
    }

    // ---------------- 小工具 ----------------

    private static String norm(String v) {
        if (v == null || v.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "公司码/订单号不能为空");
        }
        return v.trim();
    }

    /** 行内指定键与期望值比对（大小写不敏感；mock 内联后值可能带引号，trim 防御） */
    private static boolean match(Map<String, Object> row, String key, String expected) {
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)) {
                String v = en.getValue() == null ? "" : String.valueOf(en.getValue()).trim();
                return v.equalsIgnoreCase(expected.trim());
            }
        }
        return false;
    }

    private static boolean flag(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            for (Map.Entry<String, Object> en : row.entrySet()) {
                if (en.getKey() != null && en.getKey().equalsIgnoreCase(k)
                        && en.getValue() != null
                        && "1".equals(String.valueOf(en.getValue()).trim())) {
                    return true;
                }
            }
        }
        return false;
    }


    private static String concatFlags(boolean[] flags) {
        StringBuilder sb = new StringBuilder();
        for (boolean f : flags) {
            sb.append(f ? '1' : '0');
        }
        return sb.toString();
    }
}
