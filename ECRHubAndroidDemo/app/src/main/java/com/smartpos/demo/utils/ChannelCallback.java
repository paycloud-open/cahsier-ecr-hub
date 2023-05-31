package com.smartpos.demo.utils;

/**
 * @author Kevin
 * @date 2017/8/17
 */

public interface ChannelCallback {
    void readMsg(String msg);

    void onConnectStart();

    void onConnectSuccess(boolean isReConnect);

    void onConnectFail(String message);

    void onDisconnect();
}
