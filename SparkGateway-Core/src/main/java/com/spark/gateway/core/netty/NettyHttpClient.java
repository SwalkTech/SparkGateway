package com.spark.gateway.core.netty;

import com.spark.gateway.config.config.Config;
import com.spark.gateway.config.config.HttpClientConfig;
import com.spark.gateway.core.config.LifeCycle;
import com.spark.gateway.core.http.HttpClient;
import com.spark.util.SystemUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NettyHttpClient类实现了LifeCycle接口，用于管理HTTP客户端的生命周期
 * 它基于Netty和AsyncHttpClient库，用于处理HTTP请求
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {

    // 系统配置对象，用于获取HTTP客户端配置
    private final Config config;

    // 事件循环组，用于处理I/O操作
    private final EventLoopGroup eventLoopGroupWorker;

    // 原子布尔，用于表示客户端是否已启动
    private final AtomicBoolean start = new AtomicBoolean(false);

    // 异步HTTP客户端对象
    private AsyncHttpClient asyncHttpClient;

    /**
     * 构造函数，初始化NettyHttpClient
     * 根据系统是否支持Epoll来选择合适的EventLoopGroup实现
     *
     * @param config 系统配置对象，用于获取HTTP客户端配置
     */
    public NettyHttpClient(Config config) {
        this.config = config;
        if (SystemUtil.useEpoll()) {
            // 如果系统支持Epoll，则使用EpollEventLoopGroup
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getHttpClient().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("epoll-http-client-worker-nio"));
        } else {
            // 否则使用NioEventLoopGroup
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getHttpClient().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("default-http-client-worker-nio"));
        }
    }

    /**
     * 启动HTTP客户端
     * 根据配置创建并初始化AsyncHttpClient对象
     */
    @Override
    public void start() {
        // 使用CAS确保客户端只被启动一次
        if (!start.compareAndSet(false, true)) {
            return;
        }
        HttpClientConfig httpClientConfig = config.getHttpClient();
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker) // 使用传入的Netty事件循环组
                .setConnectTimeout(httpClientConfig.getHttpConnectTimeout()) // 连接超时设置
                .setRequestTimeout(httpClientConfig.getHttpRequestTimeout()) // 请求超时设置
                .setMaxRedirects(httpClientConfig.getHttpMaxRedirects()) // 最大重定向次数
                .setAllocator(PooledByteBufAllocator.DEFAULT) // 使用池化的ByteBuf分配器以提升性能
                .setCompressionEnforced(true) // 强制压缩
                .setMaxConnections(httpClientConfig.getHttpMaxConnections()) // 最大连接数
                .setMaxConnectionsPerHost(httpClientConfig.getHttpConnectionsPerHost()) // 每个主机的最大连接数
                .setPooledConnectionIdleTimeout(httpClientConfig.getHttpPooledConnectionIdleTimeout()); // 连接池中空闲连接的超时时间
        // 根据配置创建异步HTTP客户端
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
        HttpClient.getInstance().initialized(asyncHttpClient);
    }

    /**
     * 关闭HTTP客户端
     * 释放资源并确保客户端停止运行
     */
    @Override
    public void shutdown() {
        // 确保客户端已被启动
        if (!start.get()) {
            return;
        }
        if (asyncHttpClient != null) {
            try {
                // 关闭异步HTTP客户端
                this.asyncHttpClient.close();
            } catch (IOException e) {
                // 日志记录关闭过程中出现的错误
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }

    /**
     * 检查HTTP客户端是否已启动
     *
     * @return 如果客户端已启动，则返回true；否则返回false
     */
    @Override
    public boolean isStarted() {
        return start.get();
    }

}
