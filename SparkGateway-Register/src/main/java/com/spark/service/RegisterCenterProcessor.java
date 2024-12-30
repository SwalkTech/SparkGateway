package com.spark.service;

import com.spark.gateway.config.config.Config;


/**
 * @author: spark
 * @date: 2024/12/30 08:51
 * @description: 注册中心接口
 **/
public interface RegisterCenterProcessor {

    /**
     * 注册中心初始化
     */
    void init(Config config);

    /**
     * 订阅注册中心实例变化
     */
    void subscribeServiceChange(RegisterCenterListener listener);

}
