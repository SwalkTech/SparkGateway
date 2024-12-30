package com.spark.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author: spark
 * @date: 2024/12/30 08:41
 * @description: 网络工具类，用于获取本地IP地址。
 **/
public class NetUtil {

    /**
     * 匹配给定的IP地址与前缀数组，返回匹配的索引。
     *
     * @param ip     IP地址字符串
     * @param prefix 前缀数组
     * @return 匹配的索引，如果没有匹配则返回-1
     */
    private static int matchedIndex(String ip, String[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            String p = prefix[i];
            if ("*".equals(p)) { // 如果前缀是"*"，则匹配非私有IP
                if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.")) {
                    continue;
                }
                return i;
            } else {
                if (ip.startsWith(p)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 根据指定的优先级获取本地IP地址。
     *
     * @param ipPreference IP地址优先级字符串，格式如 "*>10>172>192>127"
     * @return 本地IP地址，如果获取失败则返回 "127.0.0.1"
     */
    public static String getLocalIp(String ipPreference) {
        if (ipPreference == null) {
            ipPreference = "*>10>172>192>127";
        }
        String[] prefix = ipPreference.split("[> ]+");
        try {
            Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String matchedIp = null;
            int matchedIdx = -1;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // 跳过回环网卡和虚拟网卡
                if (ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> en = ni.getInetAddresses();
                // 跳过回环地址、非站点本地地址和通配符地址
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    if (addr.isLoopbackAddress() || !addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    Matcher matcher = pattern.matcher(ip);
                    if (matcher.matches()) {
                        int idx = matchedIndex(ip, prefix);
                        if (idx == -1) {
                            continue;
                        }
                        if (matchedIdx == -1) {
                            matchedIdx = idx;
                            matchedIp = ip;
                        } else {
                            if (matchedIdx > idx) {
                                matchedIdx = idx;
                                matchedIp = ip;
                            }
                        }
                    }
                }
            }
            if (matchedIp != null) {
                return matchedIp;
            }
            return "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * 获取本地IP地址，默认优先级为 "*>10>172>192>127"。
     *
     * @return 本地IP地址，如果获取失败则返回 "127.0.0.1"
     */
    public static String getLocalIp() {
        return getLocalIp("*>10>172>192>127");
    }

}
