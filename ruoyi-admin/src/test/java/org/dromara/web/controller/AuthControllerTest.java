package org.dromara.web.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.RegisterBody;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.system.service.ISysClientService;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysSocialService;
import org.dromara.system.service.ISysTenantService;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.SysRegisterService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @BeforeAll
    static void installMinimalSpringUtilsContext() {
        // 该项目的 JsonUtils / ValidatorUtils / MessageUtils 都在 static 字段里依赖 SpringUtils.getBean(...)
        // 这里用一个最小 StaticApplicationContext 注入 Hutool SpringUtil，避免引入完整 Spring Boot 上下文。
        StaticApplicationContext ctx = new StaticApplicationContext();

        StaticMessageSource messageSource;
        Object existing = ctx.getBeanFactory().getSingleton("messageSource");
        if (existing instanceof StaticMessageSource sms) {
            messageSource = sms;
        } else {
            messageSource = new StaticMessageSource();
            ctx.getBeanFactory().registerSingleton("messageSource", messageSource);
        }
        messageSource.addMessage("auth.grant.type.error", Locale.SIMPLIFIED_CHINESE, "认证授权类型错误!");
        messageSource.addMessage("auth.grant.type.error", Locale.CHINA, "认证授权类型错误!");
        messageSource.addMessage("auth.grant.type.error", Locale.getDefault(), "认证授权类型错误!");

        ctx.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ctx.getBeanFactory().registerSingleton("validator", validator);

        injectIntoHutoolSpringUtil(ctx);

        // 触发相关工具类初始化，确保 static bean 绑定成功。
        MessageUtils.message("auth.grant.type.error");
        ValidatorUtils.validate(new Object());
    }

    private static void injectIntoHutoolSpringUtil(StaticApplicationContext ctx) {
        ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
        Class<?> type = cn.hutool.extra.spring.SpringUtil.class;
        while (type != null) {
            for (Field f : type.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    if (StaticApplicationContext.class.isAssignableFrom(f.getType())
                        || org.springframework.context.ApplicationContext.class.isAssignableFrom(f.getType())) {
                        f.set(null, ctx);
                    } else if (ConfigurableListableBeanFactory.class.isAssignableFrom(f.getType())) {
                        f.set(null, beanFactory);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to inject Hutool SpringUtil context", e);
                }
            }
            type = type.getSuperclass();
        }
    }

    @Mock
    private SocialProperties socialProperties;

    @Mock
    private SysLoginService loginService;

    @Mock
    private SysRegisterService registerService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private ISysTenantService tenantService;

    @Mock
    private ISysSocialService socialUserService;

    @Mock
    private ISysClientService clientService;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @InjectMocks
    private AuthController controller;

    @Tag("dev")
    @Tag("prod")
    @Tag("local")
    @Test
    void shouldFailLoginWhenClientIdIsInvalid() {
        String body = """
            {
              "tenantId": "000000",
              "clientId": "invalid-client",
              "grantType": "password"
            }
            """;

        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        try {
            R<LoginVo> result = controller.login(body);

            assertEquals(R.FAIL, result.getCode());
            assertEquals("认证授权类型错误!", result.getMsg());
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Tag("dev")
    @Tag("prod")
    @Tag("local")
    @Test
    void shouldRejectRegisterWhenRegisterSwitchIsOff() {
        when(configService.selectRegisterEnabled("tenant-x")).thenReturn(false);

        RegisterBody body = new RegisterBody();
        body.setTenantId("tenant-x");

        R<Void> result = controller.register(body);

        assertEquals(R.FAIL, result.getCode());
        assertEquals("当前系统没有开启注册功能！", result.getMsg());
        verify(registerService, never()).register(body);
    }
}
