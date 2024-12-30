package com.spark.gateway.core.helper;


import cn.hutool.json.JSONUtil;
import com.spark.enums.ResponseCode;
import com.spark.gateway.core.response.GatewayResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Response;

import java.util.Objects;


/**
 * Netty服务端、网关、Http客户端之间的响应转换
 */
@Slf4j
public class ResponseHelper {

    /**
     * 构建Http响应
     * 根据网关响应对象中的内容或下游服务的响应来构建一个FullHttpResponse对象
     *
     * @param gatewayResponse 网关响应对象，包含从下游服务接收到的响应或网关处理的结果
     * @return 返回一个构建好的FullHttpResponse对象
     */
    public static FullHttpResponse buildHttpResponse(GatewayResponse gatewayResponse) {
        // 初始化响应内容，根据gatewayResponse中不同的数据来源进行处理
        ByteBuf content;
        if (Objects.nonNull(gatewayResponse.getResponse())) {
            // 如果下游服务的响应结果存在，则使用该结果作为响应内容
            content = Unpooled.wrappedBuffer(gatewayResponse.getResponse().getResponseBodyAsByteBuffer()); // 下游服务的http响应结果
        } else if (gatewayResponse.getContent() != null) {
            // 如果网关响应对象中包含内容，则使用该内容作为响应内容
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes());
        } else {
            // 如果没有内容，则使用空字符串作为响应内容
            content = Unpooled.wrappedBuffer("".getBytes());
        }

        // 初始化FullHttpResponse对象，根据是否有下游服务的响应来区分处理
        DefaultFullHttpResponse httpResponse;
        // 下游响应不为空，直接拿下游响应构造
        if (Objects.nonNull(gatewayResponse.getResponse())) {
            // 使用下游服务的响应信息来构建FullHttpResponse对象
            httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(gatewayResponse.getResponse().getStatusCode()), content);
            // 添加下游服务响应中的头部信息
            httpResponse.headers().add(gatewayResponse.getResponse().getHeaders()); //
        } else {
            // 使用网关响应对象中的信息来构建FullHttpResponse对象
            httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    gatewayResponse.getHttpResponseStatus(), content);
            // 添加网关响应对象中的头部信息
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            // 设置响应的内容长度
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        }

        // 返回构建好的FullHttpResponse对象
        return httpResponse;
    }

    public static FullHttpResponse buildHttpResponse(ResponseCode responseCode) {
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                responseCode.getStatus(),
                Unpooled.wrappedBuffer(responseCode.getMessage().getBytes()));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        return httpResponse;
    }


    public static GatewayResponse buildGatewayResponse(Response response) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setResponseHeaders(response.getHeaders());
        gatewayResponse.setHttpResponseStatus(HttpResponseStatus.valueOf(response.getStatusCode()));
        gatewayResponse.setContent(response.getResponseBody());
        gatewayResponse.setResponse(response);

        return gatewayResponse;
    }

    public static GatewayResponse buildGatewayResponse(ResponseCode code) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.addHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        gatewayResponse.setHttpResponseStatus(code.getStatus());
        gatewayResponse.setContent(JSONUtil.toJsonStr(code.getMessage()));

        return gatewayResponse;
    }

    public static GatewayResponse buildGatewayResponse(Object data) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.addHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        gatewayResponse.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        gatewayResponse.setContent(JSONUtil.toJsonStr(data));

        return gatewayResponse;
    }
}
