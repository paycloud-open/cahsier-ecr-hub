package com.smartpos.demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;

import com.smartpos.demo.ConnectInfo;
import com.smartpos.demo.ConnectOptions;
import com.smartpos.demo.IChannel;
import com.smartpos.demo.WiseConnectClient;
import com.smartpos.demo.WiseException;
import com.smartpos.demo.serial.Usb2;
import com.smartpos.demo.utils.ChannelCallback;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author hongge
 * @Note 维持连接的服务
 */
public class WisConnectService extends Service {

    /**
     * 消息类型：连接
     */
    public static final int MSG_CONNECT = 1;
    /**
     * 消息类型：断开连接
     */
    public static final int MSG_DISCONNECT = 2;
    /**
     * 消息类型：发送信息
     */
    public static final int MSG_SENDMSG = 3;
    /**
     * 消息类型：连接状态
     */
    public static final int MSG_CONNECTSTATE = 4;
    /**
     * 消息类型：接收信息
     */
    public static final int MSG_RECEIVEMESSAGE = 5;


    /**
     * 接收消息Handler
     */
    private ProcessHandler mHandler;
    /**
     * 接收消息Messenger
     */
    private Messenger mReceiver;
    /**
     * 发送消息Messenger
     */
    private Messenger mSender;

    private IChannel channel;

    private ConnectInfo connectInfo;

    private ConnectOptions connectOptions;

    private Timer reconnectTimer;

    private boolean stopByManual = false;


    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new ProcessHandler();
        mReceiver = new Messenger(mHandler);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mReceiver.getBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("WisConnectService", "onDestroy");
    }

    /**
     * 连接
     */
    private void connect() {
        sendConnectStateToClient(WiseConnectClient.CONNECTING);
        if (connectInfo.getScheme().equals(ConnectInfo.Scheme.USB)) {
            channel.connect();
        }
    }


    private void initChannel() {
        if (null == channel) {
            channel = new Usb2(WisConnectService.this.getApplicationContext());
            channel.setChannelCallback(new ChannelCallback() {
                @Override
                public void readMsg(String msg) {
                    Log.i("WisConnectService", "onRecevieMsg：" + msg);
                    sendReceiveMsgToClient(msg);
                }

                @Override
                public void onConnectStart() {

                }

                @Override
                public void onConnectSuccess(boolean isReConnect) {
                    Log.i("WisConnectService", "onConnect");
                    sendConnectStateToClient(WiseConnectClient.CONNECTED);
                }

                @Override
                public void onConnectFail(String message) {

                }

                @Override
                public void onDisconnect() {
                    Log.e("WisConnectService", "channel onDisconnect");
                    if (!stopByManual) {
                        if (checkForReconnection()) {
                            startReconnect(new WiseException(-1));
                        } else {
                            sendConnectStateToClient(WiseConnectClient.DISCONNECTED);
                        }
                    } else {
                        sendConnectStateToClient(WiseConnectClient.UNCONNECTED);
                    }
                }
            });
        }
    }


    /**
     * 主动断开连接并关闭服务
     */
    private void disConnect() {
        Log.e("WisConnectService", "disConnect");
        stopByManual = true;
        stopReconnect();
        channel.disConnect();
    }

    /**
     * 发送消息到channel
     */
    private void sendMsgToChannel(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (null != connectInfo && connectInfo.getScheme().equals(ConnectInfo.Scheme.USB)) {
            channel.sendMsg(msg);
        }
    }

    /**
     * 判读是否重连
     */
    private boolean checkForReconnection() {
        if (stopByManual) {
            return false;
        }
        return connectOptions.isAutomaticRetry();
    }

    /**
     * 发送连接状态到client
     */
    private void sendConnectStateToClient(int state) {
        connectInfo.setState(state);
        sendToClient(MSG_CONNECTSTATE, connectInfo.toString());
    }

    /**
     * 发送接收消息到client
     */
    private void sendReceiveMsgToClient(String msg) {
        sendToClient(MSG_RECEIVEMESSAGE, msg);
    }

    /**
     * 发送到client
     *
     * @param what 类型
     * @param obj  内容
     */
    private void sendToClient(int what, Object obj) {
        if (mSender == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = what;
        message.obj = obj;
        try {
            mSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT:
                    mSender = msg.replyTo;
                    connectOptions = (ConnectOptions) msg.obj;
                    connectInfo = (ConnectInfo) msg.getData().getSerializable("connectInfo");
                    initChannel();
                    connect();
                    break;
                case MSG_DISCONNECT:
                    disConnect();
                    break;
                case MSG_SENDMSG:
                    sendMsgToChannel(msg.obj.toString());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void startReconnect(WiseException e) {

        ConnectOptions.IRetryPolicy retryPolicy = connectOptions.getRetryPolicy();

        long reconnectDelay = 0;

        if (retryPolicy != null) {
            long waitingTime = retryPolicy.waitingTime(e);
            if (ConnectOptions.RETRY_STOP == waitingTime) {
                return;
            }

            reconnectDelay = waitingTime;
        }

        Log.e("WisConnectService", "start reconnect timer");
        if (reconnectTimer == null) {
            reconnectTimer = new Timer();
        }
        reconnectTimer.schedule(new ReconnectTask(), reconnectDelay);
    }

    private void stopReconnect() {
        if (reconnectTimer != null) {
            Log.e("WisConnectService", "stop reconnect timer");
            reconnectTimer.cancel();
        }
    }

    private class ReconnectTask extends TimerTask {

        @Override
        public void run() {
            connect();
        }
    }


}
