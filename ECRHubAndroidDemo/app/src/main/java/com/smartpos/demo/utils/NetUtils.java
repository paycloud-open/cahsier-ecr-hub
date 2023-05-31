package com.smartpos.demo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络相关的工具类
 */

public class NetUtils {

    private static final String tag = "【NetUtils】";

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     *
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkConnect(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                android.util.Log.e(tag, "The network is connect, netType is "
                        + info.getType() + ", netTypeName is " + info.getTypeName());
                return true;
            }
        }
        return false;
    }

    /**
     * 判断WIFI是否可用
     * - 根据当前有效的网络是否是WIFI，因为如果手机和WIFI同时具备的时候，网络会使用WIFI
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            android.util.Log.e(tag, "The network is connect, netType is "
                    + networkInfo.getType() + ", netTypeName is " + networkInfo.getTypeName());
            return (networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
        } else {
            android.util.Log.e(tag, "The network is null or disConnect!");
            return false;
        }
    }

    /**
     * 判断MOBILE是否可用
     * - 如果当前连接WIFI，那么使用的就是WIFI，我们自己默认MOBILE不可用
     */
    public static boolean isMobileConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
        return false;
    }

    /**
     * 获取网络信息
     */
    public static NetworkInfo getCurrentNetworkInfo(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo();
    }
}
