package com.smartpos.demo;

import android.os.IBinder;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * @author hongge
 * @Date ${date} ${time}
 * @Note
 */
public class ConnectInfo implements Serializable {

    @Retention(RetentionPolicy.SOURCE)
    public @interface Scheme {
        String USB = "USB";
        String BT = "BT";
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        json.put("scheme", scheme);
        json.put("address", address);
        json.put("state", state);
        return json.toString();
    }

    private String scheme;
    private String address;
    private int state = WiseConnectClient.DISCONNECTED;

    public ConnectInfo() {
        try {
            Class<?> name = Class.forName("android.os.ServiceManager");
            Method method = name.getDeclaredMethod("getService", String.class);
            IBinder b = (IBinder) method.invoke(name, "WangPosSystemServiceInterface");
        } catch (Exception e) {

        }
    }

    public ConnectInfo(@Scheme String scheme, String address) {
        this.scheme = scheme;
        this.address = address;
    }

    public String getScheme() {
        return scheme;
    }


    public String getAddress() {
        return address;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
