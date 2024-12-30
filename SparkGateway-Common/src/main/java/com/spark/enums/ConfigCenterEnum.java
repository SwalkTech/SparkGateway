package com.spark.enums;


import lombok.Getter;


/**
 * @author: spark
 * @date: 2024/12/27 13:55
 * @description: 配置中心枚举
 **/
@Getter
public enum ConfigCenterEnum {

    NACOS("nacos"),
    ZOOKEEPER("zookeeper");

    private final String des;

    ConfigCenterEnum(String des) {
        this.des = des;
    }

}
