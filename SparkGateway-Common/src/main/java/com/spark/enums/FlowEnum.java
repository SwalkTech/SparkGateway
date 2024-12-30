package com.spark.enums;



/**
 * @author: spark
 * @date: 2024/12/27 14:24
 * @description: 流控算法
 **/
public enum FlowEnum {

    SLIDING_WINDOW("滑动窗口"),
    TOKEN_BUCKET("令牌桶"),
    LEAKY_BUCKET("漏桶")
    ;

    private final String des;

    FlowEnum(String des) {
        this.des = des;
    }
}
