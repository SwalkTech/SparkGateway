package com.spark.gateway;

import com.spark.gateway.bootstrap.Bootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author: spark
 * @date: 2024/12/30 11:28
 * @description:
 **/
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        Bootstrap.run(args);
    }

}
