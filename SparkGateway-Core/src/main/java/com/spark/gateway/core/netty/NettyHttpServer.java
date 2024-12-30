package com.spark.gateway.core.netty;


import com.spark.gateway.config.config.Config;
import com.spark.gateway.core.config.LifeCycle;
import com.spark.gateway.core.netty.handler.NettyHttpServerHandler;
import com.spark.gateway.core.netty.processor.NettyProcessor;
import com.spark.util.SystemUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty的Server端实现
 */
@Slf4j
public class NettyHttpServer implements LifeCycle {

    private final Config config;
    private final NettyProcessor nettyProcessor;
    private final AtomicBoolean start = new AtomicBoolean(false);
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;

    public NettyHttpServer(Config config, NettyProcessor nettyProcessor) {
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }

    /**
     * 初始化Netty服务器配置
     * <p>
     * 本方法根据系统环境选择合适的传输层实现（Epoll或NIO），并配置Netty服务器的事件循环组
     * 事件循环组用于处理I/O操作和异步任务，是Netty高性能的关键组成部分
     */
    private void init() {
        // 创建ServerBootstrap实例，用于配置和启动Netty服务器
        this.serverBootstrap = new ServerBootstrap();

        // 判断是否使用Epoll，这取决于系统是否支持Epoll（通常用于Linux环境）
        if (SystemUtil.useEpoll()) {
            // 如果使用Epoll，创建EpollEventLoopGroup实例，用于处理Boss线程组
            // Boss线程组负责接收客户端连接请求
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getNetty().getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("epoll-netty-boss-nio"));

            // 如果使用Epoll，创建EpollEventLoopGroup实例，用于处理Worker线程组
            // Worker线程组负责处理具体的I/O操作和业务逻辑
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getNetty().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("epoll-netty-worker-nio"));
        } else {
            // 如果不使用Epoll（例如在非Linux环境下），创建NioEventLoopGroup实例，用于处理Boss线程组
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getNetty().getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("default-netty-boss-nio"));

            // 如果不使用Epoll，创建NioEventLoopGroup实例，用于处理Worker线程组
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getNetty().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("default-netty-worker-nio"));
        }
    }

    // 检测是否使用Epoll优化性能
    // 启动Netty服务器
    @SneakyThrows(InterruptedException.class)
    @Override
    public void start() {
        // 使用CAS确保服务器启动状态的线程安全，防止重复启动
        if (!start.compareAndSet(false, true)) {
            return;
        }
        // 配置服务器参数，如端口、TCP参数等
        serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(SystemUtil.useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)            // TCP连接的最大队列长度
                .option(ChannelOption.SO_REUSEADDR, true)          // 允许端口重用
                .option(ChannelOption.SO_KEEPALIVE, true)          // 保持连接检测
                .childOption(ChannelOption.TCP_NODELAY, true)      // 禁用Nagle算法，适用于小数据即时传输
                .childOption(ChannelOption.SO_SNDBUF, 65535)       // 设置发送缓冲区大小
                .childOption(ChannelOption.SO_RCVBUF, 65535)       // 设置接收缓冲区大小
                .localAddress(new InetSocketAddress(config.getPort())) // 绑定监听端口
                .childHandler(new ChannelInitializer<>() {   // 定义处理新连接的管道初始化逻辑
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        // 初始化新连接的ChannelPipeline，添加处理HTTP请求的必要处理器
                        ch.pipeline().addLast(
                                new HttpServerCodec(), // 处理HTTP请求的编解码器
                                new HttpObjectAggregator(config.getNetty().getMaxContentLength()), // 聚合HTTP请求
                                new HttpServerExpectContinueHandler(), // 处理HTTP 100 Continue请求
                                new NettyHttpServerHandler(nettyProcessor) // 自定义的处理器
                        );
                    }
                });
        // 绑定端口并同步等待绑定完成
        serverBootstrap.bind().sync();
        // 设置资源泄漏检测级别为ADVANCED，以便在开发环境中更容易发现资源泄漏问题
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        // 日志记录服务器启动信息
        log.info("gateway startup on port {}", this.config.getPort());
    }

    @Override
    public void shutdown() {
        if (!start.get()) {
            return;
        }
        if (eventLoopGroupBoss != null) {
            eventLoopGroupBoss.shutdownGracefully(); // 优雅关闭boss线程组
        }

        if (eventLoopGroupWorker != null) {
            eventLoopGroupWorker.shutdownGracefully(); // 优雅关闭worker线程组
        }
    }

    @Override
    public boolean isStarted() {
        return start.get();
    }

}
