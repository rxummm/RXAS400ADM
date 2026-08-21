package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.RegionDTO;
import com.rxas400adm.system.entity.Region;

import java.util.List;

/**
 * 行政区划服务接口（rx_region）。
 */
public interface IRegionService {

    List<Region> children(String parentCode);

    PageResult<Region> page(int current, int size, String keyword, Integer level, String parentCode);

    List<Region> search(String keyword, Integer level);

    Region create(RegionDTO region);

    Region update(Long id, RegionDTO dto);

    void delete(Long id);
}
