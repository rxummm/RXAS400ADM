package com.rxas400adm.as400;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.as400.sql.SqlStatementRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen ObjectClient 委托实现（对象搜索 / 详情 / 引用 / 权限 / 拓扑）。
 */
class JTOpenObjectClient implements ObjectClient {

    private final JTOpenSqlClient sqlClient;
    private final JTOpenCommandClient commandClient;

    JTOpenObjectClient(JTOpenConnectionState state) {
        this.sqlClient = new JTOpenSqlClient(state);
        this.commandClient = new JTOpenCommandClient(state);
    }

    @Override
    public boolean objectExists(String library, String object) {
        CommandResult r = commandClient.execute(
                "DSPOBJD OBJ(" + JTOpenConnectionState.requireIdentifier(library, "库名") + "/"
                        + JTOpenConnectionState.requireIdentifier(object, "对象名")
                        + ") OBJTYPE(*ALL) OUTPUT(*PRINT)");
        return r.success();
    }

    @Override
    public List<ObjectRow> searchObjects(String library, String objectType) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        StringBuilder sql = new StringBuilder(SqlStatementRegistry.of("object.search.base"));
        List<Object> params = new ArrayList<>();
        params.add(lib);
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND OBJECT_TYPE = ?");
            params.add(objectType.trim().toUpperCase());
        }
        sql.append(" FETCH FIRST 200 ROWS ONLY");
        return sqlClient.queryList(sql.toString(), params.toArray()).stream().map(r -> new ObjectRow(
                str(r, "OBJECT_NAME"), str(r, "OBJECT_TYPE"), str(r, "OBJECT_LIBRARY"),
                lng(r, "OBJECT_SIZE"), str(r, "OBJECT_CREATION_TIMESTAMP"))).toList();
    }

    @Override
    public ObjectDetail objectDetail(String library, String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        String sql = SqlStatementRegistry.of("object.detail");
        List<Map<String, Object>> rows = sqlClient.queryList(sql, lib, objectName.toUpperCase());
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        if (row == null || row.isEmpty()) {
            return null;
        }
        return new ObjectDetail(
                str(row, "OBJECT_NAME"), str(row, "OBJECT_TYPE"), str(row, "OBJECT_LIBRARY"),
                lng(row, "OBJECT_SIZE"), str(row, "OBJECT_CREATION_TIMESTAMP"),
                str(row, "OBJECT_CHANGE_TIMESTAMP"), str(row, "OBJECT_TEXT_DESCRIPTION"),
                str(row, "OBJECT_OWNER"), str(row, "ASP_NAME"));
    }

    @Override
    public List<ObjectRefRow> objectReferences(String library, String objectName, String direction) {
        if (objectName == null || objectName.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        boolean inbound = direction != null && "IN".equalsIgnoreCase(direction);
        String sql = inbound ? SqlStatementRegistry.of("object.refs.inbound") : SqlStatementRegistry.of("object.refs.outbound");
        return sqlClient.queryList(sql, lib, objectName.toUpperCase()).stream().map(r -> new ObjectRefRow(
                str(r, "OBJECT_LIBRARY"), str(r, "OBJECT_NAME"), str(r, "OBJECT_TYPE"),
                str(r, "REF_OBJ_LIBRARY"), str(r, "REF_OBJ_NAME"), str(r, "REF_OBJ_TYPE"))).toList();
    }

    @Override
    public List<AuthorityRow> objectAuthorities(String library, String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return List.of();
        }
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        return sqlClient.queryList(SqlStatementRegistry.of("object.authority.list"), lib, objectName.trim().toUpperCase()).stream()
                .map(r -> new AuthorityRow(str(r, "AUTHORITY_HOLDER"), str(r, "AUTHORITY_HOLDER_TYPE"),
                        str(r, "AUTHORITY")))
                .toList();
    }

    @Override
    public GraphData objectGraph(String library) {
        String lib = library == null || library.isBlank() ? "QSYS" : library.trim().toUpperCase();
        List<Map<String, Object>> rows = sqlClient.queryList(SqlStatementRegistry.of("object.graph"), lib);
        Map<String, GraphNode> nodes = new LinkedHashMap<>();
        List<GraphLink> links = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String srcLib = str(r, "OBJECT_LIBRARY").isBlank() ? lib : str(r, "OBJECT_LIBRARY");
            String srcName = str(r, "OBJECT_NAME");
            String srcType = str(r, "OBJECT_TYPE");
            String refLib = str(r, "REF_OBJ_LIBRARY").isBlank() ? srcLib : str(r, "REF_OBJ_LIBRARY");
            String refName = str(r, "REF_OBJ_NAME");
            String refType = str(r, "REF_OBJ_TYPE");
            if (srcName.isBlank() || refName.isBlank()) {
                continue;
            }
            nodes.computeIfAbsent(srcLib + "." + srcName, k -> new GraphNode(k, srcName, srcType, srcLib));
            nodes.computeIfAbsent(refLib + "." + refName, k -> new GraphNode(k, refName, refType, refLib));
            links.add(new GraphLink(srcLib + "." + srcName, refLib + "." + refName));
        }
        return new GraphData(List.copyOf(nodes.values()), links);
    }


}