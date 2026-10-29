package org.dromara.web.service.identity.oidc;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * 标准 audience 校验。
 * <p>
 * 要求 {@code aud} 包含期望的 client_id；当 {@code aud} 含多个值时，按 OIDC 规范要求 {@code azp}
 * 必须显式等于期望 client_id。当 {@code azp} 出现但与本应用不匹配时同样拒绝。
 */
final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN = "invalid_token";
    private static final String AZP = "azp";

    private final String expectedAudience;

    AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences == null || !audiences.contains(expectedAudience)) {
            return failure("ID Token audience 不匹配");
        }
        String authorizedParty = jwt.getClaimAsString(AZP);
        if (audiences.size() > 1) {
            if (authorizedParty == null || !expectedAudience.equals(authorizedParty)) {
                return failure("ID Token 含多个 audience 时 azp 缺失或不匹配");
            }
        } else if (authorizedParty != null && !expectedAudience.equals(authorizedParty)) {
            return failure("ID Token azp 不匹配");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(INVALID_TOKEN, description, null));
    }
}
