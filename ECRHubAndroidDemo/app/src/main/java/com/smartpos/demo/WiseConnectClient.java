package com.smartpos.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;


import com.smartpos.demo.service.WisConnectService;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * @author hongge
 * @Note 连接类
 */
public class WiseConnectClient {

    private static final String TAG = "WiseConnectClient";
    /**
     * 连接状态：未连接
     */
    public static final int UNCONNECTED = 0;
    /**
     * 连接状态：连接中
     */
    public static final int CONNECTING = 1;
    /**
     * 连接状态：连接成功
     */
    public static final int CONNECTED = 2;
    /**
     * 连接状态：连接断开
     */
    public static final int DISCONNECTED = 3;

    /**
     * 当前连接状态
     */
    private int connectState = UNCONNECTED;

    /**
     * 状态监听器列表
     */
    private final ArrayList<OnConnectStateChangeListener> connectStateChangeListeners = new ArrayList<>();
    /**
     * 消息监听器
     */
    private OnReceiveMsgListener receiveMsgListener;

    private final Context mContext;

    private final WiseConnectServiceConnection mConnection;
    /**
     * 接收消息Messenger
     */
    private final Messenger mReceiver;
    /**
     * 发送消息Messenger
     */
    private Messenger mSender;
    /**
     * 服务是否开启
     */
    private boolean mIsServiceStarted = false;
    /**
     * 连接信息
     */
    private ConnectInfo connectInfo;
    /**
     * 连接配置
     */
    private ConnectOptions connectOptions;

    public WiseConnectClient(Context context) {
        mContext = context;
        /**
         * 接收消息Handler
         */
        ProcessHandler mHandler = new ProcessHandler();
        mReceiver = new Messenger(mHandler);
        mConnection = new WiseConnectServiceConnection();
        connectOptions = new ConnectOptions.Builder().build();
    }

    public void setConnectOption(ConnectOptions connectOptions) {
        this.connectOptions = connectOptions;
    }

    /**
     * 是否已经开始连接
     */
    private boolean isConnectStarted() {
        return connectState == CONNECTING || connectState == CONNECTED;
    }

    public ConnectInfo getConnectInfo() {
        return connectInfo;
    }

    /**
     * 开始连接
     */
    public void connect(ConnectInfo connectInfo) {
        if (connectInfo == null || TextUtils.isEmpty(connectInfo.getScheme())) {
            throw new IllegalStateException("please set a non-null connectInfo");
        } else {
            this.connectInfo = connectInfo;
        }
        if (!mIsServiceStarted) {
            mIsServiceStarted = true;
            Intent intent = new Intent(mContext, WisConnectService.class);
            try {
                mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                e.printStackTrace();
                mIsServiceStarted = false;
            }
        } else {
            onConnect();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mIsServiceStarted && mSender != null) {
            if (isConnectStarted()) {
                Message message = Message.obtain();
                message.what = WisConnectService.MSG_DISCONNECT;
                try {
                    mSender.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                mContext.unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onDisconnected();
            //由于无法收到断开连接后的状态改变，在此手动修改
            if (null == connectInfo) {
                connectInfo = new ConnectInfo();
            }
            connectInfo.setState(UNCONNECTED);
            setConnectState(connectInfo.toString());
            System.gc();
        }
    }

    /**
     * 设置连接状态，当状态发生改变时会通知所有监听器
     */
    private void setConnectState(String info) {
        try {
            JSONObject json = new JSONObject(info);
            if (json.has("state")) {
                connectState = json.getInt("state");
                connectInfo.setState(json.getInt("state"));
            }
            if (json.has("scheme")) {
                connectInfo.setScheme(json.getString("scheme"));
            }
            if (json.has("address")) {
                connectInfo.setAddress(json.getString("address"));
            }
            for (OnConnectStateChangeListener listener : connectStateChangeListeners) {
                listener.onStateChange(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加连接状态改变监听
     */
    public void addConnectStateChangeListener(OnConnectStateChangeListener listener) {
        if (listener == null) {
            throw new IllegalStateException("please set a non-null listener");
        } else if (connectStateChangeListeners.contains(listener)) {
            throw new IllegalStateException("the listener already exists");
        } else {
            connectStateChangeListeners.add(listener);
        }
    }


    public void onReceiveMsg(String msg) {
        if (receiveMsgListener != null) {
            try {
                JSONObject message = new JSONObject(msg);
                if (message.has("trans_amount")) {
                    message.put("trans_amount", changeY2F(message.getString("trans_amount")));
                }
                message.put("call_app_mode", "2");
                receiveMsgListener.onReceiveMsg(message.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("WiseConnectClient", "recevieMsgListener is null");
        }
    }

    public static String changeY2F(String amount) {
        return new BigDecimal(amount).multiply(new BigDecimal(100)).toString().replace(".00", "");
    }

    /**
     * 注册消息监听器
     */
    public void registerReceiveMsgListener(OnReceiveMsgListener listener) {
        if (listener == null) {
            throw new IllegalStateException("please set a non-null listener");
        } else {
            this.receiveMsgListener = listener;
        }
    }

    /**
     * 取消消息监听
     *
     * @param listener
     */
    public void unRegisterReceiveMsgListener(OnReceiveMsgListener listener) {
        if (listener == null) {
            throw new IllegalStateException("please set a non-null listener");
        } else {
            this.receiveMsgListener = null;
        }
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (connectState != CONNECTED || mSender == null) {
            Log.i(TAG, "Channel has not started!");
            return;
        }
        Message message = Message.obtain();
        message.what = WisConnectService.MSG_SENDMSG;
        message.obj = msg;
        try {
            mSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接
     */
    private void onConnect() {
        Message message = Message.obtain();
        message.what = WisConnectService.MSG_CONNECT;
        message.replyTo = mReceiver;
        message.obj = connectOptions;
        Bundle bundle = new Bundle();
        bundle.putSerializable("connectInfo", connectInfo);
        message.setData(bundle);
        try {
            mSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class WiseConnectServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSender = new Messenger(service);
            onConnect();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("WiseConnectClient", "onServiceDisconnected");
            onDisconnected();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e("WiseConnectClient", "onBindingDied");
            onDisconnected();
        }
    }


    private void onDisconnected() {
        mSender = null;
        mIsServiceStarted = false;
    }


    private class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //频繁连接、断开会有可能在断开连接后收到消息
            if (!mIsServiceStarted) {
                return;
            }
            switch (msg.what) {
                case WisConnectService.MSG_CONNECTSTATE:
                    setConnectState(msg.obj.toString());
                    break;
                case WisConnectService.MSG_RECEIVEMESSAGE:
                    onReceiveMsg(msg.obj.toString());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}