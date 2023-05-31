package com.smartpos.demo;


/**
 * @author hongge
 * @Note 消息监听器
 */
public interface OnReceiveMsgListener {

    /**
     * 运行在子线程中
     *
     * @param msg
     */
    void onReceiveMsg(String msg);
}
