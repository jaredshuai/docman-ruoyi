package org.dromara.docman.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.vo.DocProjectVo;

public interface DocProjectMapper extends BaseMapperPlus<DocProject, DocProjectVo> {

    /**
     * 分页查询用户可访问的项目（JOIN优化，消除N+1）
     *
     * @param page 分页参数
     * @param userId 用户ID（非超级管理员时使用）
     * @param isSuperAdmin 是否超级管理员
     * @param wrapper 查询条件
     * @return 分页结果
     */
    Page<DocProjectVo> selectAccessibleProjectVoPage(
        Page<DocProjectVo> page,
        @Param("userId") Long userId,
        @Param("isSuperAdmin") boolean isSuperAdmin,
        @Param("ew") LambdaQueryWrapper<DocProject> wrapper
    );
}
