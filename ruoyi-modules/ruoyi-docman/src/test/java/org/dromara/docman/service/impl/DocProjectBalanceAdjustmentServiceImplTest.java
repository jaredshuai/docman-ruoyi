package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.domain.entity.DocProjectBalanceAdjustment;
import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;
import org.dromara.docman.mapper.DocProjectBalanceAdjustmentMapper;
import org.dromara.docman.mapper.DocProjectEstimateSnapshotMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectBalanceAdjustmentServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProjectBalanceAdjustment.class);
        initTableInfo(DocProjectEstimateSnapshot.class);
    }

    @Mock
    private DocProjectBalanceAdjustmentMapper balanceAdjustmentMapper;

    @Mock
    private DocProjectEstimateSnapshotMapper estimateSnapshotMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @InjectMocks
    private DocProjectBalanceAdjustmentServiceImpl service;

    @Test
    void shouldRejectSaveWhenEstimateSnapshotMissing() {
        DocProjectBalanceAdjustmentBo bo = new DocProjectBalanceAdjustmentBo();
        bo.setProjectId(1L);
        bo.setMaterialPrice(new BigDecimal("123"));

        when(estimateSnapshotMapper.selectCount(any())).thenReturn(0L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.save(bo));

        assertEquals("请先完成初步估算后再执行平料", ex.getMessage());
        verify(projectAccessService).assertAction(1L, DocProjectAction.EDIT_PROJECT);
    }

    @Test
    void queryLatest_shouldMapEntityToVo() {
        Date now = new Date();
        DocProjectBalanceAdjustment entity = new DocProjectBalanceAdjustment();
        entity.setId(7L);
        entity.setProjectId(2L);
        entity.setMaterialPrice(new BigDecimal("66.88"));
        entity.setBalanceRemark("latest");
        entity.setStatus("active");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        when(balanceAdjustmentMapper.selectOne(any())).thenReturn(entity);

        DocProjectBalanceAdjustmentVo vo = service.queryLatest(2L);

        assertNotNull(vo);
        assertEquals(7L, vo.getId());
        assertEquals(new BigDecimal("66.88"), vo.getMaterialPrice());
        assertEquals(now, vo.getCreateTime());
        verify(projectAccessService).assertAction(2L, DocProjectAction.VIEW_PROJECT);
    }

    @Test
    void shouldSaveWhenEstimateSnapshotExists() {
        DocProjectBalanceAdjustmentBo bo = new DocProjectBalanceAdjustmentBo();
        bo.setProjectId(2L);
        bo.setMaterialPrice(new BigDecimal("456"));
        bo.setBalanceRemark("done");

        ArgumentCaptor<LambdaQueryWrapper<DocProjectEstimateSnapshot>> snapshotWrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(estimateSnapshotMapper.selectCount(snapshotWrapperCaptor.capture())).thenReturn(1L);

        service.save(bo);

        ArgumentCaptor<DocProjectBalanceAdjustment> captor = ArgumentCaptor.forClass(DocProjectBalanceAdjustment.class);
        verify(balanceAdjustmentMapper).insert(captor.capture());
        verify(estimateSnapshotMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(projectAccessService).assertAction(2L, DocProjectAction.EDIT_PROJECT);
        assertEquals(2L, captor.getValue().getProjectId());
        assertEquals(new BigDecimal("456"), captor.getValue().getMaterialPrice());
        assertEquals("done", captor.getValue().getBalanceRemark());
        assertEquals("active", captor.getValue().getStatus());
    }

    @Test
    void shouldRejectCrossProjectUpdate() {
        DocProjectBalanceAdjustmentBo bo = new DocProjectBalanceAdjustmentBo();
        bo.setId(9L);
        bo.setProjectId(2L);
        bo.setMaterialPrice(new BigDecimal("456"));

        DocProjectBalanceAdjustment existing = new DocProjectBalanceAdjustment();
        existing.setId(9L);
        existing.setProjectId(3L);

        when(estimateSnapshotMapper.selectCount(any())).thenReturn(1L);
        when(balanceAdjustmentMapper.selectById(9L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.save(bo));

        assertEquals("不允许跨项目修改平料记录", ex.getMessage());
    }

    @Test
    void shouldAppendNewBalanceRecordWhenUpdatingExistingProjectBalance() {
        DocProjectBalanceAdjustmentBo bo = new DocProjectBalanceAdjustmentBo();
        bo.setId(9L);
        bo.setProjectId(2L);
        bo.setMaterialPrice(new BigDecimal("789"));
        bo.setBalanceRemark("append");

        DocProjectBalanceAdjustment existing = new DocProjectBalanceAdjustment();
        existing.setId(9L);
        existing.setProjectId(2L);

        when(estimateSnapshotMapper.selectCount(any())).thenReturn(1L);
        when(balanceAdjustmentMapper.selectById(9L)).thenReturn(existing);

        service.save(bo);

        verify(balanceAdjustmentMapper).insert(any(DocProjectBalanceAdjustment.class));
        verify(balanceAdjustmentMapper, never()).updateById(any(DocProjectBalanceAdjustment.class));
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}
