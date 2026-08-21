package com.rxas400adm.as400;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;

import java.util.List;

/**
 * 对象域：DSPOBJD 搜索 / DSPPGMREF 引用 / DSPOBJAUT 权限 / 库级调用拓扑。
 */
public interface ObjectClient {

    /** 对象是否存在 */
    boolean objectExists(String library, String object);

    /**
     * 对象搜索（DSPOBJD）：按库与对象类型查询对象清单。
     * type 为 null/空表示全部类型。
     */
    List<ObjectRow> searchObjects(String library, String objectType);

    /**
     * 对象详情（DSPOBJD 属性）。对象不存在或查询失败返回 null。
     */
    ObjectDetail objectDetail(String library, String objectName);

    /**
     * 对象引用分析（DSPPGMREF）：
     * direction=IN 返回引用该对象的对象列表（被谁引用）；direction=OUT 返回该对象引用的对象列表。
     */
    List<ObjectRefRow> objectReferences(String library, String objectName, String direction);

    /**
     * 对象权限列表（2.3.4，DSPOBJAUT）。不可达或查询失败返回空列表。
     */
    List<AuthorityRow> objectAuthorities(String library, String objectName);

    /**
     * 库级调用拓扑（3.7，DSPPGMREF 聚合）。不可达或失败返回空图（不抛异常）。
     */
    GraphData objectGraph(String library);
}
