package com.spark.gateway.config.loader;


import com.spark.gateway.config.config.Config;
import com.spark.gateway.config.util.ConfigUtil;

import static com.spark.constant.ConfigConstant.CONFIG_PATH;
import static com.spark.constant.ConfigConstant.CONFIG_PREFIX;

/**
 * 配置加载
 */
public class ConfigLoader {

    /**
     * 从 YAML 文件中加载配置信息
     * 该方法使用 ConfigUtil 工具类来加载指定路径的 YAML 文件，并将其解析为 Config 类的对象
     *
     * @param args 命令行参数，本方法中未使用，但可能在将来用于指定配置文件路径或其他用途
     * @return Config对象，包含从 YAML 文件中加载的配置信息
     */
    public static Config load(String[] args) {
        // 使用 ConfigUtil 工具类从 YAML 文件中加载配置信息
        // CONFIG_PATH 为配置文件的路径，Config.class 指定要解析的目标类类型，CONFIG_PREFIX 可能用于指定配置项的前缀
        return ConfigUtil.loadConfigFromYaml(CONFIG_PATH, Config.class, CONFIG_PREFIX);
    }

}
