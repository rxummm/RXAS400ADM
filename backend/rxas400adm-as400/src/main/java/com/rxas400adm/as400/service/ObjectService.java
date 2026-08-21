package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对象搜索 / 详情 / 引用分析（DSPOBJD / DSPPGMREF / QSYS2.OBJECT_STATISTICS）。
 * 数据源按 X-AS400-Server 头路由。M2：返回类型收敛为 record（ObjectRow 等）。
 */
@Service
@RequiredArgsConstructor
public class ObjectService implements IObjectService {

    private final AS400ClientProvider clientProvider;

    /**
     * 对象搜索（支持关键字过滤 + 后端分页，避免大库全量传输）。
     *
     * @param library    库名
     * @param objectType 对象类型
     * @param keyword    名称/库/类型关键字（可选）
     * @param current    页码（从 1 开始）
     * @param size       每页条数
     */
    public PageResult<ObjectRow> searchObjects(String library, String objectType,
                                               String keyword, int current, int size) {
        AS400Client client = clientProvider.current();
        List<ObjectRow> all = client.searchObjects(library, objectType);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            all = all.stream().filter(row -> {
                String name = row.name();
                String lib = row.library();
                String type = row.type();
                return (name != null && name.toLowerCase().contains(kw))
                        || (lib != null && lib.toLowerCase().contains(kw))
                        || (type != null && type.toLowerCase().contains(kw));
            }).toList();
        }
        int total = all.size();
        int from = Math.min((current - 1) * size, total);
        int to = Math.min(from + size, total);
        List<ObjectRow> records = from >= to ? List.of() : all.subList(from, to);
        return new PageResult<>(total, records);
    }

    public ObjectDetail objectDetail(String library, String objectName) {
        return clientProvider.current().objectDetail(library, objectName);
    }

    public List<ObjectRefRow> objectReferences(String library, String objectName, String direction) {
        return clientProvider.current().objectReferences(library, objectName, direction);
    }

    public List<AuthorityRow> objectAuthorities(String library, String objectName) {
        return clientProvider.current().objectAuthorities(library, objectName);
    }
}
