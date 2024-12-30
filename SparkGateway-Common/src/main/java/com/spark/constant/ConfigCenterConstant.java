package com.spark.constant;


import com.spark.enums.ConfigCenterEnum;

/**
 * 配置中心常量接口
 * 该接口定义了配置中心的相关常量，包括是否开启配置中心、配置中心的默认实现以及默认地址
 */
public interface ConfigCenterConstant {

    // 是否开启配置中心，为了方便起项目，默认关闭
    boolean CONFIG_CENTER_DEFAULT_ENABLED = false;

    // 配置中心默认实现
    ConfigCenterEnum CONFIG_CENTER_DEFAULT_IMPL = ConfigCenterEnum.NACOS;

    // 配置中心默认地址
    String CONFIG_CENTER_DEFAULT_ADDRESS = "127.0.0.1:8848";

}

