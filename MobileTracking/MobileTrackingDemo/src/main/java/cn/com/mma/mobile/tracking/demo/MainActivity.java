package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;


/**
 * MMAChinaSDK Example
 */
public class MainActivity extends AppCompatActivity {

    //广告位监测链接
//    public static final String TRACKING_URL = "http://vxyz.admaster.com.cn/w/a86218,b1778712,c2343,i0,m202,8a2,8b2,2j,h";

//    public static final String TRACKING_URL = "http://vqq.admaster.com.cn/i/a123375,b3200889,c2209,i0,m202,8a2,8b1,2u2,2v50,2w30,2x1111,0i[M_IESID],[NEP],1f[MP],h[TENCENTSOID]";

    public static final String TRACKING_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
    public static final String CONFIG_URL = "";

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView adView;
    private TextView urlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newactivity_main);
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
        Countly.sharedInstance().onClick(TRACKING_URL, new CallBack() {
            @Override
            public void onSuccess(String exposeUrl) {

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });
        Log.d(TAG, "[click]：" + TRACKING_URL);
    }

    /**
     * 曝光监测/Tracked Ads
     *
     * @param view
     */
    public void doExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL, null, 1, new CallBack() {
            @Override
            public void onSuccess(String exposeUrl) {

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });
        Log.d(TAG, "[expose]：" + TRACKING_URL);
    }

    /**
     * 可视化曝光监测
     *
     * @param view
     */
    public void doViewAbilityExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL, adView, new CallBack() {
            @Override
            public void onSuccess(String exposeUrl) {

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });
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
