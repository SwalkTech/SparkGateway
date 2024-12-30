package com.spark.gateway.config.config;

import lombok.Data;

/**
 * netty配置
 */
@Data
public class NettyConfig {

    /**
     * Boss线程组的数量，默认为1
     * Boss线程组主要负责接收客户端的连接请求
     */
    private int eventLoopGroupBossNum = 1;

    /**
     * Worker线程组的数量，默认为可用处理器数量的两倍
     * Worker线程组主要负责处理客户端的读写操作
     */
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 最大内容长度，默认为64MB
     * 用于限制接收的最大请求或响应的大小，防止内存溢出
     */
    private int maxContentLength = 64 * 1024 * 1024; // 64MB

}
