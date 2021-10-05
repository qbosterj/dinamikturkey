package cn.com.mma.mobile.tracking.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.uodis.opendevice.aidl.OpenDeviceIdentifierService;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;


/**
 * MMAChinaSDK Example
 */
public class MainActivity extends AppCompatActivity {

    //广告位监测链接
    public static final String TRACKING_URL = "http://vxyz.admaster.com.cn/w/a86218,b1778712,c2343,i0,m202,8a2,8b2,2j,h";

//    public static final String TRACKING_URL = "http://vqq.admaster.com.cn/i/a123375,b3200889,c2209,i0,m202,8a2,8b1,2u2,2v50,2w30,2x1111,0i[M_IESID],[NEP],1f[MP],h[TENCENTSOID]";
    //sdkconfig.xml配置文件服务器存放地址
    public static final String CONFIG_URL = "";

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView adView;
    private TextView urlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adView = (TextView) findViewById(R.id.adview);
        urlView = (TextView) findViewById(R.id.ad_url);
        urlView.setText(TRACKING_URL);

        // MMASDK 初始化
        Countly.sharedInstance().setLogState(true);
        Countly.sharedInstance().init(this, CONFIG_URL);
    }

    /**
     * 点击监测
     *
     * @param view
     */
    public void doClick(View view) {
        Countly.sharedInstance().onClick(TRACKING_URL);
        Log.d(TAG, "[click]：" + TRACKING_URL);
    }

    /**
     * 曝光监测/Tracked Ads
     *
     * @param view
     */
    public void doExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL,null,1);
        Log.d(TAG, "[expose]：" + TRACKING_URL);
    }

    /**
     * 可视化曝光监测
     *
     * @param view
     */
    public void doViewAbilityExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL, adView);
//        Countly.sharedInstance().onVideoExpose(TRACKING_URL,adView,1);
        Log.d(TAG, "[ViewAbilityExpose]：" + TRACKING_URL);
//        new Handler().postDelayed(new Runnable(){
//            public void run() {
//                //execute the task
//                Log.d(TAG, "[StopViewAbilityExpose]：" + TRACKING_URL);
//                Countly.sharedInstance().stop(TRACKING_URL);
//            }
//        }, 5000);
    }

    /**
     * 可视化曝光JS监测
     *
     * @param view
     */
    public void doViewAbilityJSExpose(View view) {
        Countly.sharedInstance().onJSExpose(TRACKING_URL, adView);
        Log.d(TAG, "[ViewAbilityJSExpose]：" + TRACKING_URL);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Countly.sharedInstance().terminateSDK();
    }
}
