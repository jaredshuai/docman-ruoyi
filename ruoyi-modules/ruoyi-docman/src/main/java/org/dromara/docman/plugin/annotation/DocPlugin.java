package org.dromara.docman.plugin.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 文档插件注解，标注在 DocumentPlugin 实现类上
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface DocPlugin {
    /** 插件ID，全局唯一 */
    String value();
}
