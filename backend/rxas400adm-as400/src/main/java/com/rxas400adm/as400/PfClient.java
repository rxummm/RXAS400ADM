package com.rxas400adm.as400;

import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;

import java.util.List;
import java.util.Map;

/**
 * PF 物理文件域：文件列表 / 字段定义 / 记录分页查看。
 */
public interface PfClient {

    /**
     * PF 物理文件列表（2.4.5）。
     */
    List<PfRow> listPfFiles(String library);

    /**
     * PF 字段列表（2.4.5）。
     */
    List<PfColumnRow> pfColumns(String library, String file);

    /**
     * PF 记录分页查看（2.4.5）：返回前 limit 行（每行列名→值，动态列保持 Map）。
     */
    List<Map<String, Object>> pfData(String library, String file, int limit);
}
