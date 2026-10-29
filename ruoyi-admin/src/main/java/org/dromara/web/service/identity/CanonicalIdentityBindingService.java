package org.dromara.web.service.identity;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysSocialService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 只读的 canonical identity 绑定查询服务。
 * <p>
 * 复用 sys_social 绑定表，不创建、更新或删除任何绑定关系，也不访问外部身份提供方。
 */
@Service
@RequiredArgsConstructor
public class CanonicalIdentityBindingService {

    private final ISysSocialService sysSocialService;
    private final SysUserMapper sysUserMapper;

    /**
     * 根据规范化外部身份查找唯一且可用的本地用户。
     *
     * @return 本地用户标识；任何无法唯一确定或不可用的场景均返回 empty
     */
    public Optional<CanonicalIdentityBinding> resolve(String issuer, String subject, String tenantId) {
        return CanonicalIdentity.of(issuer, subject, tenantId)
            .flatMap(this::resolveBinding);
    }

    private Optional<CanonicalIdentityBinding> resolveBinding(CanonicalIdentity identity) {
        List<SysSocialVo> candidates = sysSocialService.selectByAuthId(identity.authId());
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        List<SysSocialVo> scoped = candidates.stream()
            .filter(candidate -> Objects.equals(identity.tenantId(), candidate.getTenantId()))
            .toList();
        if (scoped.size() != 1) {
            return Optional.empty();
        }
        SysSocialVo binding = scoped.get(0);
        if (binding.getUserId() == null) {
            return Optional.empty();
        }
        SysUserVo user = sysUserMapper.selectVoById(binding.getUserId());
        if (user == null
            || SystemConstants.DISABLE.equals(user.getStatus())
            || !Objects.equals(identity.tenantId(), user.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(new CanonicalIdentityBinding(user.getUserId(), user.getTenantId()));
    }
}
