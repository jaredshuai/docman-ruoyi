package org.dromara.docman.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface DocDashboardMapper extends BaseMapperPlus<DocProject, DocProject> {

    List<DocProjectProgressVo> selectProjectProgress(@Param("projectIds") List<Long> projectIds);

    List<DocDeadlineAlertVo> selectDeadlineAlerts(@Param("projectIds") List<Long> projectIds,
                                                  @Param("today") LocalDate today,
                                                  @Param("deadlineBefore") LocalDate deadlineBefore);

    List<DocPluginStatsVo> selectPluginStats(@Param("projectIds") List<Long> projectIds,
                                             @Param("createdAfter") Timestamp createdAfter);
}
