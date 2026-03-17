package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.docman.service.IDocNasService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocNasServiceImpl implements IDocNasService {

    private static final Map<String, String> CUSTOMER_TYPE_MAP = Map.of(
        "telecom", "电信",
        "social", "社会客户"
    );

    @Override
    public String buildProjectBasePath(String customerType, String projectName) {
        int year = LocalDate.now().getYear();
        String customerLabel = CUSTOMER_TYPE_MAP.getOrDefault(customerType, customerType);
        return String.format("/项目文档/%d/%s/%s", year, customerLabel, projectName);
    }

    @Override
    public boolean createProjectDirectory(String basePath) {
        try {
            OssClient client = OssFactory.instance();
            // S3 协议通过上传空对象模拟目录创建
            String key = basePath.startsWith("/") ? basePath.substring(1) : basePath;
            key = key + "/.keep";
            client.upload(new ByteArrayInputStream(new byte[0]), key, 0L, "application/octet-stream");
            return true;
        } catch (Exception e) {
            log.error("创建NAS项目目录失败: {}", basePath, e);
            return false;
        }
    }

    @Override
    public boolean createNodeDirectory(String basePath, String folderName) {
        return createProjectDirectory(basePath + "/" + folderName);
    }

    @Override
    public Long uploadFile(String nasPath, byte[] fileBytes, String fileName) {
        try {
            OssClient client = OssFactory.instance();
            String key = nasPath.startsWith("/") ? nasPath.substring(1) : nasPath;
            // TODO: 确认如何获取 sys_oss 记录ID，当前 OssClient.upload 返回 UploadResult(url, filename, eTag)
            // 需要在 SysOssService 层创建 sys_oss 记录并返回 ossId
            client.upload(new ByteArrayInputStream(fileBytes), key, (long) fileBytes.length, "application/octet-stream");
            return null; // TODO: 返回 sys_oss 记录的 ossId
        } catch (Exception e) {
            log.error("上传文件到NAS失败: {}", nasPath, e);
            return null;
        }
    }
}
