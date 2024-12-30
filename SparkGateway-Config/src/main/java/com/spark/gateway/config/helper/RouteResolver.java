package com.spark.gateway.config.helper;


import com.spark.enums.ResponseCode;
import com.spark.exception.NotFoundException;
import com.spark.gateway.config.manager.DynamicConfigManager;
import com.spark.gateway.config.pojo.RouteDefinition;

import java.util.*;
import java.util.regex.Pattern;

public class RouteResolver {

    private static final DynamicConfigManager manager = DynamicConfigManager.getInstance();
    /**
     * 根据uri解析出对应的路由
     * 该方法通过匹配uri与预定义的路由模式，找出所有匹配的路由定义，并按照一定规则排序，返回最优的路由选择
     *
     * @param uri 请求的uri，用于匹配路由
     * @return 返回匹配到的最优路由定义
     * @throws NotFoundException 当没有找到匹配的路由时抛出此异常
     */
    public static RouteDefinition matchingRouteByUri(String uri) {
        // 获取所有URI映射的路由定义
        Set<Map.Entry<String, RouteDefinition>> allUriEntry = manager.getAllUriEntry();

        // 存储所有匹配的路由定义
        List<RouteDefinition> matchedRoute = new ArrayList<>();

        // 遍历所有URI映射的路由定义，寻找匹配的路由
        for (Map.Entry<String, RouteDefinition> entry: allUriEntry) {
            // 将映射中的**替换为正则表达式.*，以支持模式匹配
            String regex = entry.getKey().replace("**", ".*");
            // 如果当前路由模式与请求URI匹配，则添加到匹配的路由列表中
            if (Pattern.matches(regex, uri)) {
                matchedRoute.add(entry.getValue());
            }
        }

        // 如果没有匹配的路由，抛出NotFoundException异常
        if (matchedRoute.isEmpty()) {
            throw new NotFoundException(ResponseCode.PATH_NO_MATCHED);
        }

        // 对匹配的路由按顺序排序，确保按照定义的顺序进行处理
        matchedRoute.sort(Comparator.comparingInt(RouteDefinition::getOrder));

        // 返回匹配到的最优路由定义，如果有多个匹配，优先选择顺序最小且URI长度最大的路由
        return matchedRoute.stream()
                .min(Comparator.comparingInt(RouteDefinition::getOrder)
                        .thenComparing(route -> route.getUri().length(), Comparator.reverseOrder()))
                .orElseThrow();
    }

}
