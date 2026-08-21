package com.rxas400adm.as400.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.as400.entity.JobSla;
import org.apache.ibatis.annotations.Mapper;

/** 作业 SLA 规则 Mapper（rx_job_sla） */
@Mapper
public interface JobSlaMapper extends BaseMapper<JobSla> {
}
