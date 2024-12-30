package com.spark.constant;

public interface SparkConstant {

    double MAX_GRAY_THRESHOLD = 0.95D; // 服务灰度的最大比例

    String THRESHOLD_GRAY_STRATEGY = "threshold_gray_strategy"; // 根据流量决定是否灰度的策略名

    String CLIENT_IP_GRAY_STRATEGY = "client_ip_gray_strategy"; // 根据用户ip决定是否灰度的策略名

}
