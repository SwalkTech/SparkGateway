package com.spark.gateway.config.config;


import com.spark.enums.RegisterCenterEnum;
import com.spark.gateway.config.config.lib.nacos.NacosConfig;
import lombok.Data;

import static com.spark.constant.RegisterCenterConstant.*;

/**
 * 注册中心
 */
@Data
public class RegisterCenter {

    private RegisterCenterEnum type = REGISTER_CENTER_DEFAULT_IMPL; // 注册中心实现

    private String address = REGISTER_CENTER_DEFAULT_ADDRESS; // 注册中心地址

    private NacosConfig nacos = new NacosConfig(); // 注册中心nacos配置

}
