package com.rxas400adm.as400.service;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.common.response.PageResult;

import java.util.List;

/**
 * 对象搜索 / 详情 / 引用分析（DSPOBJD / DSPPGMREF / QSYS2.OBJECT_STATISTICS）。
 * 数据源按 X-AS400-Server 头路由。
 */
public interface IObjectService {

    PageResult<ObjectRow> searchObjects(String library, String objectType, String keyword, int current, int size);

    ObjectDetail objectDetail(String library, String objectName);

    List<ObjectRefRow> objectReferences(String library, String objectName, String direction);

    List<AuthorityRow> objectAuthorities(String library, String objectName);
}
