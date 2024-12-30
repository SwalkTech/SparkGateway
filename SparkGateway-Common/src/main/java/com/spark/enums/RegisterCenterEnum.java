package com.spark.enums;

import lombok.Getter;

/**
 * @author: spark
 * @date: 2024/12/27 14:23
 * @description: 注册中心枚举
 **/
@Getter
public enum RegisterCenterEnum {

    NACOS("nacos"),
    ZOOKEEPER("zookeeper");

    private final String des;

    RegisterCenterEnum(String des) {
        this.des = des;
    }

}
