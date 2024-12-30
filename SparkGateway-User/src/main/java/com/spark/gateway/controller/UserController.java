package com.spark.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author: spark
 * @date: 2024/12/30 11:37
 * @description: 测试
 **/
@RestController(value = "user")
@RequestMapping("/test")
public class UserController {
    @GetMapping("/api/user/ping1")
    public String ping1() {
        return "this is user ping1";
    }

    @GetMapping("/api/user/ping2")
    public String ping2() {
        return "this is user ping2";
    }

}
