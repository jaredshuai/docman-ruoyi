package org.dromara.docman.service.impl;

import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.dromara.docman.mapper.DocProjectEstimateSnapshotMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectEstimateSnapshotServiceImplTest {

    @Mock
    private DocProjectEstimateSnapshotMapper estimateSnapshotMapper;

    @InjectMocks
    private DocProjectEstimateSnapshotServiceImpl service;

    @Test
    void queryLatest_shouldMapEntityToVo() {
        Date now = new Date();
        DocProjectEstimateSnapshot entity = new DocProjectEstimateSnapshot();
        entity.setId(9L);
        entity.setProjectId(2L);
        entity.setEstimateType("initial_estimate");
        entity.setEstimateAmount(new BigDecimal("123.45"));
        entity.setDrawingCount(3L);
        entity.setVisaCount(1L);
        entity.setStatus("mocked");
        entity.setSummary("summary");
        entity.setCreateTime(now);

        when(estimateSnapshotMapper.selectOne(any())).thenReturn(entity);

        DocProjectEstimateSnapshotVo vo = service.queryLatest(2L);

        assertNotNull(vo);
        assertEquals(9L, vo.getId());
        assertEquals("initial_estimate", vo.getEstimateType());
        assertEquals(new BigDecimal("123.45"), vo.getEstimateAmount());
        assertEquals(3L, vo.getDrawingCount());
        assertEquals(now, vo.getCreateTime());
    }
}
