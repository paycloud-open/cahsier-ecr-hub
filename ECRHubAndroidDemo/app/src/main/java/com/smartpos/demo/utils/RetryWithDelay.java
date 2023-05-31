package com.smartpos.demo.utils;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;


public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Flowable<?>> {

    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;
    private Scheduler scheduler;

    /**
     * @param maxRetries       重试次数，如果<0则无限重试
     * @param retryDelayMillis 时间间隔
     */
    public RetryWithDelay(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * @param maxRetries       重试次数，如果<0则无限重试
     * @param retryDelayMillis 时间间隔
     */
    public RetryWithDelay(int maxRetries, int retryDelayMillis, Scheduler scheduler) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.scheduler = scheduler;
    }

    @Override
    public Flowable<?> apply(Flowable<? extends Throwable> observable) {
        return observable.flatMap(new Function<Throwable, Flowable<?>>() {
            @Override
            public Flowable<?> apply(Throwable throwable) {
                Log.i("RetryWithDelay", "异常重试");
                if (maxRetries < 0 || ++retryCount < maxRetries) {
                    Log.i("RetryWithDelay", retryDelayMillis + "毫秒后" + (maxRetries < 0 ? "重试" : "第" + retryCount + "次重试"));
                    if (scheduler != null) {
                        return Flowable.timer(retryDelayMillis, TimeUnit.MILLISECONDS, scheduler);
                    } else {
                        return Flowable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                    }
                }
                return Flowable.error(throwable);
            }
        });
    }
}
