package com.rxas400adm.quality.vo;

import com.rxas400adm.quality.entity.TraceabilityChain;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TraceabilityChainVO {

    private Long id;
    private String itemCode;
    private String batchNo;
    private String traceType;
    private String sourceType;
    private String sourceNo;
    private String targetType;
    private String targetNo;
    private String relationship;
    private LocalDateTime createdTime;

    public static TraceabilityChainVO from(TraceabilityChain entity) {
        TraceabilityChainVO vo = new TraceabilityChainVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
