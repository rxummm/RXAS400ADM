package com.rxas400adm.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.as400.controller.As400Controller;
import com.rxas400adm.as400.dto.IbmiSystemDTO;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.report.IReportScheduleService;
import com.rxas400adm.report.ReportScheduleController;
import com.rxas400adm.report.dto.ReportScheduleDTO;
import com.rxas400adm.system.controller.DocController;
import com.rxas400adm.system.controller.NoticeController;
import com.rxas400adm.system.controller.SysRoleController;
import com.rxas400adm.system.dto.DocDTO;
import com.rxas400adm.system.dto.NoticeDTO;
import com.rxas400adm.system.dto.SysRoleDTO;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.entity.Notice;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.service.IDocService;
import com.rxas400adm.system.service.INoticeService;
import com.rxas400adm.system.service.IRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DTO 防伪造安全回归（R2 迁移后加固）：
 * 写接口的 @RequestBody 均为 *DTO，且 DTO 物理上不含 id/createdBy/status(服务端托管) 等内部字段
 * （编译期即不可伪造）。生产环境 Jackson FAIL_ON_UNKNOWN_PROPERTIES=false（Spring Boot 默认），
 * 伪造的内部字段会被静默忽略——本测试锁定该契约：
 *   ① DTO 类不含内部字段（反射断言，防后续误加）；
 *   ② 含伪造字段的请求体能正常解析，业务字段正常绑定进 DTO。
 */
class DtoForgeGuardTest {

    /** standalone MockMvc：ObjectMapper 镜像 Boot 默认（未知属性静默忽略） */
    private MockMvc mockMvc(Object controller) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /** 断言 DTO 物理上不含指定字段（防伪造内部字段被误加回 DTO） */
    private void assertFieldAbsent(Class<?> dtoClass, String field) {
        assertThrows(NoSuchFieldException.class, () -> dtoClass.getDeclaredField(field),
                "DTO " + dtoClass.getSimpleName() + " 不得含内部字段 " + field);
    }

    @Test
    void noticeDTO_hasNoInternalFields() {
        assertFieldAbsent(NoticeDTO.class, "id");
        assertFieldAbsent(NoticeDTO.class, "createdBy");
        assertFieldAbsent(NoticeDTO.class, "publishedTime");
        assertFieldAbsent(NoticeDTO.class, "createdTime");
        assertFieldAbsent(NoticeDTO.class, "updatedTime");
    }

    @Test
    void reportScheduleDTO_hasNoInternalFields() {
        assertFieldAbsent(ReportScheduleDTO.class, "id");
        assertFieldAbsent(ReportScheduleDTO.class, "status");
        assertFieldAbsent(ReportScheduleDTO.class, "createdBy");
        assertFieldAbsent(ReportScheduleDTO.class, "lastRunTime");
        assertFieldAbsent(ReportScheduleDTO.class, "lastResult");
    }

    @Test
    void ibmiSystemDTO_hasNoInternalFields() {
        assertFieldAbsent(IbmiSystemDTO.class, "id");
        assertFieldAbsent(IbmiSystemDTO.class, "status");
        assertFieldAbsent(IbmiSystemDTO.class, "connectionStatus");
        assertFieldAbsent(IbmiSystemDTO.class, "createdTime");
    }

    @Test
    void docDTO_hasNoInternalFields() {
        assertFieldAbsent(DocDTO.class, "id");
        assertFieldAbsent(DocDTO.class, "version");
        assertFieldAbsent(DocDTO.class, "status");
        assertFieldAbsent(DocDTO.class, "deleted");
        assertFieldAbsent(DocDTO.class, "createdBy");
        assertFieldAbsent(DocDTO.class, "updatedBy");
        assertFieldAbsent(DocDTO.class, "approvedBy");
        assertFieldAbsent(DocDTO.class, "approvedTime");
    }

    @Test
    void sysRoleDTO_hasNoInternalFields() {
        assertFieldAbsent(SysRoleDTO.class, "id");
    }

    @Test
    void noticeCreate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        INoticeService service = mock(INoticeService.class);
        when(service.create(any(NoticeDTO.class), eq("anonymous"))).thenReturn(new Notice());

        mockMvc(new NoticeController(service))
                .perform(post("/api/v1/notices")
                        .contentType("application/json")
                        .content("{\"id\":999,\"title\":\"安全公告\",\"content\":\"内容\","
                                + "\"createdBy\":\"hacker\",\"publishedTime\":\"2020-01-01T00:00:00\","
                                + "\"createdTime\":\"2020-01-01T00:00:00\",\"status\":5}"))
                .andExpect(status().isOk());

        ArgumentCaptor<NoticeDTO> captor = ArgumentCaptor.forClass(NoticeDTO.class);
        verify(service).create(captor.capture(), eq("anonymous"));
        NoticeDTO dto = captor.getValue();
        assertEquals("安全公告", dto.getTitle(), "业务字段正常绑定");
        assertEquals(5, dto.getStatus(), "status 为公告可写字段（发布语义），应正常绑定");
    }

    @Test
    void reportScheduleCreate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        IReportScheduleService service = mock(IReportScheduleService.class);
        when(service.create(any(ReportScheduleDTO.class), eq("anonymous")))
                .thenReturn(new com.rxas400adm.report.ReportSchedule());

        mockMvc(new ReportScheduleController(service))
                .perform(post("/api/v1/report-schedules")
                        .contentType("application/json")
                        .content("{\"id\":999,\"name\":\"周报\",\"reportType\":\"DAILY\","
                                + "\"cronExpr\":\"0 0 9 * * ?\",\"status\":\"PUBLISHED\","
                                + "\"createdBy\":\"hacker\",\"lastRunTime\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReportScheduleDTO> captor = ArgumentCaptor.forClass(ReportScheduleDTO.class);
        verify(service).create(captor.capture(), eq("anonymous"));
        ReportScheduleDTO dto = captor.getValue();
        assertEquals("周报", dto.getName());
        assertEquals("DAILY", dto.getReportType());
        assertEquals("0 0 9 * * ?", dto.getCronExpr());
    }

    @Test
    void ibmiSystemCreate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        IIbmiSystemService service = mock(IIbmiSystemService.class);
        when(service.create(any(IbmiSystemDTO.class))).thenReturn(new IbmiSystem());

        mockMvc(new As400Controller(service))
                .perform(post("/api/v1/as400/systems")
                        .contentType("application/json")
                        .content("{\"id\":999,\"name\":\"US400CND\",\"host\":\"10.0.0.1\",\"port\":8470,"
                                + "\"username\":\"admin\",\"status\":\"ONLINE\","
                                + "\"connectionStatus\":\"UP\",\"createdTime\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<IbmiSystemDTO> captor = ArgumentCaptor.forClass(IbmiSystemDTO.class);
        verify(service).create(captor.capture());
        IbmiSystemDTO dto = captor.getValue();
        assertEquals("US400CND", dto.getName());
        assertEquals("10.0.0.1", dto.getHost());
        assertEquals("admin", dto.getUsername());
    }

    @Test
    void docCreate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        IDocService service = mock(IDocService.class);
        when(service.createDoc(any(DocDTO.class), eq("anonymous"))).thenReturn(new Doc());

        mockMvc(new DocController(service))
                .perform(post("/api/v1/docs")
                        .contentType("application/json")
                        .content("{\"id\":999,\"templateId\":1,\"title\":\"变更单\",\"content\":\"正文\","
                                + "\"version\":99,\"status\":\"PUBLISHED\",\"deleted\":1,"
                                + "\"createdBy\":\"hacker\",\"approvedBy\":\"hacker\","
                                + "\"approvedTime\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<DocDTO> captor = ArgumentCaptor.forClass(DocDTO.class);
        verify(service).createDoc(captor.capture(), eq("anonymous"));
        DocDTO dto = captor.getValue();
        assertEquals("变更单", dto.getTitle());
        assertEquals("正文", dto.getContent());
    }

    @Test
    void docUpdate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        IDocService service = mock(IDocService.class);
        when(service.updateDoc(eq(7L), any(DocDTO.class), eq("anonymous"))).thenReturn(new Doc());

        mockMvc(new DocController(service))
                .perform(put("/api/v1/docs/7")
                        .contentType("application/json")
                        .content("{\"templateId\":1,\"title\":\"v2\",\"content\":\"新内容\","
                                + "\"version\":99,\"status\":\"PUBLISHED\","
                                + "\"approvedBy\":\"hacker\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<DocDTO> captor = ArgumentCaptor.forClass(DocDTO.class);
        verify(service).updateDoc(eq(7L), captor.capture(), eq("anonymous"));
        DocDTO dto = captor.getValue();
        assertEquals("v2", dto.getTitle());
    }

    @Test
    void roleCreate_forgedInternalFields_ignored_businessFieldsBound() throws Exception {
        IRoleService service = mock(IRoleService.class);
        when(service.create(any(SysRoleDTO.class))).thenReturn(new SysRole());

        mockMvc(new SysRoleController(service))
                .perform(post("/api/v1/roles")
                        .contentType("application/json")
                        .content("{\"id\":999,\"roleCode\":\"DEVELOPER\",\"roleName\":\"开发\","
                                + "\"menuIds\":[1,2,3]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<SysRoleDTO> captor = ArgumentCaptor.forClass(SysRoleDTO.class);
        verify(service).create(captor.capture());
        SysRoleDTO dto = captor.getValue();
        assertEquals("DEVELOPER", dto.getRoleCode());
        assertEquals(3, dto.getMenuIds().size(), "menuIds 为授权业务字段，正常绑定");
    }
}
