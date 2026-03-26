package org.dromara.docman.service.impl;

import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentReminderServiceImplTest {

    @Mock
    private IDocProcessConfigService processConfigService;

    @Mock
    private IDocProjectService projectService;

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @Mock
    private SystemMessagePort systemMessagePort;

    @InjectMocks
    private DocDocumentReminderServiceImpl service;

    @Nested
    @DisplayName("sendPendingReminders() 分支测试")
    class SendPendingReminders {

        @Test
        @DisplayName("无运行中的流程配置，返回 0")
        void shouldReturnZeroWhenNoRunningProcessConfigExists() {
            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of());

            int reminded = service.sendPendingReminders(3);

            assertEquals(0, reminded);
            verify(projectService, never()).listByIdsAndStatus(any(), any());
            verify(systemMessagePort, never()).publishToUsers(anyList(), any());
        }

        @Test
        @DisplayName("运行中的配置但项目ID为空，返回 0")
        void shouldReturnZeroWhenProjectIdsAreNull() {
            DocProcessConfig config = new DocProcessConfig();
            config.setProjectId(null);
            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of(config));

            int reminded = service.sendPendingReminders(3);

            assertEquals(0, reminded);
            verify(projectService, never()).listByIdsAndStatus(any(), any());
        }

        @Test
        @DisplayName("运行中的配置但无活跃项目，返回 0")
        void shouldReturnZeroWhenNoActiveProjects() {
            DocProcessConfig config = new DocProcessConfig();
            config.setProjectId(1L);
            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of(config));
            when(projectService.listByIdsAndStatus(any(), eq(DocProjectStatus.ACTIVE))).thenReturn(List.of());

            int reminded = service.sendPendingReminders(3);

            assertEquals(0, reminded);
            verify(documentRecordService, never()).listPendingCreatedBeforeByProjectIds(any(), any());
        }

        @Test
        @DisplayName("活跃项目但无超期待处理文档，返回 0")
        void shouldReturnZeroWhenNoOverduePendingRecords() {
            DocProcessConfig config = new DocProcessConfig();
            config.setProjectId(1L);
            DocProject project = new DocProject();
            project.setId(1L);
            project.setName("测试项目");
            project.setOwnerId(100L);

            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of(config));
            when(projectService.listByIdsAndStatus(any(), eq(DocProjectStatus.ACTIVE))).thenReturn(List.of(project));
            when(documentRecordService.listPendingCreatedBeforeByProjectIds(any(), any())).thenReturn(List.of());

            int reminded = service.sendPendingReminders(3);

            assertEquals(0, reminded);
            verify(systemMessagePort, never()).publishToUsers(anyList(), any());
        }

        @Test
        @DisplayName("正常发送提醒，返回已提醒数量")
        void shouldSendRemindersAndReturnCount() {
            DocProcessConfig config = new DocProcessConfig();
            config.setProjectId(1L);
            DocProject project = new DocProject();
            project.setId(1L);
            project.setName("测试项目");
            project.setOwnerId(100L);
            DocDocumentRecord record = new DocDocumentRecord();
            record.setProjectId(1L);

            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of(config));
            when(projectService.listByIdsAndStatus(any(), eq(DocProjectStatus.ACTIVE))).thenReturn(List.of(project));
            when(documentRecordService.listPendingCreatedBeforeByProjectIds(any(), any())).thenReturn(List.of(record));

            try (MockedStatic<org.dromara.common.redis.utils.RedisUtils> redisMock = mockStatic(org.dromara.common.redis.utils.RedisUtils.class)) {
                redisMock.when(() -> org.dromara.common.redis.utils.RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class)))
                    .thenReturn(true);

                int reminded = service.sendPendingReminders(3);

                assertEquals(1, reminded);
                verify(systemMessagePort).publishToUsers(eq(List.of(100L)), anyString());
            }
        }

        @Test
        @DisplayName("项目无负责人时跳过提醒")
        void shouldSkipWhenProjectHasNoOwner() {
            DocProcessConfig config = new DocProcessConfig();
            config.setProjectId(1L);
            DocProject project = new DocProject();
            project.setId(1L);
            project.setName("测试项目");
            project.setOwnerId(null);
            DocDocumentRecord record = new DocDocumentRecord();
            record.setProjectId(1L);

            when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of(config));
            when(projectService.listByIdsAndStatus(any(), eq(DocProjectStatus.ACTIVE))).thenReturn(List.of(project));
            when(documentRecordService.listPendingCreatedBeforeByProjectIds(any(), any())).thenReturn(List.of(record));

            int reminded = service.sendPendingReminders(3);

            assertEquals(0, reminded);
            verify(systemMessagePort, never()).publishToUsers(anyList(), any());
        }
    }
}
