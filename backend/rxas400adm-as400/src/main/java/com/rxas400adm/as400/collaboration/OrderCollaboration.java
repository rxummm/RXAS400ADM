package com.rxas400adm.as400.collaboration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rx_order_collaboration")
public class OrderCollaboration {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String customerCode;
    private String customerName;
    /** PENDING / IN_PROGRESS / COMPLETED / CANCELLED */
    private String status;
    /** LOW / NORMAL / HIGH / URGENT */
    private String priority;
    private String assignedTo;
    private LocalDate dueDate;
    private String notes;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
