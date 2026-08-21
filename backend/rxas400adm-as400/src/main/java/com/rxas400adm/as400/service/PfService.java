package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * PF 物理文件浏览（2.4.5，参照旧项目 pfBrowse）：文件列表 / 字段 / 记录查看。
 * 数据源按 X-AS400-Server 路由。M2：文件/字段返回 record，记录数据保持 Map（动态列）。
 */
@Service
@RequiredArgsConstructor
public class PfService implements IPfService {

    private final AS400ClientProvider clientProvider;

    public List<PfRow> files(String library) {
        return clientProvider.current().listPfFiles(library);
    }

    public List<PfColumnRow> columns(String library, String file) {
        return clientProvider.current().pfColumns(library, file);
    }

    public List<Map<String, Object>> data(String library, String file, int limit) {
        return clientProvider.current().pfData(library, file, limit);
    }
}
