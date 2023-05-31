package com.smartpos.demo;

import org.json.JSONObject;

/**
 * @author hongge
 * @Note 连接状态改变监听器
 */
public interface OnConnectStateChangeListener {

    /**
     * @param state 当前状态
     */
    void onStateChange(JSONObject state);
}
