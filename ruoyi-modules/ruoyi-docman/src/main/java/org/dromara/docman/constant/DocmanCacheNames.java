package org.dromara.docman.constant;

/**
 * Docman模块缓存名称常量
 * <p>
 * key 格式为 cacheNames#ttl#maxIdleTime#maxSize#local
 * <p>
 * ttl 过期时间 如果设置为0则不过期 默认为0
 * maxIdleTime 最大空闲时间 根据LRU算法清理空闲数据 如果设置为0则不检测 默认为0
 * maxSize 组最大长度 根据LRU算法清理溢出数据 如果设置为0则无限长 默认为0
 * local 默认开启本地缓存为1 关闭本地缓存为0
 */
public interface DocmanCacheNames {

    /**
     * 用户可访问项目ID列表
     * 缓存5分钟，最大空闲时间30分钟
     */
    String USER_ACCESSIBLE_PROJECTS = "docman_user_accessible_projects#5m#30m#1000";

    /**
     * 用户项目角色
     * 缓存10分钟，最大空闲时间1小时
     */
    String USER_PROJECT_ROLE = "docman_user_project_role#10m#1h#5000";
}