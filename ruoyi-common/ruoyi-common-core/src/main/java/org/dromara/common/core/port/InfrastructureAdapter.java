package org.dromara.common.core.port;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 基础设施适配器标记注解。
 *
 * <p>标注在实现 {@link OutboundPort} 接口的适配器类上，表明该类是某个出站端口的基础设施实现。
 * 含 {@link Component} 语义，Spring 自动注册为 Bean。
 *
 * <p>用法示例：
 * <pre>{@code
 * @InfrastructureAdapter("S3/OSS 文档存储")
 * public class OssDocumentStorageAdapter implements DocumentStoragePort { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface InfrastructureAdapter {

    /**
     * 适配器描述，便于架构检查工具使用。
     */
    String value() default "";
}
