package com.netease.hystrix.dubbo.rpc.filter;

import org.apache.log4j.Logger;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

public class DubboHystrixCommand extends HystrixCommand<Result> {

    private static Logger    logger                       = Logger.getLogger(DubboHystrixCommand.class);
    private static final int DEFAULT_THREADPOOL_CORE_SIZE = 30;
    private Invoker<?>       invoker;
    private Invocation       invocation;
    
    public DubboHystrixCommand(Invoker<?> invoker,Invocation invocation){
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(String.format("%s_%d", invocation.getMethodName(), invocation.getArguments() == null ? 0 : invocation.getArguments().length)))
              .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                            .withCircuitBreakerRequestVolumeThreshold(20)//10秒钟内至少19此请求失败，熔断器才发挥起作用
                                            .withCircuitBreakerSleepWindowInMilliseconds(30000)//熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
                                            .withCircuitBreakerErrorThresholdPercentage(50)//错误率达到50开启熔断保护
                                            .withExecutionTimeoutEnabled(false))//使用dubbo的超时，禁用这里的超时
              .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(getThreadPoolCoreSize(invoker.getUrl()))));//线程池为30
       
        
        this.invoker=invoker;
        this.invocation=invocation;
    }
    
    /**
     * 获取线程池大小
     * 
     * @param url
     * @return
     */
    private static int getThreadPoolCoreSize(URL url) {
        if (url != null) {
            int size = url.getParameter("ThreadPoolCoreSize", DEFAULT_THREADPOOL_CORE_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug("ThreadPoolCoreSize:" + size);
            }
            return size;
        }

        return DEFAULT_THREADPOOL_CORE_SIZE;

    }

    @Override
    protected Result run() throws Exception {
    	System.out.println(">>>>>>>>>>>>>>>>>>>>invoke method...");
        return invoker.invoke(invocation);
    }
}
