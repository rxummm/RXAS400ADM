package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.IbmiSystemDTO;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.common.crypto.AesCryptoService;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IbmiSystemServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, IbmiSystem.class);
    }

    @Mock private IbmiSystemMapper ibmiSystemMapper;
    @Mock private AS400ClientProvider clientProvider;
    @Mock private AesCryptoService aesCryptoService;
    @Mock private DangerousClCommandValidator clValidator;
    @Mock private AS400Client as400Client;

    private IbmiSystemService service;

    @BeforeEach
    void setUp() {
        service = new IbmiSystemService(ibmiSystemMapper, clientProvider, aesCryptoService, clValidator);
    }

    @Test
    @DisplayName("list → 返回所有服务器")
    void list_shouldReturnAll() {
        IbmiSystem sys = new IbmiSystem();
        sys.setId(1L);
        sys.setName("PROD400");
        when(ibmiSystemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sys));

        List<IbmiSystem> result = service.list();
        assertEquals(1, result.size());
        assertEquals("PROD400", result.get(0).getName());
    }

    @Test
    @DisplayName("get → 不存在抛异常")
    void get_notExists_shouldThrow() {
        when(ibmiSystemMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.get(999L));
    }

    @Test
    @DisplayName("get → 存在返回服务器")
    void get_exists_shouldReturn() {
        IbmiSystem sys = new IbmiSystem();
        sys.setId(1L);
        when(ibmiSystemMapper.selectById(1L)).thenReturn(sys);
        assertNotNull(service.get(1L));
    }

    @Test
    @DisplayName("create → name 重复抛异常")
    void create_duplicateName_shouldThrow() {
        when(ibmiSystemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        IbmiSystemDTO dto = new IbmiSystemDTO();
        dto.setName("PROD400");
        dto.setHost("10.0.0.1");
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create → 正常新增")
    void create_valid_shouldInsert() {
        when(ibmiSystemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ibmiSystemMapper.insert(any(IbmiSystem.class))).thenAnswer(inv -> {
            inv.getArgument(0, IbmiSystem.class).setId(1L);
            return 1;
        });

        IbmiSystemDTO dto = new IbmiSystemDTO();
        dto.setName("TEST400");
        dto.setHost("10.0.0.1");
        dto.setPort(8470);
        dto.setUsername("admin");
        dto.setPasswordEncrypt("pass");

        IbmiSystem result = service.create(dto);
        assertEquals("TEST400", result.getName());
    }

    @Test
    @DisplayName("delete → 删除服务器并缓存失效")
    void delete_shouldDeleteAndEvict() {
        service.delete(1L);
        verify(ibmiSystemMapper).deleteById(1L);
        verify(clientProvider).evict(1L);
    }
}
