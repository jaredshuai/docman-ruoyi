package org.dromara.web.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mail.config.properties.MailProperties;
import org.dromara.common.web.config.properties.CaptchaProperties;
import org.dromara.web.domain.vo.CaptchaVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaptchaControllerTest {

    @Tag("dev")
    @Tag("prod")
    @Tag("local")
    @Test
    void shouldReturnDisabledCaptchaWhenCaptchaSwitchIsOff() {
        CaptchaProperties captchaProperties = new CaptchaProperties();
        captchaProperties.setEnable(false);
        MailProperties mailProperties = new MailProperties();

        CaptchaController controller = new CaptchaController(captchaProperties, mailProperties);

        R<CaptchaVo> result = controller.getCode();

        assertEquals(R.SUCCESS, result.getCode());
        assertFalse(result.getData().getCaptchaEnabled());
        assertNull(result.getData().getUuid());
        assertNull(result.getData().getImg());
    }

    @Tag("dev")
    @Tag("prod")
    @Tag("local")
    @Test
    void shouldReturnFailWhenMailFunctionIsDisabled() {
        CaptchaProperties captchaProperties = new CaptchaProperties();
        captchaProperties.setEnable(false);
        MailProperties mailProperties = new MailProperties();
        mailProperties.setEnabled(false);

        CaptchaController controller = new CaptchaController(captchaProperties, mailProperties);

        R<Void> result = controller.emailCode("demo@example.com");

        assertEquals(R.FAIL, result.getCode());
        assertEquals("当前系统没有开启邮箱功能！", result.getMsg());
    }
}
