package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.assembler.DocDocumentAssembler;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.enums.DocDocumentSourceType;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.service.DocDocumentStateMachine;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocDocumentRecordServiceImpl implements IDocDocumentRecordService {

    private final DocDocumentRecordMapper baseMapper;
    private final IDocProjectAccessService projectAccessService;
    private final DocDocumentAssembler documentAssembler;

    @Override
    public TableDataInfo<DocDocumentRecordVo> queryPageList(Long projectId, PageQuery pageQuery) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
        LambdaQueryWrapper<DocDocumentRecord> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DocDocumentRecord::getProjectId, projectId);
        lqw.orderByAsc(DocDocumentRecord::getNasPath);
        Page<DocDocumentRecordVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public DocDocumentRecordVo queryById(Long id) {
        DocDocumentRecord record = getRecordOrThrow(id);
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.VIEW_DOCUMENT);
        return baseMapper.selectVoById(id);
    }

    @Override
    public DocDocumentRecord queryEntityById(Long id) {
        DocDocumentRecord record = getRecordOrThrow(id);
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.VIEW_DOCUMENT);
        return record;
    }

    @Override
    public void recordUpload(DocDocumentRecordBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.UPLOAD_DOCUMENT);
        DocDocumentRecord record = documentAssembler.toEntity(bo);
        record.setSourceType(DocDocumentSourceType.UPLOAD.getCode());
        record.setStatus(DocDocumentStatus.GENERATED.getCode());
        record.setGeneratedAt(new Date());
        baseMapper.insert(record);
    }

    @Override
    public void recordPluginGenerated(Long projectId, String pluginId, PluginResult.GeneratedFile file) {
        DocDocumentRecord record = new DocDocumentRecord();
        record.setProjectId(projectId);
        record.setPluginId(pluginId);
        record.setSourceType(DocDocumentSourceType.PLUGIN.getCode());
        record.setFileName(file.getFileName());
        record.setNasPath(file.getNasPath());
        record.setOssId(file.getOssId());
        record.setStatus(DocDocumentStatus.GENERATED.getCode());
        record.setGeneratedAt(new Date());
        baseMapper.insert(record);
    }

    @Override
    public void markLatestUniquePluginArtifactsObsolete(Long projectId, String pluginId) {
        if (projectId == null || pluginId == null || pluginId.isBlank()) {
            return;
        }
        baseMapper.selectList(new LambdaQueryWrapper<DocDocumentRecord>()
                .eq(DocDocumentRecord::getProjectId, projectId)
                .eq(DocDocumentRecord::getPluginId, pluginId)
                .in(DocDocumentRecord::getStatus,
                    DocDocumentStatus.PENDING.getCode(),
                    DocDocumentStatus.RUNNING.getCode(),
                    DocDocumentStatus.GENERATED.getCode(),
                    DocDocumentStatus.FAILED.getCode()))
            .forEach(this::markObsolete);
    }

    @Override
    public void markObsoleteById(Long id) {
        DocDocumentRecord record = getRecordOrThrow(id);
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.EDIT_PROJECT);
        markObsolete(record);
    }

    @Override
    public void markObsoleteByProjectId(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        baseMapper.selectList(new LambdaQueryWrapper<DocDocumentRecord>()
                .eq(DocDocumentRecord::getProjectId, projectId)
                .in(DocDocumentRecord::getStatus, DocDocumentStatus.PENDING.getCode(), DocDocumentStatus.RUNNING.getCode(), DocDocumentStatus.GENERATED.getCode(), DocDocumentStatus.FAILED.getCode()))
            .forEach(this::markObsolete);
    }

    @Override
    public List<DocDocumentRecord> listPendingCreatedBeforeByProjectIds(List<Long> projectIds, Date cutoffTime) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectList(
            new LambdaQueryWrapper<DocDocumentRecord>()
                .in(DocDocumentRecord::getProjectId, projectIds)
                .eq(DocDocumentRecord::getStatus, DocDocumentStatus.PENDING.getCode())
                .le(DocDocumentRecord::getCreateTime, cutoffTime)
                .select(DocDocumentRecord::getProjectId)
        );
    }

    private DocDocumentRecord getRecordOrThrow(Long id) {
        DocDocumentRecord record = baseMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("文档记录不存在");
        }
        return record;
    }

    private void markObsolete(DocDocumentRecord record) {
        DocDocumentStateMachine.checkTransition(DocDocumentStatus.of(record.getStatus()), DocDocumentStatus.OBSOLETE);
        record.setStatus(DocDocumentStatus.OBSOLETE.getCode());
        baseMapper.updateById(record);
    }
}
