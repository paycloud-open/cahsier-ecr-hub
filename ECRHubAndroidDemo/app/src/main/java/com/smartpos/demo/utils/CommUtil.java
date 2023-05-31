package com.smartpos.demo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * @author pupan
 */
public class CommUtil {
    public static boolean isUsbConnected(Context context) {
        boolean isUsbConnected = false;
        // 主动发送包含是否正在充电状态的广播 , 该广播会持续发送
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // 注册广播接受者
        Intent intent = context.registerReceiver(null, intentFilter);
        // 获取充电状态
        int batteryChargeState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        // 判定是否是 AC 交流电充电
        boolean isAc = batteryChargeState == BatteryManager.BATTERY_PLUGGED_AC;
        // 判断是否是 USB 充电
        boolean isUsb = batteryChargeState == BatteryManager.BATTERY_PLUGGED_USB;
        // 判断是否是 无线充电
        boolean isWireless = batteryChargeState == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        // 如何上述任意一种为 true , 说明当前正在充电
        isUsbConnected = isAc || isUsb || isWireless;
        return isUsbConnected;
    }
}