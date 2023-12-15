# synchronized-header-analysis
在 HotSpot 25.271-b09 虚拟机下 synchronized 关键字对锁的对象头中的 markword 的影响 -> 锁升级的过程
#### 起因

这是一个测试用的项目起因是在刷面试题的时候看到了 关于 synchronized 锁升级方面的争论 一是偏向锁到轻量级锁的原因 二是自旋部分是属于重量级锁还是轻量级锁。我主要是对第一个问题做了一些测试因为原文章没有代码都是一些理论知识。至于第二个的话由于 synchronized 是 JNI 的范畴,我个人 C 的能力薄弱是看不太懂了，只对这 2 种说法进行一下个人理解的阐述

##### 关于自旋部分是重量级锁还是轻量级锁

最先这个问题是从 [乐观锁和悲观锁详解 | JavaGuide(Java面试 + 学习指南)](https://javaguide.cn/java/concurrent/optimistic-lock-and-pessimistic-lock.html#总结) 中然后从参考文章里跳转到了 [通俗易懂 悲观锁、乐观锁、可重入锁、自旋锁、偏向锁、轻量/重量级锁、读写锁、各种锁及其Java实现！ - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/71156910) 这篇文章，然后看评论区又到了 [关于 Synchronized 的一个点，网上99%的文章都错了_Java_yes_InfoQ写作社区](https://xie.infoq.cn/article/d9479ba8900bd7645c035d006) 这篇文章。

整体看了一遍(最后一篇就简单看了下，C的代码真的不太行)，我个人认为的话这应该算是一个定义问题。第二篇中对轻量级锁的定义是有自旋操作的是轻量级锁的原因是认为 **短时间的忙等，换取线程在用户态和内核态之间切换的开销** 我的理解是短时间的忙等消耗的系统资源小于 切换线程导致的上下文切换消耗的系统资源，所以自旋是轻量的而导致系统发送上下文切换的锁是重量级的（如果忙等时间过长自旋浪费的反而可能大于上下文切换）。而第三篇是看了源码发现自旋相关的代码是在锁的状态变成重量级锁之后才触发的，所以认为自旋锁属于重量级锁的范畴（这里的锁的状态可能是指后面会提到的一张图）。所以我觉得都可以理解，如果认为第二篇是对的话那可以说锁的状态只是人为定义的你想把自旋部分写在 synchronized 的轻量级锁状态下的代码也可以写在重量级的也可以重点是定义了自旋锁就是轻量级锁，如果绝对第三篇是对的话那就是认同写这部分代码的人的观点。

##### 偏向锁到轻量级锁的原因

###### 观点一

线程A获得偏向锁之后不会主动释放 当线程B获取偏向锁的时候如果线程A还存在升级为轻量级锁。如果线程A不存在了则锁不升级并且偏向锁由B获取。同时也是第二篇文章作者的观点

###### 观点二

只要有一个线程获取了偏向锁 不管第二个线程获取的时候第一个线程是否死了都会升级为轻量级锁 

上面2个观点第二个线程获取的时候同步代码块是都已经结束了或者快结束了不然可能会变成重量级锁

###### 结论

观点二是对的 **但是测下来有很多匪夷所思的结果！！！！**

### 过程

#### 环境

主要软件、依赖的版本

**JDK 版本 1.8.0_271**

**org.openjdk.jol 0.9**

应该没什么关系

**maven 3.8.2**

#### 原理

其实一开始是一头雾水因为写了这么个代码中间打断点啥都看不到，属于是知识盲区了。

```java
        synchronized (x){
            System.out.println();
        }
```

后来查了查找到了这么个依赖

```xml
        <dependency>
            <groupId>org.openjdk.jol</groupId>
            <artifactId>jol-core</artifactId>
            <version>0.9</version>
        </dependency>
```

调用最基础的方法

```java
System.out.println(ClassLayout.parseInstance(lock).toPrintable());
```

可以读取到对象的头信息是这样个格式

```
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           09 de 9b 67 (00001001 11011110 10011011 01100111) (1738268169)
      4     4        (object header)                           3c 00 00 00 (00111100 00000000 00000000 00000000) (60)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

人一下子就清醒了是我看不懂的样子。

然后找到了 [JOL：Java 对象内存布局 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/621287001) 这篇文章反正就看了下去

![HotSpot-32-MarkWord](https://raw.githubusercontent.com/SJC1015/synchronized-header-analysis/main/src/main/resources/images/HotSpot-32-MarkWord.webp)

![HotSpot-64-MarkWord](https://raw.githubusercontent.com/SJC1015/synchronized-header-analysis/main/src/main/resources/images/HotSpot-64-MarkWord.jpg)

我的电脑是64位的就看第二张了

由于我用的是最基础的 Object 对象作为锁的在基础的几个状态下试了试

```java
package com.sjc.test.common;

import org.openjdk.jol.info.ClassLayout;

public class Test {

    private static final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("只初始化的时候");
        System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        System.out.println("lock的hashcode" + lock.hashCode());
        System.out.println("输出了 hashcode 的时候");
        System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        synchronized (lock){
            System.out.println("锁住没有出同步代码块的是时候");
            System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }
    }
}

```

  **结果 输出hashcode 的原因是无锁状态下会在 markword 里存放 hashcode 我想根据这个来做定位**

```
只初始化的时候
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

lock的hashcode1013423070
输出了 hashcode 的时候
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 de 9b 67 (00000001 11011110 10011011 01100111) (1738268161)
      4     4        (object header)                           3c 00 00 00 (00111100 00000000 00000000 00000000) (60)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

锁住没有出同步代码块的是时候
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           20 f2 df e7 (00100000 11110010 11011111 11100111) (-404753888)
      4     4        (object header)                           a2 00 00 00 (10100010 00000000 00000000 00000000) (162)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

可以看到对 lock 对象什么方法都没有调用的时候 是真的什么都没有 一开始真的无从下手因为和图中的位置完全对不上。

按照图和文章来看最主要的就是那 3 行 32 位的数字 二 十 十六进制的都给了 其中最后一行按文章说法是 **Class Pointer** 也就是指向类的指针和我要研究的目前没有关系 而且不论在什么状态下都是不变的所以不管。

再看第一行和第二行刚好 64 位和图里的位数一样我本来以为都对上了结果完全不对按图里的顺序不论是无锁还是偏向锁（当前代码只有一个线程）最后一位都应该是 1 但这里第二排最后一位不是 1 而且最后24位都是 0 这么看都这么像图里的 unused 相关的属性。

后来输出了一下 hashcode 转成 16 进制之后明了了 上面的是 1013423070 转成 16 进制是 3C679BDE 刚好和第二个输出里 从第一排第二个到第三排第一个 2 位 16 进制相同只是顺序是反的，那明了了第一排第一个的最后 2 位就是锁的标志位，倒数第三位就是偏向模式的标志位连起来是 001 前面是分代年龄的 4 位和一个 unused 后面加了一个 system.gc() 分代年龄也确实变了证明没错。（虽然后面加了好几个 gc 这个字段没有涨还是 0001 但可能是编译后的代码优化了或者什么吧）第一个全是 0 的原因可能是初始化只有没有使用过所以一些 markword 相关的数据没有存进去 后面调用了方法之后就有了也可以理解。

但是问题来了 第三种情况 这里理应是偏向锁但是原来的 3 个位置上都是 0 连 1 都没了

**(这里实际上是轻量级锁，原因是 JVM 启动后的 4 秒内 偏向锁是不触发的原因不阐述了所以要么在最前面加上睡眠 4 秒或者在启动命令里加上 -XX:BiasedLockingStartupDelay=0 )** 

这时候我就发现不对了 不管是前面 无锁的时候 hashcode 的顺序、 锁标志的位置 还是后面偏向模式标志的位置 都存在很大的偏移或者说重排序，就只能看源码了还好不是特别复杂，最后找到了乱序的原因

```java
VirtualMachine vm = VM.current();
for (long off = 0; off < headerSize(); off += 4) {
    int word = vm.getInt(instance, off);
    pw.printf(" %6d %5d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n", off, 4, "", "(object header)",
                    toHex((word >> 0)  & 0xFF) + " " +
                    toHex((word >> 8)  & 0xFF) + " " +
                    toHex((word >> 16) & 0xFF) + " " +
                    toHex((word >> 24) & 0xFF) + " " +
                    "(" +
                    toBinary((word >> 0)  & 0xFF) + " " +
                    toBinary((word >> 8)  & 0xFF) + " " +
                    toBinary((word >> 16) & 0xFF) + " " +
                    toBinary((word >> 24) & 0xFF) + ") " +
                    "(" + word + ")"
    );
}
```

我也没特别详细的看 大概意思就是获取当前的 jvm 机器的对象 然后他有一个方法可以根据偏移位（off）和对象（instance）获取头信息他这里是从 0 开始每 4 个字节（也就是 32 位的二进制数刚好是 int 的长度）读取一次并且输出然后把 最右边8位 先输出 最左边8位最后输出。

就我个人而言我觉得有点反人类了，不清楚是不是这种协议的输出有格式要求，不过这个依赖本身也确实不是只用来看 synchronized 相关信息的。所以我就自己写了几个简单的类来更直观的输出信息。

核心就是这 2 行代码 

```
        VirtualMachine vm = VM.current();
        final long markWord = vm.getLong(o, 0);
```

因为我只关心前 64 位的值所以我直接用一个 long 全部获取到了

具体代码就不贴出来了不多也比较简单

#### 测试过程

###### 1、测试一个对象由主线程创建使用和不使用 hash code 的情况下的变化方式

对应测试项目的 com.sjc.Test.test_1() 方法

一开始是因为定位 hashcode 再 markword 的位置后面发现调用 hashcode 和不调用 hashcode 锁的初始状态不同

执行后发现了很多问题 这些问题的根本是在认同那张 64 位 HotSpot 虚拟机对象头 Mark word 对应表的图的情况下
1、o2在线程中的同步代码块中的锁状态 是偏向锁 但是他的偏向线程 ID 我找不到 可能是线程的某个特殊值吧 这个问题还好
2、为什么对象在调用了 hashcode 方法后锁状态直接就是无锁而不调用反而是偏向锁？
2-1、 而且为什么无锁的 o1 到了线程的同步代码块中时会变成轻量级锁？
2-2、 反而 o2 原来就是偏向锁结果还是偏向锁而且原来空的 偏向线程 ID 有了值（虽然我找不到这个值在线程对象中的位置) 但
3、同时使用了 debug 来测试发现结果差很多而且和断点无关如 2 个对象到了线程中的同步代码块里都是轻量级锁？

###### 2、调查 hashcode 对 markword 产生影响的原因

对应测试项目的 com.sjc.Test.test_1_1() 方法

这个是对上面的问题进行一些想法的测试

测试结果是 还是除了一开始的偏向锁状态 其他状态 都在预料中

但是有了一个惊人的发现那就是被主线程使用 synchronized 获取过一次锁之后再由一个线程获取锁变成了轻量级这里是在预料中 但是出来之后和最后的状态都是无锁

根据上面的现象我给出了上面 2 和 2-1 的答案的可能性那就是 hashcode 代码可能开了一个线程并锁定了自己所以在使用

然后就去找 HotSpot hashcode 相关的文章 找到了这个 [HashCode方法的调用对Java锁的影响-CSDN博客](https://blog.csdn.net/nibonnn/article/details/124026493)

问题得到了解决： 原因是原生的 hashcode 方法是通过随机数计算所以只会计算一次并存在 对象的 mark word中

以下是我的理解 由于 hashcode 的重要性所以必须有个地方存放 hashcode

 * 无锁态下 hashcode 存在 mark word 里
 * 偏向锁态下 hashcode 不可能存放在 偏向线程 id 里不说多个锁的情况下但是线程会死亡这一点就不可取
 * 轻量级锁 hashcode 存放在调用栈中锁记录的指针指向的对象的信息中出同步代码块后变为无锁态并将 hashcode 数据放回 mark word 中
 * 重量级锁 和轻量级锁类似放在重量级锁的对象信息中

并且对象最开始的状态也并不是无锁态而是匿名偏向不过我觉得这个和文章最开始的 自旋锁部分是轻量级还是重量级锁一样也是个定义问题毕竟这个匿名偏向我的理解是一个特殊的无锁态因为变成真正的无锁态之后就无法到达偏向锁态了

###### 3、改用 String 而不是 Object 作为锁对象

对应测试项目的 com.sjc.Test.test_1_2() 方法

为了验证上面的说法 使用 String 对象作为锁对象 因为他的 hashcode 方法是重写过的 (项目中不要使用 String 是有常量池的可能
结果上确实和预期相同 匿名偏向 -> 偏向锁态 -> 轻量级锁 -> 无锁态
同时也证明了一开始的问题 偏向锁到轻量级锁 结论就是 只要有一个线程获取了偏向锁 不管第二个线程获取的时候第一个线程是否死了都会升级为轻量级锁



###### 4、测试重量级锁的条件

对应测试项目的 com.sjc.Test.test2() 方法

这里测试重量级锁的条件 和预想的一样

至于这里所谓的偏向 到 轻量级锁 （中间还有个自旋） 再到 重量级锁 的过程不知道该怎么测试 反正用这个依赖包可以看到的只有重量级锁

而且测试下来 哪怕 下面2个 sleep 的时间是相同的 （不考虑输入方法的话） 也有可能升级到重量级锁

所以就我的结论上来看这个自旋的时间起码不是人可以感知的那种（比较 cpu 做个 cas 操作才需要多久）至于有没有用不清楚追求高并发的如果有测试过的可以告知一下



#### 结果

这里就放2个流程图

###### 不调用 native hashcode 的情况

![synchronized-noHashcode-flow-chart](https://raw.githubusercontent.com/SJC1015/synchronized-header-analysis/main/src/main/resources/images/synchronized-noHashcode-flow-chart.jpg)

###### 调用 native hashcode 的情况

![synchronized-useHashcode-flow-chart](https://raw.githubusercontent.com/SJC1015/synchronized-header-analysis/main/src/main/resources/images/synchronized-useHashcode-flow-chart.jpg)

这里关于 native hashcode 相关的猜测

1、我看一些文章中说轻量级锁的时候hashcode 值也是放在重量级锁对象里的但是轻量级锁状态下又没有关于重量级锁的指针信息最后返回无锁态的时候怎么再获取到hashocode 值呢？所以我觉得轻量级锁的hashcode值时放在 调用栈中的锁记录（只是猜测
2、升级到重量级锁的过程中肯定会有轻量级锁那hashcode 值应该是由从调用栈的信息中转移到重量级锁对象的变量的过程吧（只是猜测