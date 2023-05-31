package com.smartpos.demo;

/**
 * @author hongge
 * @Date ${date} ${time}
 * @Note
 */
public class WiseException extends Exception {

    public static final int REASON_CODE_CLIENT_EXCEPTION = 10000;//客户端异常
    public static final int REASON_CODE_CHECKCODE_ERROR = 10001;//校验和异常

    public static final int REASON_CODE_NET_ERROR = 20000;//网络异常
    public static final int REASON_CODE_SERIAL_LINE_NOT_INSERTED_ERROR = 20001;//串口线未插入
    public static final int REASON_CODE_BLUETOOTH_NOT_SUPPORT_ERROR = 20002;//蓝牙设备异常
    public static final int REASON_CODE_MAC_ERROR = 20003;//mac地址异常

    private final int reasonCode;
    private Throwable cause;


    public WiseException(int reasonCode) {
        super();
        this.reasonCode = reasonCode;
    }

    public WiseException(Throwable cause) {
        super();
        this.reasonCode = REASON_CODE_CLIENT_EXCEPTION;
        this.cause = cause;
    }

    public WiseException(int reason, Throwable cause) {
        super();
        this.reasonCode = reason;
        this.cause = cause;
    }

    public int getReasonCode() {
        return reasonCode;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

}
