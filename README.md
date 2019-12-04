# Spring Boot Gracefully Shutdown



## 1. 概述

对于生产环境的系统来说，应用程序的生命周期至关重要。可以优雅的关闭系统，也是判断程序员是能对系统掌控能力的核心指标之一。
这里我们来讨论一下基于`Spring Boot`优雅关机的常见场景。



## 2. 原理知识

当程序运行时，操作系统调度器加载到内存，分配进程ID，进入待执行状态，当程序分配到`时间片`后，CPU的程序计数器`PC`指向进程代码的入口地址，依次执行后加一，保证程序正序执行。如果有中断，进程进入暂停，保留现场现场数据等待后续回复。其中涉及到进程暂停或者终止的主要有`硬中断`、`软中断`和`信号`。

- 硬中断：由其他硬件设备产生对CPU的中断。如键盘，电源关机等；
- 软中断：通常由硬中断服务程序对内核中断，可CPU内部产生，也可以有程序安排调用中断服务程序，系统调用。如故障处理，陷阱指令和`INT`指令等；
- 信号：异步进程之间的通信机制，由内核或其他进程对某个进程进行中断。例如`TERM`, `KILL`等等。这里我们重点讨论信号。



### 2.1 信号初探

我们主要讨论的是信号，这里先做改命令做一些介绍工作：

`kill -l` 列出当前系统所以的信号编号`Signal`，该列表其实就是一个枚举，后面跟着信号编号就能打印出对应的数值，我们常用的 `kill `就是默认的`kill -s TERM`, `kill -9` 就是`kill -s KILL`。

```bash
➜  ~ kill -l
HUP INT QUIT ILL TRAP ABRT EMT FPE KILL BUS SEGV SYS PIPE ALRM TERM URG STOP TSTP CONT CHLD TTIN TTOU IO XCPU XFSZ VTALRM PROF WINCH INFO USR1 USR2
➜  ~ kill -l HUP
1
➜  ~ kill -l INT
2
➜  ~ kill -l QUIT
3
➜  ~ kill -l KILL
9
➜  ~ kill -l TERM
15
➜  ~ kill -l ABRT
6
➜  ~ kill -l SEGV
11
```

如果使用Linux则会显示更多的信号编号：

```bash
[~]$ kill -l
 1) SIGHUP	 2) SIGINT	 3) SIGQUIT	 4) SIGILL	 5) SIGTRAP
 6) SIGABRT	 7) SIGBUS	 8) SIGFPE	 9) SIGKILL	10) SIGUSR1
11) SIGSEGV	12) SIGUSR2	13) SIGPIPE	14) SIGALRM	15) SIGTERM
16) SIGSTKFLT	17) SIGCHLD	18) SIGCONT	19) SIGSTOP	20) SIGTSTP
21) SIGTTIN	22) SIGTTOU	23) SIGURG	24) SIGXCPU	25) SIGXFSZ
26) SIGVTALRM	27) SIGPROF	28) SIGWINCH	29) SIGIO	30) SIGPWR
31) SIGSYS	34) SIGRTMIN	35) SIGRTMIN+1	36) SIGRTMIN+2	37) SIGRTMIN+3
38) SIGRTMIN+4	39) SIGRTMIN+5	40) SIGRTMIN+6	41) SIGRTMIN+7	42) SIGRTMIN+8
43) SIGRTMIN+9	44) SIGRTMIN+10	45) SIGRTMIN+11	46) SIGRTMIN+12	47) SIGRTMIN+13
48) SIGRTMIN+14	49) SIGRTMIN+15	50) SIGRTMAX-14	51) SIGRTMAX-13	52) SIGRTMAX-12
53) SIGRTMAX-11	54) SIGRTMAX-10	55) SIGRTMAX-9	56) SIGRTMAX-8	57) SIGRTMAX-7
58) SIGRTMAX-6	59) SIGRTMAX-5	60) SIGRTMAX-4	61) SIGRTMAX-3	62) SIGRTMAX-2
63) SIGRTMAX-1	64) SIGRTMAX
```


常用的有以下几个信号：

| 数值 | 信号                                     | 解释                                                         |
| :--- | :--------------------------------------- | :----------------------------------------------------------- |
| 1    | HUP (hang up)                            | 当用户离开正在运行某个程序的终端时，HUP命令就会发送到应用程序里面。想象一下，当我们登录远程Linux命令的时候，如果没有用 nohup 进行控制，断线后重连就发现程序停止了，其实是无主动意识的，这和TERM主动关闭是最大的区别。 但是对于后台运行程序，另外一层语义则是重新加载完配置文件 |
| 2    | INT (interrupt)                          | Ctrl+C, 非交互程序通常按照 SIGTERM 来处理                    |
| 3    | QUIT (quit)                              | Ctrl+\ 或者 Command+Q，就是应用程序异常行为发生后，需要结算是发送的命令，并伴随着一个 dump 文件生成，用于诊断 (ulimit -c unlimited 要打开) |
| 6    | ABRT (abort)                             | 通常由内存分配或者访问造成，并伴随着一个 dump 文件生成，用于诊断 (ulimit -c unlimited 要打开) |
| 9    | KILL (non-catchable, non-ignorable kill) | 不可忽略的终止信号，应用程序会立即强制被关闭。 如果是一个占用用了大量内存的僵尸程序 结合 free 可以释放内存 |
| 11   | SEGV                                     | 无效的内存地址，导致系统异常退出的信号，此时JVM表现出来为Crash |
| 14   | ALRM (alarm clock)                       | 定时时钟到期后，会发送报警信号，可以大概比喻为JavaScript的setTimeout或者setInterval |
| 15   | TERM (software termination signal)       | 程序终止信号，也就是我们应用程序正常的终止信号               |

更多详细命令参考一下[链接](https://github.com/nu11secur1ty/SIGNALS-IN-LINUX/blob/master/signal.md)

JVM使用到的信号有：

| 信号                                               | 描述                                                         |
| -------------------------------------------------- | ------------------------------------------------------------ |
| `SIGSEGV`, `SIGBUS`, `SIGFPE`, `SIGPIPE`, `SIGILL` | These signals are used in the implementation for implicit null check, and so forth. |
| `SIGQUIT`                                          | This signal is used to dump Java stack traces to the standard error stream. (Optional) |
| `SIGTERM`, `SIGINT`, `SIGHUP`                      | These signals are used to support the shutdown hook mechanism (`java.lang.Runtime.addShutdownHook`) when the VM is terminated abnormally. (Optional) |
| `SIGJVM1` , `SIGJVM2`                              | These signals are reserved for use by the Java Virtual Machine. (Solaris only) |
| `SIGUSR2`                                          | This signal is used internally on Linux and macOS. It is not used by the VM on Solaris. |
| `SIGABRT`                                          | The HotSpot VM does not handle this signal. Instead, it calls the `abort` function after fatal error handling. If an application uses this signal, then it should terminate the process to preserve the expected semantics. |

> Signals tagged as "optional" are not used when the `-Xrs` option is specified to reduce signal usage. With this option, fewer signals are used, although the VM installs its own signal handler for essential signals such as `SIGSEGV`. Specifying this option means that the shutdown hook mechanism will not execute if the process receives a `SIGQUIT`, `SIGTERM`, `SIGINT`, or `SIGHUP`. Shutdown hooks will execute, as expected, if the VM terminates normally (that is, when the last non-daemon thread completes or the System.exit method is invoked).
>
> `SIGUSR2` is used to implement, suspend, and resume on Linux and macOS. However, it is possible to specify an alternative signal to be used instead of `SIGUSR2`. This is done by specifying the `_JAVA_SR_SIGNUM` environment variable. If this environment variable is set, then it must be set to a value larger than the maximum of `SIGSEGV` and `SIGBUS`.


更多详细命令参考一下[链接](https://docs.oracle.com/javase/10/troubleshoot/handle-signals-and-exceptions.htm#JSTGD356)

```java
// 如何使用Java处理底层信号
// 创建一个信号处理器  
sun.misc.SignalHandler handler = new sun.misc.SignalHandler() {  
    @Override  
    public void handle(sun.misc.Signal signal) {  
        // 什么都不做  
    }  
};  

// 设置INT信号(Ctrl+C中断执行)交给指定的信号处理器处理，废掉系统自带的功能  
sun.misc.Signal.handle(new sun.misc.Signal("INT"), handler);  
```



### 2.2 相关命令

命令发出信号的严重程度：kill -3 < kill(-15) < kill -6 < kill -9  

系统相关命令：


| 命令      |      参数     |  说明                       |
|----------|:-------------:|:---------------------------|
| kill     | -15 | 程序正常终止信号                   |
| kill     |  -3 pid       | 程序退出信号，标准输出生成stack dump数据 |
| kill     |  -6 pid       | 程序异常终止信号 |
| kill     |  -9 pid       | 程序强制终止信号 |
| tcpdump | -i -s -v | 监听网络服务例，如Mac下 sudo tcpdump -i en0 -s 0 -v tcp and port 80 |

Java相关命令：

| 其他命令      |      参数     |  说明                   |
|----------|:--------------|:---------------------------|
| jps       |     -lv       | 找到Java进程               |
| jstack    |  -l pid       | 等价于 kill -3，如果指定输出，也可生成stack dump数据文件 |
| jcmd     |  pid  GC.run     | 触发JVM GC执行           |
| jmap     | jmap -dump:format=b,file=file.bin  pid    | 生成Java Heap Dump文件 |

- 程序实例: DirectMemoryTest


###2.3 JVM退出流程

- 单进程无其他信号发送：JVM在非守护进程方法执行完会正常退出，main退出的时候，守护进程不一定退出。但是这样的退出状态码一般都是`exit(0)`。

- 当有其他进程发送信号或者代码内有`exit(n)`退出时，系统都不会正常退出，具体流程如下：

  | 类/文件 | 类/文件                                       | 方法                          | 说明                                                         |
  | ------- | --------------------------------------------- | ----------------------------- | ------------------------------------------------------------ |
  | 1       | java.lang.Runtime                             | exit                          |                                                              |
  | 2       | java.lang.Shutdown                            | exit                          | native halt0                                                 |
  | 3       | src/java.base/share/native/libjava/Shutdown.c | Java_java_lang_Shutdown_halt0 | JNIEXPORT void JNICALL<br/>Java_java_lang_Shutdown_halt0<br/>(JNIEnv *env, jclass ignored, jint code){JVM_Halt(code);} |
  | 4       | src/hotspot/share/prims/jvm.cpp               | JVM_Halt                      | JVM_ENTRY_NO_ENV(void, JVM_Halt(jint code))<br/>before_exit(thread);vm_exit(code); |
  | 5       | src/hotspot/share/runtime/java.cpp            | vm_exit                       | JVM_ENTRY_NO_ENV(void, JVM_Halt(jint code))before_exit(thread);m_exit(code); |
  | 6       | src/hotspot/share/runtime/os.hpp              | static void exit(int num);    | Call ::exit() on all platforms but Windows                   |
  |         |                                               |                               |                                                              |

- 程序实例：DirectMemoryTest


## 3. 问题及解决方案

由于使用的技术栈是围绕`Java`以及`Spring Boot`生态，这里我们重点以这个出发点进行开始，讨论服务停止或者异常停止可能造成的问题。



### 3.1 Standalone

#### 3.1.1 单进程的程序

- Spring的默认监听钩子：Spring在`org.springframework.context.support.AbstractApplicationContext`注册了一个默认的系统钩子：

  ```java
  
  /**
    * Register a shutdown hook with the JVM runtime, closing this context
    * on JVM shutdown unless it has already been closed at that time.
    * <p>Delegates to {@code doClose()} for the actual closing procedure.
    * @see Runtime#addShutdownHook
    * @see #close()
    * @see #doClose()
  */
  @Override
  public void registerShutdownHook() {
      if (this.shutdownHook == null) {
            this.shutdownHook = new Thread() {
                public void run() {
                    synchronized(AbstractApplicationContext.this.startupShutdownMonitor) {
                        AbstractApplicationContext.this.doClose();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
  }
  ```

  当系统正常结束或者被外部信号通知关闭时候，会回调该方法，依次处理Spring各个Bean的销毁步骤。
  ```java
      protected void doClose() {
            if (this.active.get() && this.closed.compareAndSet(false, true)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Closing " + this);
                }
    
                LiveBeansView.unregisterApplicationContext(this);
    
                try {
                    this.publishEvent((ApplicationEvent)(new ContextClosedEvent(this))); //发布容器关闭事件
                } catch (Throwable var3) {
                    this.logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", var3);
                }
                // 省略其他部分代码
  ```
  
  下面我们开始讨论各自情况：
- 服务关闭的各种情况：
  - `kill -3 pid`：JVM并不终止，只是生成Stack Dump文件，符合JVM描述的只是将栈信息输出到标准错误错误输出流。
  - `kill pid`：JVM收到信号后，Spring开始销毁对象，最后`interrupted by signal 15: SIGTERM`程序正常退出。
  - `kill -6 pid`：JVM立即终止，终端显示`interrupted by signal 6: SIGABRT`，符合JVM描述的JVM忽略这个信号，直接调用`abort()`函数异常结束程序。
  - `kill -9 pid`：JVM立即终止，终端显示`interrupted by signal 9: SIGKILL`。

- 解决方案：
  - 尽量使用Spring来托管所用的Bean。如果Bean在销毁的时候需要释放或者关闭资源，实现`DisposableBean`或者使用`@PostConstruct`来处理自定义Bean的销毁；
  - 不要轻易使用`kill -6`或者`kill -9`来终止程序，除非确认真的是僵尸进程，不得不处理才这样做，一般来说这样的情况很有可能是发生了`死锁`，需要用`kill -3`或者`jstack`来得到当前栈信息的快照来分析。



#### 3.1.2 自定义线程池的程序

ThreadPoolExecutor 线程池使用默认标准的固定大小，拒绝策略是`AbortPolicy`。
```java
 private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();
```

- 线程池直接硬编码实现：无论是正常还是异常终止程序，当Spring销毁后，JVM直接退出，不会考虑线程池是否执行完成。这个是使用线程池错误示范。

```java 

    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    
    public Optional<String> addUpgradeGray(ReqUpgradeVo vo, List<User> users) {
            UpgradeInfoDto dto = vo.getUpgradeInfoDto();
            int count = upgradeMapper.checkDuplicate(dto);
            if (count > 0) {
                return Optional.of("duplicate upgrade");
            }
            upgradeMapper.addUpgradeInfo(dto);
            Long upgradeId = dto.getId();
            upgradeMapper.addUpgradeGrayMembers(upgradeId, users);
            final String companyCode = Utils.getCommonHeader(CommonHeaderEnum.COMPANY_CODE);
            users.forEach(user -> {
                pool.execute(() -> { 
                    UpgradeInfoDto d = getLatestVersion(dto.getVersion(), dto.getType(), dto.getOs(), user.getUserId());
                    //如果新添加的版本新 则更新redis
                    if (d == null || d.getVersion().equals(dto.getVersion())) {
                        stringRedisTemplate.opsForValue().set(String.format(UPGRADE_GRAYSCALE_REDIS_KEY, companyCode, dto.getType(), dto.getOs(), user.getUserId()), JSON.toJSONString(dto));
                    }
                });
            });
            return Optional.empty();
        }
```
上面这段代码中pool执行的逻辑，从业务角度讲是必须执行完成的。但是在使用了自定义线程池的情况下，在spring容器关闭时，我们并不能掌握该线程池中线程执行的情况，执行到什么程度。这是十分危险的。


- 线程池硬编码实现，Spring销毁：程序正常终止时，线程池可以考虑以下2种方式来关闭线程池：

  - shutdown：通知已经提交的线程关闭，后续请求不在接收。关闭期间不interrupt提交线程， 也不等待他们是否执行完成，只要`executorService`本身执行代码后，Spring就认为应该退出了。因此必须后面紧跟一个超时设置，否则其他线程可能没有执行完成，也跟着一起关闭了。
                线程池将变成shutdown状态，此时不接收新任务，但会处理完正在运行的 和 在阻塞队列中等待处理的任务

  - shutdownNow()后线程池将变成stop状态，此时不接收新任务，不再处理在阻塞队列中等待的任务，还会尝试中断正在处理中的工作线程。
    
    ```java
    // 关闭线程池，等待10秒退出
    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);
    // 等待10秒
    executorService.shutdownNow();
    ```
    
    1 为什么线程池需要调用shutdown()进行关闭
    > A pool that is no longer referenced in a program <em>AND</em>
 has no remaining threads will be {@code shutdown} automatically. If
 you would like to ensure that unreferenced pools are reclaimed even
 if users forget to call {@link #shutdown}, then you must arrange
 that unused threads eventually die, by setting appropriate
 keep-alive times, using a lower bound of zero core threads and/or
 setting {@link #allowCoreThreadTimeOut(boolean)}. 
 
 [线程中断机制](https://www.jianshu.com/p/e0ff2e420ab6)
    
    2 shutdown() 和 shutdownNow() 的处理逻辑:
    TODO
    
- 代码一：
    App.java 
        不开启@PreDestroy shutdown()时程序立即退出
        开启@PreDestroy shutdown()程序等到设置时间到期后线程退出
    SpringApp.java
        同理

- 使用Spring托管的线程池：这次我们使用Spring自带的线程池，查看表现行为：

  - 代码一：

    ```java
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }
    ```

  - 现象一：`kill pid` 执行后，我们可以观察到，Spring会同时执行`shutdownNow`，立即通知关闭线程。

    

  - 代码二：

    ```java
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor2() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setThreadNamePrefix("default_task_executor_thread2");
        executor.initialize();
        return executor;
    }
    ```

  - 现象二：`kill pid` 执行后，我们可以观察到，Spring会等待一定的时候，然后才退出。

    

  - 结论：查看`org.springframework.scheduling.concurrent.ExecutorConfigurationSupport`我们可以看到当Spring的线程池销毁时，根据不同的性质，会使用不同的关闭策略。因此尽量使用Spring的线程池能更多的减少隐患发生的概率。

    ```java
    
    @Override
    public void destroy() {
        shutdown();
    }
    
    /**
      * Perform a shutdown on the underlying ExecutorService.
      * @see java.util.concurrent.ExecutorService#shutdown()
      * @see java.util.concurrent.ExecutorService#shutdownNow()
      */
    public void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
        }
        if (this.executor != null) {
            if (this.waitForTasksToCompleteOnShutdown) {
                this.executor.shutdown();
            }
            else {
                for (Runnable remainingTask : this.executor.shutdownNow()) {
                    cancelRemainingTask(remainingTask);
                }
            }
            awaitTerminationIfNecessary(this.executor);
        }
    }
    ```

    

#### 3.1.3 Servlet容器程序

- 服务访问请求的情况：

  - 访问请求时间过长：假设某个请求时间过长，当我们关闭JVM的时候，servlet请求很可能还没有执行完成，线程就被迫中断退出了，很有可能出现数据修改异常状态；
  - 新的请求进来：当JVM正在关闭的时候，新的请求策略参见上述线程池的关闭策略——即新的请求一律直接拒绝服务。

- 优雅关机的标准:
    - 在对应用进程发送停止指令之后，能保证正在执行的业务操作不受影响。应用接收到停止指令之后的步骤应该是，停止接收访问请求，等待已经接收到的请求处理完成，并能成功返回，这时才真正停止应用。

- 解决方案：

  - 实现`TomcatConnectorCustomizer`并且注册到Spring的`ApplicationListener<ContextClosedEvent>`，监听Spring容器销毁时，如何销毁自定义的Tomcat容器。这里我们定义自己的Tomcat在30秒内，如果还有servlet线程没有执行完成，则强行关闭。

    ```java
    public class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {
        private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);
        private volatile Connector connector;
    
        @Override
        public void customize(Connector connector) {
            this.connector = connector;
        }
    
        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            this.connector.pause();
            Executor executor = this.connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                try {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                    threadPoolExecutor.shutdown();
                    if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        log.warn("Tomcat thread pool did not shut down gracefully within "
                                + "30 seconds. Proceeding with forceful shutdown");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    ```

    通过上述代码我们可以看到，这里我们使用的是shutdown而且不是shutdownNow是因为，一般在servlet里面是很少监听interrupt事件的，所以尽可能的等待servlet线程完成在退出。

    另外就是servlet执行的程序时间不能过长，最好有一个超时时间进行提前防御。
    
    过长的servlet程序就要考虑是否进行异步。
- 处理结果
    正常访问在timeout之前得到返回值
    在kill 之后访问情况得到返回值：Connection reset by peer    

#### 3.1.3.1 Servlet 异步处理方案
- 直接异步处理方式
```java 
@GetMapping("asyncDeferred")
    public DeferredResult<String> deferredResult() {
        logger.error("async start");
        DeferredResult<String> deferredResult = new DeferredResult<>();
        CompletableFuture.supplyAsync(() -> execute())
                .whenCompleteAsync((result, throwable) -> deferredResult.setResult(result));
        logger.error("async end");
        return deferredResult;
    }
```
创建一个线程并将结果set到DeferredResult。
用completablefuture创建一个异步任务。这将创建一个新的线程，在那里我们的长时间运行的任务将被执行。也就是在这个线程中，我们将set结果到DeferredResult并返回。默认情况下，在completablefuture的supplyasync方法将在forkjoin池运行任务。

    运行结果当前线程执行完成，execute()方法执行并未完成
    `
    [com.guo.demo.controller.AsyncController] [http-nio-9090-exec-2] [32] [ERROR] async start
[com.guo.demo.controller.AsyncController] [http-nio-9090-exec-2] [36] [ERROR] async end
Process finished with exit code 143 (interrupted by signal 15: SIGTERM)
    `
    
    
    问题在于该线程池的执行情况不由我们控制，所以为了更优雅的关闭结合上面提到的线程池做再加工。
    

- 结合线程池处理方式
```Java 
 @GetMapping("asyncDeferredWithOwnThread")
    public DeferredResult<String> deferredResultWithOwnThread() {
        logger.error("async with myThreadPool start");
        DeferredResult<String> deferredResult = new DeferredResult<>();
        CompletableFuture.supplyAsync(() -> execute(), executor)
                .whenCompleteAsync((result, throwable) -> deferredResult.setResult(result));
        logger.error("async with myThreadPool end");
        return deferredResult;
    }
```
运行结果显示当前线程运行完成，在我们自己线程池中的execute()方法也执行完成。

#### 3.1.4 使用消息队列

- 全局事务：如果所使用的消息队列，例如`JMS`支持事务，那么在执行业务方法的时候，可以使用`JTA`对数据源进行统一托管，确保消息和数据库操作保持一致。

  - 使用方法一：声明式的处理方法

    ```java
    // 数据源封装使用XA
    @Bean("dataSourceAccount")
    public DataSource dataSource() throws Exception {
        return createHsqlXADatasource("jdbc:hsqldb:mem:accountDb");
    }
    
    @Bean("dataSourceAudit")
    public DataSource dataSourceAudit() throws Exception {
        return createHsqlXADatasource("jdbc:hsqldb:mem:auditDb");
    }
    
    private DataSource createHsqlXADatasource(String connectionUrl) throws Exception {
        JDBCXADataSource dataSource = new JDBCXADataSource();
        dataSource.setUrl(connectionUrl);
        dataSource.setUser("sa");
        BitronixXADataSourceWrapper wrapper = new BitronixXADataSourceWrapper();
        return wrapper.wrapDataSource(dataSource);
    }
    
    @Bean("jdbcTemplateAccount")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSourceAccount") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean("jdbcTemplateAudit")
    public JdbcTemplate jdbcTemplateAudit(@Qualifier("dataSourceAudit") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    // Service代码不变
    @Service
    public class UtilisateurService {
    
        @Autowired
        private UtilisateurRepository utilisateurRepository;
    
        @Autowired
        private SendMessage sendMessage;
    
        @Transactional(rollbackOn = java.lang.Exception.class)
        public Utilisateur create(Utilisateur utilisateur) throws Exception {
            final Utilisateur result = utilisateurRepository.save(utilisateur);
            sendMessage.send("creation utilisateur : " + result.getId());
            throw new Exception("rollback");
            //return result;
        }
    }
    
    @Component
    public class SendMessage {
    
        private final JmsMessagingTemplate jmsMessagingTemplate;
    
        @Value("${jms.queue.destination}")
        private String destinationQueue;
    
        @Autowired
        public SendMessage(JmsMessagingTemplate jmsMessagingTemplate) {
            this.jmsMessagingTemplate = jmsMessagingTemplate;
            // 开启事务，前提是JMS得支持
            this.jmsMessagingTemplate.getJmsTemplate().setSessionTransacted(true);
        }
    
        public void send(String msg) {
            this.jmsMessagingTemplate.convertAndSend(destinationQueue, msg);
        }
    
    }
    ```

  - 使用方法二：编程式的处理方式，注意Spring使用的事务是`TransactionTemplate`

    | JavaEE API | Spring Utility                                               | Configured With                                              |
    | ---------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
    | JDBC       | `org.springframework.jdbc.core.JdbcTemplate`                 | `javax.sql.DataSource`                                       |
    | JMS        | `org.springframework.jms.core.JmsTemplate`                   | `javax.jms.ConnectionFactory`                                |
    | JTA        | `org.springframework.transaction.support.TransactionTemplate` | `org.springframework.transaction.PlatformTransactionManager` |

    ```java
    // Create or get from ApplicationContext or injected with @Inject/@Autowired.
    JmsTemplate jms = new JmsTemplate(...);
    JdbcTemplate jdbc = new JdbcTemplate(...);
    TransactionTemplate tx = new TransactionTemplate(...);
    
    tx.execute((status) -> {
        // Perform JMS operations within transaction.
        jms.execute((SessionCallback<Object>)(session) -> {
            // Perform operations on JMS session
            return ...;
        });
        // Perform JDBC operations within transaction.
        jdbc.execute((ConnectionCallback<Object>)(connection) -> {
            // Perform operations on JDBC connection.
            return ...;
        });
        return ...;
    });
    
    ```

  - 优缺点：处理异常情况比较方便，代码比较统一，缺点就是性能不高。

    

- 本地事务：

  - 先提交后发消息：这个思路是在业务`service`的执行完成后才调用消息队列发送。

    - 代码示例：

      ```java
      @Controller
      public class UserController {
      
          @Autowired
          private UserService userService;
          
          @Autowired
          private SendMessage sendMessage;
      
          @GetMapping
          public String create(UserDTO userDTO) {
              var userId = userService.save(userDTO);
              // 可能执行到这里，系统关闭了或者报错
              sendMessage.send(userId);
              return "success";
          }
      }
      
      ```

    - 优缺点：性能最高，消息可能不能保证发送成功。

      

  - 两阶段消息：在`service`里面预先发送一个`preMsg`通知其他服务注意有业务发生，在执行完成后，在发送另外一个`confirmMsg`，告诉消息发送成功。

    - 代码示例：

      ```java
      
      @Service
      public class UserService {
      
          @Autowired
          private KafkaService kafkaService;
          
          @Autowired
          private UserRepository userRepository;
      
          @Transactional
          public String create(UserDTO userDTO) {
              var userId = IDGenerator.next();
              var success = kafkaService.send("user.topic.preMsg", userId);
              if(!success) throw new RuntimeException("消息队列发送错误");
              userRepository.save(userDTO);
              return userId;
          }
      }
      
      
      @Controller
      public class UserController {
      
          @Autowired
          private UserService userService;
          
          @Autowired
          private KafkaService kafkaService;
      
          @GetMapping
          public String create(UserDTO userDTO) {
              var userId = userService.save(userDTO);
              // 可能执行到这里，系统关闭了或者报错
              kafkaService.send("user.topic.postMsg", userId);
              return "success";
          }
      }
      
      
      @Service
      public class KafkaBackground {
          
          @Autowired
          JdbcService jdbc;
          
          @Autowired
          private KafkaService kafkaService;
          
          @Autowired
          UserFegin api;
          
      	// 预存第一阶段的消息
          @EventListener("user.topic.preMsg") 
      	public void acceptLocal(RemoteApplicationEvent event) {
        		jdbc.insert(event.userId);
      	}
          
          // 事务提交成功后
          @EventListener("user.topic.postMsg") 
      	public void acceptLocal(RemoteApplicationEvent event) {
              var userId = event.msg;
              // 删除第一阶段消息
              var affected = jdbc.delete(userId);
              if(affected <= 0) throw new RumtimeException("消息删除失败。");
              if(event.code == "success") {
                  // 如果成功发送第二阶段消息
                  kafkaService.send("user.topic.consumerMsg", userId);
              }
      	}
          
          // 每30秒追踪一下第一阶段的消息是否被消费了
          @Scheduled(cron = "0/30 * * * * ?")
         	public void runTask() {
          	val keys = jdbc.selectAll();
              keys.forEash(key -> {
                  var status = api.check(key);
                  if(status == 0) {
                      // 判断用户状态是否创建成功
                      var affected = jdbc.delete(key);
                      if(affected <= 0) log.error("消息删除失败。");
                      if(event.code == "success") {
                          // 如果成功发送第二阶段消息
                          kafkaService.send("user.topic.consumerMsg", userId);
                      }
                  } else {
                      // 其他情况
                  }
              });
        	}
      
      }
      
      ```

    - 优缺点：最复杂，性能介于JTA和局部事务之间，依靠最终一致性解决问题，是目前对一致性和性能都有要求的折中选择。

    

#### 3.1.5 使用外部接口的情况

这里说的外部接口是指不具备任何事务的接口请求。

- 普通调用：如果调用失败，就直接失败，最多做几次重试处理。重试的逻辑用2的幂次方来设置重试调用间隔时间。

- 柔性事务调用：

  - 非幂等性：参见上面的`两阶段消息`，通过消息队列来实现。

  - 补偿表：原理和`两阶段消息`类似，操作的时候，先用数据库保存一下，然后在调用成功后，设置为成功。在通过一个`Job`定期扫描还没有设置为最终状态的记录，重新反查一下。

    - 代码：

      ```java
      
      @Service
      public class UserService {
      
          @Autowired
          private UserRepository userRepository;
      
          @Autowired
          private CheckingService checkingService;
          
          @Transactional
          public String create(UserDTO userDTO) {
              var userId = IDGenerator.next();
              userRepository.save(userDTO);
              checkingService.save(userId);
              return userId;
          }
      }
      
      
      @Controller
      public class UserController {
      
          @Autowired
          private UserService userService;
          
          @Autowired
          private AccountFegin accountFegin;
          
          @Autowired
          private CheckingService checkingService;
          
          @GetMapping
          public String create(UserDTO userDTO) {
              var userId = userService.save(userDTO);
              // 可能执行到这里，系统关闭了或者报错
              accountFegin.create(userId);
              checkingService.update(userId);
              return "success";
          }
      }
      
      
      @Service
      public class KafkaBackground {
          
          @Autowired
          private CheckingService checkingService;
          
          @Autowired
          private AccountFegin accountFegin;
          
          // 每30秒追踪一下是否有访问外部接口失败的情况
          @Scheduled(cron = "0/30 * * * * ?")
         	public void runTask() {
          	val keys = checkingService.selectAll();
              keys.forEash(key -> {
                  var status = accountFegin.check(key);
                  if(status == 0) {
                      // 判断是否账号创建成功
                      accountFegin.create(key);
                      var affected = checkingService.update(key);
                      if(affected <= 0) log.error("更新失败。");
                  } else {
                      // 其他情况
                  }
              });
        	}
      
      }
      ```

      

    - 优缺点：性能比`两阶段消息`要差，如果消息过多时候，数据库压力会比较大。当调用外部接口完成，但自身状态更新失败时，被调用方可能会被调用多次，因此被调用方需要实现`幂等`操作，确保没有副作用发生。



### 3.2 HTTP请求关闭

​	现在我们来分析一下使用HTTP请求来关闭应用程序的情况，这里主要有两种解决方案：

#### 3.2.1 使用Spring的`actuator`

- 示例代码：

  ```xml
  <!-- 配置actuator -->
  <dependency>
  	<groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  ```

  启用允许URL关闭程序：

  ```properties
  management.endpoints.web.exposure.include=*
  management.endpoint.shutdown.enabled=true
  ```

  当程序启动后，访问：

  ```bash
  ➜ curl -X POST localhost:8080/actuator/shutdown
  {"message":"Shutting down, bye..."}%       
  
  ```

  会触发`org.springframework.boot.actuate.context.ShutdownEndpoint`来开始关闭Spring容器。

  观察Spring关闭情况，我们可以得到下面的结论：

  - 如果线程池默认采用的是`shutdown`然后等待子线程结束，但是实际上子线程如果是`while(true)`的话，永远结束不了，*究其原因是这种方式是触发Spring的ApplicationContext关闭，而while(true)的线程并不归Spring来管理*， 是这个地方需要注意一下；
  - 如果线程池采用的是`shutdownNow`，则会立即`interrupt`和终止子线程。

- 结论：这类关闭程序的方法可能存在潜在的问题，尤其是有线程池的情况下，处理不当就无法正常关闭JVM。

  

#### 3.2.2 使用Spring的`ApplicationContext.close()`

- 示例代码：

  ```java
  
  @RestController
  public class TestController implements ApplicationContextAware {
  
      private ApplicationContext context;
  
      @GetMapping("/shutdown")
      public void shutdownContext() {
          ((ConfigurableApplicationContext) context).close();
      }
  
      @Override
      public void setApplicationContext(ApplicationContext ctx) throws BeansException {
          this.context = ctx;
      }
  }
  
  // 或者返回自定义的Spring Code
  @RestController
  public class TestController implements ApplicationContextAware {
  
      private ApplicationContext context;
  
      @GetMapping("/shutdown")
      public void shutdownContext() {
          exitApplication((ConfigurableApplicationContext) context);
      }
  
      @Override
      public void setApplicationContext(ApplicationContext ctx) throws BeansException {
          this.context = ctx;
      }
  
      static void exitApplication(ConfigurableApplicationContext ctx) {
          int exitCode = SpringApplication.exit(ctx, new ExitCodeGenerator() {
              @Override
              public int getExitCode() {
                  // 如果异常退出，返回值0会被具体code替代
                  return 0;
              }
          });
          System.exit(exitCode);
      }
  }
  ```

  访问地址`http://localhost:8080/shutdown`会触发和`actuator`类似的情况，只不过因为关闭Spring容器时间过长，期间还会触发Tomcat超时关闭的日志。

- 结论：该方式和`actuator`完全一致，使用时需要注意的问题也一样。

  

### 3.3 Docker

​	最后讨论一下Docker环境下Java程序的运行情况：

#### 3.3.1 使用Docker的内存问题

- Java8 update 131之前的版本：在机器上，JVM的可用内存和CPU数量并不是Docker允许你使用的可用内存和CPU数量。
- Java8 update 131及以上版本：增加了`-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap`参数，
- Java10 +：增加 `-XX:-UseContainerSupport` 来关闭容器的支持。

时至今日，绝大多数产品级应用仍然在使用Java 8（或者更旧的版本），而这可能会带来问题。Java 8(update 131之前的版本)跟docker无法很好地一起工作。问题是在你的机器上，jvm的可用内存和CPU数量并不是docker允许你使用的可用内存和CPU数量。

比如，如果你限制了你的docker容器只能使用100MB内存，但是呢，旧版本的java并不能识别这个限制。JAVA看不到这个限制。JVM会要求更多内存，而且远超这个限制。如果使用太多内存，Docker将采取行动并杀死容器内的进程！JAVA进程被干掉了，很明显，这并不是我们想要的。。。

为了解决这个问题，你需要给java指定一个最大内存限制。在旧版本的JAVA（8u131之前），你需要在容器中通过设置-Xmx来限制堆大小。这感觉不太对，你可不想定义这些限制两次，也不太想在你的容器中来定义。

幸运的是我们现在有了更好的方式来解决这个问题。从JAVA9之后（8u131+），jvm增加了如下标志:

-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap
这些标志强制jvm检查linux的cgroup配置，docker是通过cgroup来实现最大内存设置的。现在，如果你的应用到达了docker设置的限制（比如500MB），JVM是可以看到这个限制的。JVM将会尝试GC操作。如果仍然超过内存限制，JVM就会做它该做的事情，抛出OutOfMemoryException。也就是说，JVM能够看到docker的这些设置。

从JAVA10之后（参考下面的测试），这些体验标志位是默认开启的，也可以使用-XX:+UseContainerSupport来使能（你可以通过设置-XX:-UseContainerSupport来禁止这些行为）。

#### 3.3.2 使用Docker的CPU问题

- Java 10之前的版本：Docker可能不运行JVM使用Runtime检查到的所有CPU。
- Java 10+：解决了这个问题。

简而言之，JVM将查看硬件并检测CPU的数量。它会优化你的runtime以使用这些CPUs。但是同样的情况，这里还有另一个不匹配，Docker可能不允许你使用所有这些CPUs。可惜的是，这在Java 8或Java 9中并没有修复，但是在Java 10中得到了解决。 从Java 10开始，可用的CPUs的计算将采用以不同的方式（默认情况下）解决此问题（同样是通过UseContainerSupport）。

#### 3.3.3 测试Docker和Java版本

- 内存问题：JDK 7 和 JDK 12 分别测试 (Docker openjdk 从12开始支持获取)

  ```java
  import java.util.ArrayList;
  import java.util.List;
  
  public class MemEat {
      public static void main(String[] args) {
          List l = new ArrayList<>();
          while (true) {
              byte b[] = new byte[1048576];
              l.add(b);
              Runtime rt = Runtime.getRuntime();
              System.out.println("free memory: " + rt.freeMemory() / (1024 * 1024) + " mb");
          }
      }
  }
  ```

  - JDK7：通过命令`docker run -m 100m -it anapsix/alpine-java:7_jdk /bin/bash`。然后在命令行vi生成一个Java测试文件，`javac`后，通过下面的命令进行测试：

    - ` java MemEat`：当使用内存查过100mb后，Docker直接杀死了Java程序。

      ```bash
      bash-4.3# java MemEat
      free memory: 117
      free memory: 116
      free memory: 115
      free memory: 114
      ......
      free memory: 83
      free memory: 82
      free memory: 81
      Killed
      
      ```

      

    - ` java -Xmx100m MemEat`: Java内存不够退出，语义正确，但是需要配置2次，麻烦。

      ```bash
      bash-4.3# java -Xmx100m MemEat
      free memory: 93
      free memory: 92
      free memory: 91
      ......
      free memory: 9
      free memory: 8
      free memory: 7
      free memory: 6
      free memory: 5
      free memory: 4
      Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
              at MemEat.main(MemEat.java:7)
      
      ```

      

  - JDK12：通过命令`docker run -m 100m -it openjdk:12-alpine /bin/sh`。和上面类似的测试方式：

    - `java MemEat`：Java内存不够退出，语义正确，也只需要配置一次。确实通过`UseContainerSupport`解决了老JDK版本的问题。

      ```bash
      /opt # java MemEat
      free memory: 6 mb
      free memory: 5 mb
      free memory: 4 mb
      ......
      free memory: 3 mb
      free memory: 2 mb
      free memory: 1 mb
      Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
              at MemEat.main(MemEat.java:7)
      
      ```

      

- CPU问题：继续使用JDK 7 和 JDK 12 分别测试CPU个数情况。

  ```java
  public class CpuTest {
  
      public static void main(String[] args) {
  
          Runtime runtime = Runtime.getRuntime();
  
          int processors = runtime.availableProcessors();
          long maxMemory = runtime.maxMemory();
  
          System.out.format("Number of processors: %d\n", processors);
          System.out.format("Max memory: %d bytes\n", maxMemory);
      }
  }
  ```

  

  - JDK7：通过命令`docker run --cpus=1 -it anapsix/alpine-java:7_jdk /bin/bash`。然后在命令行vi生成一个Java测试文件，`javac`后，通过下面的命令进行测试：

    ```bash
    bash-4.3# java CpuTest
    Number of processors: 6
    Max memory: 1858600960 bytes
    ```

    结果显然不对，我们应该只是指定了使用一个CPU。

  - JDK12：通过命令`docker run -m 100m --cpus=1 -it openjdk:12-alpine /bin/sh`,生成同上所述的测试文件，发现运行结果和预期一样：

    ```bash
    /opt # java CpuTest
    Number of processors: 1
    Max memory: 50724864 bytes
    ```

    

#### 3.3.4 Docker容器关闭对程序的影响

先通过maven命令`mvn clean package`，对编写好的Spring Boot项目达成jar包。准备在docker环境下使用：

- 直接前台运行：因为需要使用自定义的jar，所以我们需要实现build我们自己的docker image：

  - 编写`Dockerfile`:

    ```dock
    FROM openjdk:12-alpine
    COPY ./gracefully-shutdown-1.0-SNAPSHOT.jar java-and-docker.jar
    
    CMD ["java", "-jar", "java-and-docker.jar"]
    ```

  - 构建：

    ```bash
    docker build -t springboot:jdk11 .
    ```

  - 运行：

    ```bash
    docker run springboot:jdk11
    ```

  - `CTRL+C`：前台运行成功后，能看到控制台的正常输出。`CTRL+C`后表现的和本地环境一致，没有问题。

    

- 后台运行：现在测试后台运行Spring Boot程序，停止Docker的影响情况。

  - 运行：

    ```ba
     docker run -it -d --name demo_backend springboot:jdk11
     2758594a1e6538e15671f1357e99ac90a047d1e12de22bc7b825896318dcd8d9
    ```

  - 通过命令挂载到当前程序：

    ```bash
    docker exec -it 2758594a1e65 /bin/sh
    tail -f mylog.log
    ```

  - 通过命令关闭程序：

    ```bash
     docker stop -t 30 2758594a1e65
    ```

    默认Docker是先发送`SIGTERM`，默认等待`10s`后，在发送`SIGKILL`。我们的程序执行时间比较长，因此这里设置30秒，确保能完整退出。

  - 通过Docker的`docker kill`命令：

    ```bash
    docker kill 2758594a1e65
    # 如果想发送其他命令，可以如下操作
    # docker kill --signal=SIGHUP my_container
    ```

    通过`KILL`信号，整个容器立即退出，程序也随着退出了。

     

## 4. 尾声



### 4.1 总结

- 对于Spring程序来说尽可能使用Spring托管的Bean；
-  对于线程池创建出来的子线程或者Job服务，如果需要`for(;;)`用不考虑结束，那么请判断是否有其他线程发送`interrupt`，在接收到进行资源释放和关闭操作；
- 对于Spring的`Controller`程序，无论从用户还是系统保护的角度上来看，尽量不要执行时间太长，可以通过设置超时退出防御性机制，保证程序更加健壮；
- 对于跨事务的分布式操作，尽量不使用`JPA`，在可控和时间成本范围内选择消息或补充表；
- 对于外部接口操作，在可控和时间成本范围内选择消息或补充表；
- Spring系统的关闭一定要从进程消息级别开始，不能轻易认为只使用Spring容器销毁就万事大吉；
- Docker和Java结合，尽可能使用新版本的JDK，可以直接绕过需要多次配置才能勉强接近的水平；
- Docker的启停命令和本地环境基本一致，需要注意的是如果使用`docker stop`需要考虑默认的时间是否足够，否则程序会被迫终止退出。



### 4.2 更多

​	在微服务的架构里面，优雅停机不仅仅只是本服务停止而已，还需要考虑的因素有：

- 隔断流量——服务下线后，其上游服务不用因为流量没有下游承接消耗，累计超时导致`雪崩效应`；
- 注册中心移除——在服务开始关闭时，通知注册中心移除，避免现有消费者继续访问，新的消费者得到陈旧的节点信息；
- 订阅者接收通知移除——消费者通过订阅注册中心，将本地的缓存服务者重新更新，避免后续请求继续发送到无效节点；
- 处理失败请求——消费者如遇到发送的请求已经没有响应或者直接被拒绝，按照业务要求做响应处理；



### 4.3 参考链接	

<https://www.baeldung.com/spring-boot-shutdown>

<https://dzone.com/articles/graceful-shutdown-spring-boot-applications>

<https://www.cnkirito.moe/gracefully-shutdown/>

<https://access.redhat.com/documentation/en-us/red_hat_fuse/7.3/html/apache_karaf_transaction_guide/choosing-transaction-manager>

<https://www.baeldung.com/jee-jta>

<https://dev.to/zac_siegel/java-and-docker---memory-and-cpu-limits-3h77>

<https://blog.csdn.net/sinat_25596967/article/details/80231669>

<https://www.cnblogs.com/locean/p/4729500.html>

<https://kelvinji2009.github.io/blog/java和docker限制的那些事儿译/>

