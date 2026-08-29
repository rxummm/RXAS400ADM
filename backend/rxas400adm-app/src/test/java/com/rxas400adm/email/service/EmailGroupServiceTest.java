package com.rxas400adm.email.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.email.dto.EmailGroupCreateDTO;
import com.rxas400adm.email.dto.EmailRecipientDTO;
import com.rxas400adm.email.entity.EmailRecipient;
import com.rxas400adm.email.entity.EmailRecipientGroup;
import com.rxas400adm.email.mapper.EmailRecipientGroupMapper;
import com.rxas400adm.email.mapper.EmailRecipientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailGroupService 测试")
class EmailGroupServiceTest {

    @Mock
    private EmailRecipientGroupMapper groupMapper;
    @Mock
    private EmailRecipientMapper recipientMapper;

    private EmailGroupService service;

    @BeforeEach
    void setUp() {
        service = new EmailGroupService(groupMapper, recipientMapper);
    }

    @Test
    @DisplayName("create() — 正常创建分组")
    void create_normal() {
        EmailGroupCreateDTO dto = new EmailGroupCreateDTO();
        dto.setGroupName("运维组");
        dto.setDescription("运维团队");
        when(groupMapper.insert(any(EmailRecipientGroup.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.create(dto));

        ArgumentCaptor<EmailRecipientGroup> captor = ArgumentCaptor.forClass(EmailRecipientGroup.class);
        verify(groupMapper).insert(captor.capture());
        assertEquals("运维组", captor.getValue().getGroupName());
    }

    @Test
    @DisplayName("delete() — 正常删除分组及成员")
    void delete_normal() {
        EmailRecipientGroup group = new EmailRecipientGroup();
        group.setId(1L);
        when(groupMapper.selectById(1L)).thenReturn(group);
        when(groupMapper.deleteById(1L)).thenReturn(1);
        when(recipientMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);

        assertDoesNotThrow(() -> service.delete(1L));

        verify(groupMapper).deleteById(1L);
        verify(recipientMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("delete() — 不存在抛异常")
    void delete_notExists_throws() {
        when(groupMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(99L));
    }

    @Test
    @DisplayName("addMember() — 正常添加成员")
    void addMember_normal() {
        EmailRecipientGroup group = new EmailRecipientGroup();
        group.setId(1L);
        when(groupMapper.selectById(1L)).thenReturn(group);
        when(recipientMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(recipientMapper.insert(any(EmailRecipient.class))).thenReturn(1);

        EmailRecipientDTO dto = new EmailRecipientDTO();
        dto.setEmail("test@example.com");
        assertDoesNotThrow(() -> service.addMember(1L, dto));

        verify(recipientMapper).insert(any(EmailRecipient.class));
    }

    @Test
    @DisplayName("addMember() — 重复邮箱抛异常")
    void addMember_duplicate_throws() {
        EmailRecipientGroup group = new EmailRecipientGroup();
        group.setId(1L);
        when(groupMapper.selectById(1L)).thenReturn(group);
        when(recipientMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        EmailRecipientDTO dto = new EmailRecipientDTO();
        dto.setEmail("test@example.com");
        assertThrows(BusinessException.class, () -> service.addMember(1L, dto));
    }

    @Test
    @DisplayName("members() — 返回分组成员列表")
    void members_normal() {
        EmailRecipient r = new EmailRecipient();
        r.setId(1L);
        r.setEmail("a@test.com");
        when(recipientMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r));

        List<EmailRecipient> result = service.members(1L);

        assertEquals(1, result.size());
        assertEquals("a@test.com", result.get(0).getEmail());
    }
}