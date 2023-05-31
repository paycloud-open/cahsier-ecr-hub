package com.smartpos.demo;


/**
 * @author hongge
 * @Date ${date} ${time}
 * @Note
 */
public final class ConnectOptions {

    public static long RETRY_STOP = -1;

    private final boolean simpleMode;//简单模式：无心跳、无握手流程

    private final int keepAliveInterval;//心跳间隔,单位秒

    private final boolean automaticRetry;//自动重连

    private final boolean debugMode;//debug模式

    private final IRetryPolicy retryPolicy;//重连策略


    private ConnectOptions(Builder builder) {
        this.simpleMode = builder.simpleMode;
        this.keepAliveInterval = builder.keepAliveInterval;
        this.automaticRetry = builder.automaticRetry;
        this.debugMode = builder.debugMode;
        this.retryPolicy = builder.retryPolicy;
    }


    public boolean isSimpleMode() {
        return simpleMode;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public boolean isAutomaticRetry() {
        return automaticRetry;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public IRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public interface IRetryPolicy {

        /**
         * -1 不再重试
         * 0 立即重试
         * >0 等待一定时间后重试，单位毫秒
         *
         * @param e disconnect断开连接无异常
         * @return 重试等待时间，单位毫秒
         */
        long waitingTime(WiseException e);
    }

    public static class Builder {

        private static final int KEEP_ALIVE_INTERVAL_DEFAULT = 2;

        private boolean simpleMode = true;//简单模式：无心跳、无握手流程

        private int keepAliveInterval = KEEP_ALIVE_INTERVAL_DEFAULT;//心跳间隔,单位秒

        private boolean automaticRetry = true;//自动重连

        private boolean debugMode = false;//debug模式

        private IRetryPolicy retryPolicy;//重连策略

        public ConnectOptions build() {
            return new ConnectOptions(this);
        }

        public Builder simpleMode(boolean simpleMode) {
            this.simpleMode = simpleMode;
            return this;
        }

        public Builder keepAliveInterval(int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder automaticRetry(boolean automaticRetry) {
            this.automaticRetry = automaticRetry;
            return this;
        }

        public Builder debugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        public Builder retryPolicy(IRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
    }


}
