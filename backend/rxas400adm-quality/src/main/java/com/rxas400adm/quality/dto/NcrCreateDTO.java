package com.rxas400adm.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "NCR创建请求体")
public class NcrCreateDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "公司代码")
    private String cono;

    @Schema(description = "关联检验记录ID")
    private Long inspectionId;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "物料编码")
    private String itemCode;

    @Schema(description = "物料描述")
    private String itemDesc;

    @Schema(description = "批次号")
    private String batchNo;

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "不合格数量")
    private BigDecimal qtyRejected;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "缺陷类型")
    private String defectType;

    @Schema(description = "缺陷描述")
    private String defectDescription;

    @Schema(description = "处置方式：USE_AS_IS/REWORK/SCRAP/RETURN")
    private String disposition;

    @Schema(description = "处置日期")
    private LocalDate dispositionDate;

    @Schema(description = "根本原因")
    private String rootCause;

    @Schema(description = "纠正措施")
    private String correctiveAction;

    @Schema(description = "预防措施")
    private String preventiveAction;

    @Schema(description = "负责人")
    private String assignedTo;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "备注")
    private String remark;
}
