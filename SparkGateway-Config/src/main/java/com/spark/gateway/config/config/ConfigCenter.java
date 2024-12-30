package com.spark.gateway.config.config;


import com.spark.enums.ConfigCenterEnum;

import static com.spark.constant.ConfigCenterConstant.*;

import com.spark.gateway.config.config.lib.nacos.NacosConfig;
import lombok.Data;
/**
 * 配置中心
 */
@Data
public class ConfigCenter {

    private boolean enabled = CONFIG_CENTER_DEFAULT_ENABLED; // 是否开启配置中心

    private ConfigCenterEnum type = CONFIG_CENTER_DEFAULT_IMPL; // 配置中心实现

    private String address = CONFIG_CENTER_DEFAULT_ADDRESS; // 配置中心地址

    private NacosConfig nacos = new NacosConfig(); // nacos配置

}
