package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.entity.DocProjectType;
import org.dromara.docman.domain.vo.DocProjectTypeVo;
import org.dromara.docman.mapper.DocProjectTypeMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectTypeServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DocProjectType.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), DocProjectType.class);
        }
    }

    @Mock
    private DocProjectTypeMapper projectTypeMapper;

    @InjectMocks
    private DocProjectTypeServiceImpl service;

    @Test
    void listAll_shouldMapEntitiesToVoList() {
        Date now = new Date();
        DocProjectType telecom = createEntity(1L, "telecom", "电信项目", now);
        DocProjectType social = createEntity(2L, "social", "社会客户", now);
        when(projectTypeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(telecom, social));

        List<DocProjectTypeVo> result = service.listAll();

        assertEquals(2, result.size());
        assertEquals("telecom", result.get(0).getCode());
        assertEquals("电信项目", result.get(0).getName());
        assertEquals(now, result.get(0).getCreateTime());
        assertEquals("social", result.get(1).getCode());
        verify(projectTypeMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void queryById_shouldThrowWhenProjectTypeMissing() {
        when(projectTypeMapper.selectById(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.queryById(99L));

        assertEquals("项目类型不存在", ex.getMessage());
    }

    @Test
    void queryById_shouldMapEntityToVo() {
        Date now = new Date();
        DocProjectType entity = createEntity(8L, "telecom", "电信项目", now);
        when(projectTypeMapper.selectById(8L)).thenReturn(entity);

        DocProjectTypeVo result = service.queryById(8L);

        assertNotNull(result);
        assertEquals(8L, result.getId());
        assertEquals("telecom", result.getCode());
        assertEquals("电信项目", result.getName());
        assertEquals(now, result.getUpdateTime());
        verify(projectTypeMapper).selectById(8L);
    }

    private DocProjectType createEntity(Long id, String code, String name, Date time) {
        DocProjectType entity = new DocProjectType();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setCustomerType("enterprise");
        entity.setDescription(name + "说明");
        entity.setSortOrder(id.intValue());
        entity.setStatus("active");
        entity.setCreateTime(time);
        entity.setUpdateTime(time);
        return entity;
    }
}
