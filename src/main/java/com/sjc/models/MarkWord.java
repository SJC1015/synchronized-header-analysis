package com.sjc.models;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @interfaceName : MarkWord
 * @description : mark word 的接口
 */
public interface MarkWord {


    /**
     * 获取 mark word的原始数据
     */
    long getMarkWord();

    /**
     * 获取 mark word的结构化后的数据
     */
    String getMarkWordStr();

    /**
     * 输出当前锁状态
     */
    String getLockStatus();

    static void getLongToString(String columnTitle, int length, long number, StringBuilder stringBuilder){
        stringBuilder.append(columnTitle)
                .append("\t长度: ").append(length)
                .append("\t 十进制: ").append(number)
                .append("\t 十六进制: ").append(Long.toHexString(number))
                .append("\t二进值: ").append(Long.toBinaryString(number))
                .append("\n");
    }
}
