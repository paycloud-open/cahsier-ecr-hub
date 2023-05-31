package com.smartpos.demo.sample;

import android.content.Context;

import com.smartpos.demo.ConnectOptions;
import com.smartpos.demo.WiseConnectClient;

/**
 * 由于WiseConnectClient、MyDataHandler的生命周期>Activity，所以放到单例下，也可以放到service中
 *
 * @author hongge
 * @Date ${date} ${time}
 * @Note
 */
public class ECRHolder {

    private ECRHolder() {
    }


    public static ECRHolder getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        static ECRHolder instance = new ECRHolder();
    }


    private WiseConnectClient wiseConnectClient;

    private ECRDataHandler dataHandler;

    private Context context;


    public void init(Context context) {
        if (context == null) {
            throw new IllegalStateException("please set a non-null context");
        }
        this.context = context.getApplicationContext();
        ConnectOptions connectOptions = new ConnectOptions.Builder()
                .debugMode(true)
                .automaticRetry(true)
                .retryPolicy(new ECRRetryPolicy())
                .build();
        wiseConnectClient = new WiseConnectClient(this.context);
        wiseConnectClient.setConnectOption(connectOptions);
        dataHandler = new ECRDataHandler(wiseConnectClient, this.context);
    }

    public WiseConnectClient getWiseConnectClient() {
        checkInit();
        return wiseConnectClient;
    }

    public ECRDataHandler getDataHandler() {
        checkInit();
        return dataHandler;
    }

    private void checkInit() {
        if (context == null) {
            throw new IllegalStateException("please initialize first");
        }
    }


}
