package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.docman.service.IDocNasService;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.mapper.SysOssMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocNasServiceImpl implements IDocNasService {

    private final SysOssMapper sysOssMapper;

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

    /**
     * 上传文件到 NAS 并创建 sys_oss 记录
     *
     * @param nasPath   NAS 存储路径（如 /项目文档/2026/电信/项目名/文件.pdf）
     * @param fileBytes 文件字节数组
     * @param fileName  原始文件名
     * @return 上传成功返回 ossId，失败返回 null
     */
    @Override
    public Long uploadFile(String nasPath, byte[] fileBytes, String fileName) {
        try {
            OssClient client = OssFactory.instance();
            String key = nasPath.startsWith("/") ? nasPath.substring(1) : nasPath;

            // 上传文件到 OSS
            UploadResult uploadResult = client.upload(
                new ByteArrayInputStream(fileBytes),
                key,
                (long) fileBytes.length,
                "application/octet-stream"
            );

            // 创建 sys_oss 记录
            SysOss oss = new SysOss();
            oss.setUrl(uploadResult.getUrl());
            oss.setFileName(uploadResult.getFilename());
            oss.setOriginalName(fileName);

            // 提取文件后缀
            String suffix = StringUtils.substring(fileName, fileName.lastIndexOf("."), fileName.length());
            oss.setFileSuffix(suffix);
            oss.setService(client.getConfigKey());

            // 设置扩展信息
            SysOssExt ext = new SysOssExt();
            ext.setFileSize((long) fileBytes.length);
            ext.setContentType("application/octet-stream");
            oss.setExt1(JsonUtils.toJsonString(ext));

            // 插入数据库
            sysOssMapper.insert(oss);

            log.info("文件上传成功: nasPath={}, ossId={}", nasPath, oss.getOssId());
            return oss.getOssId();
        } catch (Exception e) {
            log.error("上传文件到NAS失败: {}", nasPath, e);
            return null;
        }
    }
}
