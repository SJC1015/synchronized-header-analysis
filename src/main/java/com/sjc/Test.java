package com.sjc;

import com.sjc.models.MarkWordFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * @author : shenjc
 * @date : 2023年12月13日
 * @classname : Test1
 * @description : 主要测试类
 */
public class Test {

    public static void main(String[] args) {
        RuntimeMXBean mxb = ManagementFactory.getRuntimeMXBean();
        System.out.println(mxb.getVmName());
        System.out.println(mxb.getVmVersion());
        test3();
    }

    /**
     * 测试一个对象由主线程创建使用和不使用 hash code 的情况下的变化方式
     *
     * 执行后发现了很多问题 这些问题的根本是在认同那张 64 位 HotSpot 虚拟机对象头 Mark word 对应表的图的情况下
     * 1、o2在线程中的同步代码块中的锁状态 是偏向锁 但是他的偏向线程 ID 我找不到 可能是线程的某个特殊值吧 这个问题还好
     * 2、为什么对象在调用了 hashcode 方法后锁状态直接就是无锁而不调用反而是偏向锁？
     * 2-1、 而且为什么无锁的 o1 到了线程的同步代码块中时会变成轻量级锁？
     * 2-2、 反而 o2 原来就是偏向锁结果还是偏向锁而且原来空的 偏向线程 ID 有了值（虽然我找不到这个值在线程对象中的位置) 但是 o2 除了一开始的偏向锁状态后面的各个状态都是我可以理解的
     * 3、同时使用了 debug 来测试发现结果差很多而且和断点无关如 2 个对象到了线程中的同步代码块里都是轻量级锁？
     */
    private static void test_1(){
        Object o1 = new Object();
        Object o2 = new Object();
        System.out.println("o1的hashcode => " + o1.hashCode());
        System.out.println("o1初始的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
        System.out.println("o2初始的锁状态 => " + MarkWordFactory.getMarkword(o2).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(o2).getMarkWordStr());
        Thread thread = new Thread(() -> {
            funcCommonOutput(o1, "thread", "o1");
            funcCommonOutput(o2, "thread", "o2");
        });
        thread.start();
    }

    /**
     * 这个是对上面的问题进行一些想法的测试
     * 测试结果是 还是除了一开始的偏向锁状态 其他状态 都在预料中
     * 但是有了一个惊人的发现那就是被主线程使用 synchronized 获取过一次锁之后再由一个线程获取锁变成了轻量级这里是在预料中 但是出来之后和最后的状态都是无锁
     * 根据上面的现象我给出了上面 2 和 2-1 的答案的可能性那就是 hashcode 代码可能开了一个线程并锁定了自己所以在使用
     * 然后就去找 HotSpot hashcode 相关的文章 找到了这个 <a href="https://blog.csdn.net/nibonnn/article/details/124026493">...</a>
     * 问题得到了解决： 原因是原生的 hashcode 方法是通过随机数计算所以只会计算一次并存在 对象的 mark word中
     * 以下是我的理解 由于 hashcode 的重要性所以必须有个地方存放 hashcode
     * 无锁态下 hashcode 存在 mark word 里
     * 偏向锁态下 hashcode 不可能存放在 偏向线程 id 里不说多个锁的情况下但是线程会死亡这一点就不可取
     * 轻量级锁 hashcode 存放在调用栈中锁记录的指针指向的对象的信息中出同步代码块后变为无锁态并将 hashcode 数据放回 mark word 中
     * 重量级锁 和轻量级锁类似放在重量级锁的对象信息中
     * 并且对象最开始的状态也并不是无锁态而是匿名偏向不过我觉得这个和文章最开始的 自旋锁部分是轻量级还是重量级锁一样也是个定义问题毕竟这个匿名偏向我的理解是一个特殊的无锁态因为变成真正的无锁态之后就无法到达偏向锁态了
     */
    private static void test_1_1(){
        Object o1 = new Object();
        funcCommonOutput(o1, "主线程", "o1");
        Thread thread = new Thread(() -> {
            funcCommonOutput(o1, "thread", "o1");
        });
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Thread thread2 = new Thread(() -> {
            funcCommonOutput(o1, "thread2", "o1");
        });
        thread2.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("o1最后锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
    }

    /**
     * 为了验证上面的说法 使用 String 对象作为锁对象 因为他的 hashcode 方法是重写过的 (项目中不要使用 String 是有常量池的可能会出意料外的问题)
     * 结果上确实和预期相同 匿名偏向 -> 偏向锁态 -> 轻量级锁 -> 无锁态
     * 同时也证明了一开始的问题 偏向锁到轻量级锁 结论就是 只要有一个线程获取了偏向锁 不管第二个线程获取的时候第一个线程是否死了都会升级为轻量级锁
     */
    private static void test_1_2(){
        String o1 = "ABCF";
        System.out.println(o1.hashCode());
        System.out.println("o1初始的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
        Thread thread = new Thread(() -> {
            funcCommonOutput(o1, "thread", "o1");
        });
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        funcCommonOutput(o1, "主线程", "o1");
        Thread thread2 = new Thread(() -> {
            funcCommonOutput(o1, "thread2", "o1");
        });
        thread2.start();
    }

    /**
     * 这里测试重量级锁的条件 和预想的一样
     * 至于这里所谓的偏向 到 轻量级锁 （中间还有个自旋） 再到 重量级锁 的过程不知道该怎么测试 反正用这个依赖包可以看到的只有重量级锁
     * 而且测试下来 哪怕 下面2个 sleep 的时间是相同的 （不考虑输入方法的话） 也有可能升级到重量级锁
     * 所以就我的结论上来看这个自旋的时间起码不是人可以感知的那种（比较 cpu 做个 cas 操作才需要多久）至于有没有用不清楚追求高并发的如果有测试过的可以告知一下
     */
    private static void test2(){
        Object o1 = new Object();
        //由于依赖可能需要预热 会导致第一次方法调用慢而获取的数据存在问题
        System.out.println("01的初始状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
        Thread thread = new Thread(() -> {
            synchronized (o1){
                System.out.println("01在thread的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("01在thread的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
            }
        });
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (o1){
                System.out.println("01在thread2的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
            }
        });
        thread.start();
        thread2.start();
    }


    /**
     * 这里就是验证一下 轻量级锁 到 重量级锁 结果符合预期
     */
    private static void test3(){
        Object o1 = new Object();
        //由于依赖可能需要预热 会导致第一次方法调用慢而获取的数据存在问题
        synchronized (o1){
            System.out.println("01的初始状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
            System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
        }
        Thread thread = new Thread(() -> {
            synchronized (o1){
                System.out.println("01在thread的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("01在thread的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
            }
        });
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (o1){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("01在thread2的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(o1).getLockStatus());
                System.out.println(MarkWordFactory.getMarkword(o1).getMarkWordStr());
            }
        });
        thread.start();
        thread2.start();
    }

    /*
    *
    * */
    /**
     * 通用的一个输出方法
     */
    private static void funcCommonOutput(Object lock, String threadName, String lockName){
        System.out.println(lockName + "在" + threadName + "的同步代码块前的锁状态 => " + MarkWordFactory.getMarkword(lock).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(lock).getMarkWordStr());
        synchronized (lock){
            System.out.println(lockName + "在" + threadName + "的同步代码块中的锁状态 => " + MarkWordFactory.getMarkword(lock).getLockStatus());
            System.out.println(MarkWordFactory.getMarkword(lock).getMarkWordStr());
        }
        System.out.println(lockName + "在" + threadName + "的同步代码块后的锁状态 => " + MarkWordFactory.getMarkword(lock).getLockStatus());
        System.out.println(MarkWordFactory.getMarkword(lock).getMarkWordStr());
    }
}
