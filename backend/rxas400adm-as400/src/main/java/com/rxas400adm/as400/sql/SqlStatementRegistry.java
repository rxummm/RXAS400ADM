package com.rxas400adm.as400.sql;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【第六章·P1】AS400 侧 SQL 语句注册器——SQL 集中声明于 classpath:sql/*.xml，
 * Java 代码按 id 取用，禁止内联拼 SQL（开发规范对齐主数据源 Mapper XML 惯例）。
 *
 * <p>为什么不是 MyBatis Mapper：BPCS 查询必须随 {@code X-AS400-Server} 头路由到
 * <b>用户所选的 AS400 服务器</b>（经 AS400ClientProvider 的 per-host 连接池执行），
 * 而 MyBatis 主数据源固定（MySQL/RXADMIN），无法随请求切换目标机器；
 * 故本注册器仅承担「SQL 文本的集中管理与加载」，执行仍走
 * {@code AS400Client.queryListCheckedBounded}（只读校验 + 占位符参数 + setMaxRows 护栏不变）。
 *
 * <p>XML 形态：
 * <pre>{@code
 * <statements>
 *   <statement id="bpcs.order.header">SELECT * FROM {lib}.ECH ...</statement>
 * </statements>
 * }</pre>
 * 其中 {@code {lib}} 为业务侧白名单校验后的 Library 占位符（标识符不可参数化，
 * Service 层负责 replace 并保证已过 IDENTIFIER 校验）；{@code ?} 为 JDBC 参数占位符。
 */
@Slf4j
@Component
public class SqlStatementRegistry {

    private static final String LOCATION_PATTERN = "classpath:sql/as400-*.xml";

    private final Map<String, String> statements = new ConcurrentHashMap<>();

    @PostConstruct
    void load() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            for (Resource resource : resolver.getResources(LOCATION_PATTERN)) {
                parse(resource);
            }
            log.info("[SQL注册器] 已加载 {} 条 AS400 语句", statements.size());
        } catch (Exception e) {
            throw new IllegalStateException("AS400 SQL 注册器初始化失败", e);
        }
    }

    /** 按 id 取 SQL 文本；未注册视为编程错误直接抛出 */
    public String get(String id) {
        String sql = statements.get(id);
        if (sql == null) {
            throw new IllegalStateException("未注册的 AS400 SQL 语句: " + id);
        }
        return sql;
    }

    private void parse(Resource resource) {
        try {
            var doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(resource.getInputStream());
            NodeList nodes = doc.getElementsByTagName("statement");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String id = el.getAttribute("id");
                if (id == null || id.isBlank()) {
                    throw new IllegalStateException("statement 缺少 id: " + resource.getFilename());
                }
                statements.put(id, el.getTextContent().trim());
            }
        } catch (Exception e) {
            throw new IllegalStateException("AS400 SQL 注册文件解析失败: " + resource.getFilename(), e);
        }
    }
}
