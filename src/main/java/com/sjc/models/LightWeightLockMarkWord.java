package com.sjc.models;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : LightWeightLockMarkWord
 * @description : 轻量级锁的 mark word
 */
public class LightWeightLockMarkWord implements MarkWord{

    /**
     * 全长 64 位的 mark word
     */
    private final long markWord;

    private static final long LOCK_FLAG = 0;

    private final long stackLockRecordPointer;


    public LightWeightLockMarkWord(long markWord) {
        this.markWord = markWord;
        this.stackLockRecordPointer = markWord >> 2;
    }

    @Override
    public long getMarkWord() {
        return markWord;
    }

    @Override
    public String getMarkWordStr() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("当前状态为 轻量级锁\n");
        MarkWord.getLongToString("mark word 值\t\t\t\t\t", 64, markWord, stringBuilder);
        MarkWord.getLongToString("指向线程栈中Lock Record 锁记录的指针\t", 62, stackLockRecordPointer, stringBuilder);
        MarkWord.getLongToString("锁标志位\t\t\t\t\t\t\t", 2, LOCK_FLAG, stringBuilder);
        return stringBuilder.toString();
    }

    @Override
    public String getLockStatus() {
        return "轻量级锁";
    }

    public LightWeightLockMarkWord(long markWord, long stackLockRecordPointer) {
        this.markWord = markWord;
        this.stackLockRecordPointer = stackLockRecordPointer;
    }
}
