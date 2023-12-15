package com.sjc.models;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : NoLockMarkWord
 * @description : 没有锁的 mark word
 */
public class NoLockMarkWord implements MarkWord{
    /**
     * 偏向锁标志 默认是 0 因为生成该对象的条件之一就是这个
     */
    private static final long BIASED_LOCK_FLAG = 0;

    /**
     * 锁标志 默认是 1 因为生成该对象的条件之一就是这个
     */
    private static final long LOCK_FLAG = 1;

    /**
     * 第一段未用数据
     */
    private static final long FIRST_UNUSED = 0;

    /**
     * 第二段未用数据
     */
    private static final long SECOND_UNUSED = 0;

    /**
     * 全长 64 位的 mark word
     */
    private final long markWord;

    /**
     * 对象的hashcode
     */
    private final long lockItemHashCode;

    /**
     * 分代年龄
     */
    private final long gcAge;

    public NoLockMarkWord(long markWord) {
        this.markWord = markWord;
        lockItemHashCode = markWord << 25 >>> 33;
        gcAge = markWord << 57 >> 60;
    }

    @Override
    public long getMarkWord() {
        return markWord;
    }

    @Override
    public String getMarkWordStr() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("当前状态为 无锁\n");
        MarkWord.getLongToString("mark word 值", 64, markWord, stringBuilder);
        MarkWord.getLongToString("第一个未使用位\t", 25, FIRST_UNUSED, stringBuilder);
        MarkWord.getLongToString("对象的hashcode", 31, lockItemHashCode, stringBuilder);
        MarkWord.getLongToString("第二个未使用位\t", 1, SECOND_UNUSED, stringBuilder);
        MarkWord.getLongToString("分代年龄\t\t", 4, gcAge, stringBuilder);
        MarkWord.getLongToString("偏向锁位\t\t", 1, BIASED_LOCK_FLAG, stringBuilder);
        MarkWord.getLongToString("锁标志位\t\t", 2, LOCK_FLAG, stringBuilder);
        return stringBuilder.toString();
    }

    @Override
    public String getLockStatus() {
        return "无锁";
    }

    public long getFirstUnused() {
        return FIRST_UNUSED;
    }

    public long getLockItemHashCode() {
        return lockItemHashCode;
    }

    public long getSecondUnused() {
        return SECOND_UNUSED;
    }

    public long getGcAge() {
        return gcAge;
    }
}
