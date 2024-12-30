package com.spark.gateway.core.helper;

import com.alibaba.nacos.common.utils.StringUtils;
import com.spark.gateway.config.pojo.ServiceDefinition;
import com.spark.gateway.core.request.GatewayRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.asynchttpclient.Request;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.spark.constant.HttpConstant.HTTP_FORWARD_SEPARATOR;


/**
 * Netty服务端、网关、Http客户端之间的请求转换
 */
public class RequestHelper {

    /**
     * 构建网关请求对象
     * <p>
     * 该方法根据传入的服务定义和完整的HTTP请求，以及通道处理上下文，来构造一个GatewayRequest对象
     * 它主要负责解析HTTP请求中的关键信息，如主机名、请求方法、URI、客户端IP地址、内容类型和字符集，
     * 并将这些信息与服务定义一起封装到GatewayRequest对象中，以便后续处理
     *
     * @param serviceDefinition 服务定义，描述了服务的相关信息
     * @param fullHttpRequest   完整的HTTP请求对象，包含了请求的所有信息
     * @param ctx               通道处理上下文，提供了与客户端通信的通道、地址信息等
     * @return 返回构建好的GatewayRequest对象，用于后续的请求处理
     */
    public static GatewayRequest buildGatewayRequest(ServiceDefinition serviceDefinition, FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
        // 获取服务端的HTTP请求头
        HttpHeaders headers = fullHttpRequest.headers();
        // 提取HTTP请求头中的host信息
        String host = headers.get(HttpHeaderNames.HOST);
        // 获取HTTP请求的方法类型
        HttpMethod method = fullHttpRequest.method();
        // 获取HTTP请求的URI
        String uri = fullHttpRequest.uri();
        // 获取客户端的IP地址
        String clientIp = getClientIp(ctx, fullHttpRequest);
        // 获取请求的MIME类型，如果不可用则为null
        String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null :
                HttpUtil.getMimeType(fullHttpRequest).toString();
        // 获取请求的字符集，如果不可用则默认为UTF-8
        Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);

        // 使用解析出的信息构建并返回GatewayRequest对象
        return new GatewayRequest(serviceDefinition, charset, clientIp, host, uri, method,
                contentType, headers, fullHttpRequest);
    }

    public static Request buildHttpClientRequest(GatewayRequest gatewayRequest) {
        return gatewayRequest.build();
    }

    /**
     * 获取客户端IP地址
     * 该方法首先尝试从HTTP请求头中获取客户端IP地址，如果获取失败，则从ChannelHandlerContext中获取
     *
     * @param ctx     ChannelHandlerContext对象，用于获取远程地址信息
     * @param request FullHttpRequest对象，包含HTTP请求的所有信息
     * @return 返回客户端的IP地址字符串
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 尝试从请求头中获取X-Forwarded-For的值，这通常包含客户端IP地址
        String xForwardedValue = request.headers().get(HTTP_FORWARD_SEPARATOR);

        // 初始化客户端IP地址为null
        String clientIp = null;

        // 如果X-Forwarded-For的值不为空
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            // 分割X-Forwarded-For的值，获取所有IP地址
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            // 如果IP地址列表不为空且第一个IP地址不为空，则将其作为客户端IP地址
            if (values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }

        // 如果未能从请求头中获取客户端IP地址
        if (clientIp == null) {
            // 从ChannelHandlerContext中获取远程地址信息
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            // 从远程地址信息中提取IP地址并将其作为客户端IP地址
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }

        // 返回客户端IP地址
        return clientIp;
    }

}
