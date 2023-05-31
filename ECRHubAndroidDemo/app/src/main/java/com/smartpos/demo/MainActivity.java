package com.smartpos.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartpos.demo.sample.ECRHolder;

import org.json.JSONObject;


public class MainActivity extends Activity implements OnConnectStateChangeListener, OnReceiveMsgListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        this.findViewById(R.id.init).setOnClickListener(view -> {
            ECRHolder.getInstance().init(MainActivity.this.getApplicationContext());
            ECRHolder.getInstance().getDataHandler().registerReceiveMsgListener(MainActivity.this);
            ECRHolder.getInstance().getWiseConnectClient().addConnectStateChangeListener(MainActivity.this);
        });
        this.findViewById(R.id.conncect).setOnClickListener(view -> ECRHolder.getInstance().getWiseConnectClient().connect(
                new ConnectInfo(
                        ConnectInfo.Scheme.USB,
                        ""
                )
        ));
        this.findViewById(R.id.sendMessag).setOnClickListener(view -> ECRHolder.getInstance().getDataHandler().sendStepMsg("message"));
    }

    @Override
    public void onStateChange(JSONObject state) {
        Toast.makeText(this, "状态改变:" + state.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceiveMsg(String msg) {
        Toast.makeText(this, "收到信息:" + msg, Toast.LENGTH_SHORT).show();
    }
}