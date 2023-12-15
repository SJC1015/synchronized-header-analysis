package com.sjc.models;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : HeavyWeightLockMarkWord
 * @description : 重量级锁的 Mark Word
 */
public class HeavyWeightLockMarkWord implements MarkWord{
    /**
     * 锁标志 默认是 2 因为生成该对象的条件之一就是这个
     */
    private static final long LOCK_FLAG = 2;

    /**
     * 全长 64 位的 mark word
     */
    private final long markWord;

    /**
     * 指向重量级锁的指针
     */
    private final long heavyWeightLockPointer;

    public HeavyWeightLockMarkWord(long markWord) {
        this.markWord = markWord;
        this.heavyWeightLockPointer = markWord >> 2;
    }

    @Override
    public long getMarkWord() {
        return markWord;
    }

    @Override
    public String getMarkWordStr() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("当前状态为 重量级锁\n");
        MarkWord.getLongToString("mark word 值\t", 64, markWord, stringBuilder);
        MarkWord.getLongToString("指向重量级锁的指针\t", 62, heavyWeightLockPointer, stringBuilder);
        MarkWord.getLongToString("锁标志位\t\t", 2, LOCK_FLAG, stringBuilder);
        return stringBuilder.toString();
    }

    @Override
    public String getLockStatus() {
        return "重量级锁";
    }

    public long getHeavyWeightLockPointer() {
        return heavyWeightLockPointer;
    }
}
