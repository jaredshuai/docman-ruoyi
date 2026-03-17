package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocDocumentApplicationService;
import org.dromara.docman.application.service.DocDocumentQueryApplicationService;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/document")
public class DocDocumentRecordController extends BaseController {

    private final DocDocumentApplicationService documentApplicationService;
    private final DocDocumentQueryApplicationService documentQueryApplicationService;

    @SaCheckPermission("docman:document:list")
    @GetMapping("/list")
    public TableDataInfo<DocDocumentRecordVo> list(@RequestParam Long projectId, PageQuery pageQuery) {
        return documentQueryApplicationService.list(projectId, pageQuery);
    }

    @SaCheckPermission("docman:document:query")
    @GetMapping("/{id}")
    public R<DocDocumentRecordVo> getInfo(@PathVariable Long id) {
        return R.ok(documentQueryApplicationService.getById(id));
    }

    @SaCheckPermission("docman:document:upload")
    @Log(title = "文档上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<Void> upload(@Validated @RequestBody DocDocumentRecordBo bo) {
        documentApplicationService.upload(bo);
        return R.ok();
    }
}
