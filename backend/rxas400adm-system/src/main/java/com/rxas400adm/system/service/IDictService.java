package com.rxas400adm.system.service;

import com.rxas400adm.system.dto.DictItemDTO;
import com.rxas400adm.system.dto.DictTypeDTO;
import com.rxas400adm.system.entity.DictItem;
import com.rxas400adm.system.entity.DictType;

import java.util.List;

/**
 * 数据字典服务接口（rx_dict_type / rx_dict_item）。
 */
public interface IDictService {

    List<DictType> listTypes();

    DictType createType(DictTypeDTO type);

    DictType updateType(Long id, DictTypeDTO dto);

    void deleteType(Long id);

    List<DictItem> listItems(String typeCode);

    List<DictItem> enabledItems(String typeCode);

    DictItem createItem(DictItemDTO item);

    DictItem updateItem(Long id, DictItemDTO dto);

    void deleteItem(Long id);
}
