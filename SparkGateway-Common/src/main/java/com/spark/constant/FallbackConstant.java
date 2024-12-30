package com.spark.constant;

/**
 * 定义了fallback（回退）机制中的常量
 * 用于在整个系统中统一处理回退逻辑的命名
 */
public interface FallbackConstant {

    /**
     * 默认的回退处理器名称
     * 用于在没有特别指定回退处理器时使用
     */
    String DEFAULT_FALLBACK_HANDLER_NAME = "default_fallback_handler";

}
