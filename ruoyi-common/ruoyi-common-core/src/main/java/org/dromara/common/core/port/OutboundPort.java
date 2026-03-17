package org.dromara.common.core.port;

import java.lang.annotation.*;

/**
 * 出站端口标记注解。
 *
 * <p>标注在应用层定义的出站端口接口上，表明该接口是系统与外部依赖（数据库、OSS、HTTP 服务等）
 * 之间的边界契约。其实现（Adapter）位于基础设施层。
 *
 * <p>用法示例：
 * <pre>{@code
 * @OutboundPort
 * public interface DocumentStoragePort {
 *     boolean ensureDirectory(String path);
 * }
 * }</pre>
 *
 * <p>配合 {@code @Component} 标注在 Adapter 实现类上：
 * <pre>{@code
 * @Component
 * public class OssDocumentStorageAdapter implements DocumentStoragePort { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OutboundPort {

    /**
     * 端口描述，便于文档生成和架构检查工具使用。
     */
    String value() default "";
}
