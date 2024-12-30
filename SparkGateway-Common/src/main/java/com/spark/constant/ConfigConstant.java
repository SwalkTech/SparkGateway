package com.spark.constant;

/**
 * 接口 ConfigConstant 定义了系统配置相关的常量
 */
public interface ConfigConstant {

    // 配置文件名称
    String CONFIG_PATH = "gateway.yaml";

    // 配置前缀
    String CONFIG_PREFIX = "spark.gateway";

    // 服务默认名字
    String DEFAULT_NAME = "spark-gateway";

    // 默认端口
    int DEFAULT_PORT = 10086;

    // 默认环境
    String DEFAULT_ENV = "dev";

}
