package com.spark.gateway.core.algorithm;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing {

    private final int virtualNodeNum;

    // 哈希环
    private final SortedMap<Integer, String> hashCircle = new TreeMap<>();

    // 构造函数，初始化一致性哈希环
    public ConsistentHashing(List<String> nodes, int virtualNodeNum) {
        this.virtualNodeNum = virtualNodeNum;
        for (String node : nodes) {
            addNode(node);
        }
    }

    /**
     * 在一致性哈希环中添加节点
     * 通过将节点添加到哈希环中，实现负载均衡和高效的数据定位
     *
     * @param node 实际节点的标识符，用于计算哈希值和标识节点
     */
    public void addNode(String node) {
        // 遍历虚拟节点数量，为每个实际节点添加相应的虚拟节点
        for (int i = 0; i < virtualNodeNum; i++) {
            // 生成虚拟节点的标识符，通过连接实际节点标识符和虚拟节点序号
            String virtualNode = node + "&&VN" + i;
            // 将虚拟节点的哈希值和实际节点标识符映射到哈希环上
            hashCircle.put(getHash(virtualNode), node);
        }
    }

    /**
     * 根据键获取对应的节点
     * 该方法主要用于在一致性哈希环中找到对应键的节点
     * 如果哈希环为空，返回null
     * 否则，计算键的哈希值，并在哈希环中找到第一个大于或等于该哈希值的节点
     * 如果没有找到这样的节点，则返回哈希环中第一个节点
     *
     * @param key 键，用于计算哈希值并找到对应的节点
     * @return 对应键的节点，如果找不到则返回null
     */
    public String getNode(String key) {
        // 检查哈希环是否为空，如果为空则返回null
        if (hashCircle.isEmpty()) {
            return null;
        }
        // 计算键的哈希值
        int hash = getHash(key);
        // 在哈希环中找到第一个大于或等于计算出哈希值的位置
        SortedMap<Integer, String> tailMap = hashCircle.tailMap(hash);
        // 如果没有找到这样的位置，说明键的哈希值在哈希环的末尾之后，需要返回哈希环的第一个节点
        // 否则，返回找到的位置对应的节点
        Integer nodeHash = tailMap.isEmpty() ? hashCircle.firstKey() : tailMap.firstKey();
        // 返回找到的节点
        return hashCircle.get(nodeHash);
    }

    /**
     * 计算给定字符串的哈希值
     * 本方法使用了Jenkins哈希函数的变种，来生成字符串的哈希值
     * 这个实现通过异或和移位操作确保了哈希值的均匀分布
     *
     * @param str 要计算哈希值的字符串
     * @return 字符串的哈希值，保证返回的哈希值为非负整数
     */
    private int getHash(String str) {
        // 素数，用于乘法运算，以实现更好的哈希值分布
        final int p = 16777619;
        // 初始化哈希值，使用一个任意的非零值
        int hash = (int) 2166136261L;
        // 遍历字符串中的每个字符
        for (int i = 0; i < str.length(); i++) {
            // 将当前字符的ASCII值与哈希值进行异或，然后乘以素数p，以实现哈希值的更新
            hash = (hash ^ str.charAt(i)) * p;
        }
        // 通过位移和异或操作进一步混淆哈希值，以达到更好的分布效果
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        // 确保哈希值为非负整数
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        // 返回最终计算出的哈希值
        return hash;
    }

}