package com.smartpos.demo.sample;


import android.content.Context;
import android.util.Log;

import com.smartpos.demo.OnReceiveMsgListener;
import com.smartpos.demo.WiseConnectClient;
import com.smartpos.demo.utils.ScreenUtil;

/**
 * @author pupan
 */
public class ECRDataHandler {

    private final WiseConnectClient client;
    private final Context context;


    public ECRDataHandler(WiseConnectClient connectClient, Context context) {
        this.client = connectClient;
        this.context = context;
        client.registerReceiveMsgListener(new DataHandler());
    }

    public void registerReceiveMsgListener(OnReceiveMsgListener listener) {
        if (listener == null) {
            throw new IllegalStateException("please set a non-null listener");
        } else {
            client.registerReceiveMsgListener(listener);
        }
    }


    /**
     * 向PC发送步骤消息，需要在接收到请求后再调用
     * - 发送交易中的状态，比如：
     * 1. 正在创建订单
     * 2. 请挥卡、刷卡或插卡
     * 3.
     */
    public void sendStepMsg(String msg) {
        client.sendMsg(msg);
    }

    private class DataHandler implements OnReceiveMsgListener {

        @Override
        public void onReceiveMsg(String msg) {
            Log.i("DataHander", "onRecevieMsg：" + msg);
            org.json.JSONObject requestMsg = null;
            try {
                requestMsg = new org.json.JSONObject(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (requestMsg != null) {
                ScreenUtil.wakeUpScreen(context);
                //  MessageHandler.INSTANCE.handleMessageRequest(requestMsg);
            } else {
                Log.i("DataHander", "onRecevieMsg：Error");
            }
        }
    }
}
