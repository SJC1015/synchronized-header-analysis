package com.sjc.models;

import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : MarkWordFactory
 * @description : 创建对应锁类的工厂
 */
public class MarkWordFactory {

    /**
     * 创建对应锁类的方法
     */
    public static MarkWord getMarkword(Object o){
        VirtualMachine vm = VM.current();
        final long markWord = vm.getLong(o, 0);
        //锁标志位 二进制 01 代表 偏向锁或者无锁态 00 代表轻量级锁 11 代表 重量级锁
        long lockFlag =  markWord & 3;
        //偏向标志位 二进制 1 代表 偏向锁 0 无锁态
        long biasedFlag =  markWord & 4;
        if (lockFlag == 1){
            if (biasedFlag == 0){
                return new NoLockMarkWord(markWord);
            }else{
                return new BiasedLockMarkWord(markWord);
            }
        }else if (lockFlag == 0){
            return new LightWeightLockMarkWord(markWord);
        }else if (lockFlag == 2){
            return new HeavyWeightLockMarkWord(markWord);
        }
        throw new RuntimeException("CMS 过程中");
    }
}
