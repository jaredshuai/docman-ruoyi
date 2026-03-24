package org.dromara.docman.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocNodeDeadline;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;

import java.time.LocalDate;
import java.util.List;

public interface DocNodeDeadlineMapper extends BaseMapperPlus<DocNodeDeadline, DocNodeDeadlineVo> {

    List<DocNodeDeadlineVo> selectDeadlineList(@Param("projectId") Long projectId);

    List<DocNodeDeadlineVo> selectApproachingDeadlines(@Param("deadlineBefore") LocalDate deadlineBefore,
                                                       @Param("maxReminderCount") int maxReminderCount);
}
