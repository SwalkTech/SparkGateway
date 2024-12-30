package com.spark.gateway.config.manager;


import com.spark.gateway.config.pojo.RouteDefinition;

public interface RouteListener {

    void changeOnRoute(RouteDefinition routeDefinition);

}
