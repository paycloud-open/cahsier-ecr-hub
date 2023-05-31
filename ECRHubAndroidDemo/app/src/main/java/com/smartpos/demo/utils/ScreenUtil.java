package com.smartpos.demo.utils;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.view.WindowManager;

/**
 * Created by Kevin on 2017/8/18.
 */

public class ScreenUtil {

    /**
     * 保持亮屏状态
     * - 不能主动点亮屏幕，必须在屏幕电量的状态下给Window增加该flag，可以保持屏幕常量，
     * 经过测试发现，当Activity关闭的时候，窗口又恢复到可以自动熄灭的状态。
     */
    public static void keepScreenOn(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 移除亮屏状态
     * - 当调用了上面的方法的时候，屏幕会一直点亮，然后可以通过该方法移除该Flag，让屏幕可以熄灭
     */
    public static void clearScreenOn(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 申请电源锁，点亮屏幕
     */
    public static void wakeUpScreen(Context context) {
        // 获取电源管理器对象
        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
        // 申请锁，点亮屏幕
        wl.acquire();
        // 释放锁
        wl.release();
    }


}