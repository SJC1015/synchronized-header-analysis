package com.sjc.models;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : BiasedLockMarkWord
 * @description : 偏向锁的 Mark Word
 */
public class BiasedLockMarkWord implements MarkWord{

    /**
     * 偏向锁标志 默认是 1 因为生成该对象的条件之一就是这个
     */
    private static final long BIASED_LOCK_FLAG = 1;

    /**
     * 锁标志 默认是 1 因为生成该对象的条件之一就是这个
     */
    private static final long LOCK_FLAG = 1;

    /**
     * 全长 64 位的 mark word
     */
    private final long markWord;

    /**
     * 偏向线程ID
     */
    private final long biasedThreadId;

    /**
     * epoch偏向时间戳
     */
    private final long epochBiasedTimestamp;

    /**
     * 第一个未使用位
     */
    private static final long FIRST_UNUSED = 0;

    /**
     * 分代年龄
     */
    private final long gcAge;

    /**
     * 构造方法
     */
    public BiasedLockMarkWord(long markWord) {
        this.markWord = markWord;
        //最前面就是 偏向线程ID 长度 54 位 直接向右移 10 位
        this.biasedThreadId = markWord >>> 10;
        //第二段 是 epoch偏向时间戳（没太搞明白这个是什么意思）向左移 54 位将 偏向线程ID的数据通过右移清空 其余移位操作同理不做过多赘述
        this.epochBiasedTimestamp = markWord << 54 >> 62;
        this.gcAge = markWord << 57 >> 60;
    }

    @Override
    public long getMarkWord() {
        return markWord;
    }

    @Override
    public String getMarkWordStr() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("当前状态为 偏向锁\n");
        MarkWord.getLongToString("mark word 值", 64, markWord, stringBuilder);
        MarkWord.getLongToString("偏向线程ID\t", 54, biasedThreadId, stringBuilder);
        MarkWord.getLongToString("epoch偏向时间戳", 2, epochBiasedTimestamp, stringBuilder);
        MarkWord.getLongToString("第一个未使用位\t", 1, FIRST_UNUSED, stringBuilder);
        MarkWord.getLongToString("分代年龄\t\t", 4, gcAge, stringBuilder);
        MarkWord.getLongToString("偏向锁位\t\t", 1, BIASED_LOCK_FLAG, stringBuilder);
        MarkWord.getLongToString("锁标志位\t\t", 2, LOCK_FLAG, stringBuilder);
        return stringBuilder.toString();
    }

    @Override
    public String getLockStatus() {
        return "偏向锁";
    }

    public long getBiasedThreadId() {
        return biasedThreadId;
    }

    public long getEpochBiasedTimestamp() {
        return epochBiasedTimestamp;
    }

    public long getGcAge() {
        return gcAge;
    }
}
