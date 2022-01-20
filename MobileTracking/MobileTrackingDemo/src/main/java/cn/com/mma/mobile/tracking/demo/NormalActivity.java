package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;


/**
 * MMAChinaSDK Example
 */
public class NormalActivity extends BaseActivity {

    private TextView adView;

    private String urlTest = "https://e.cn.miaozhen.com/r?k=2220972&p=7p4o577777&dx=__IPDX__&rt=2&pro=s&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=111111111111&m4=__AAID__&m5=111111111111&m6=__MAC1__&m6a=__MAC__&m11=__OAID__&mn=__ANAME__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&nf=__FLL__&ne=__SLL__&nvn=__VNAME__&nl=__SCENEID__&pgsp=D212000281[2]-2&nu=__inoceanenginevid__&o=";

    @Override
    protected int getContentView() {
        return R.layout.activity_normal;
    }

    @Override
    protected String setActionBar() {
        return "普通曝光及点击示例";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adView = (TextView) findViewById(R.id.adview);

       
        Countly.sharedInstance().onExpose(urlTest, null, 0, new CallBack() {
            @Override
            public void onSuccess(String eventType) {

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });


        Countly.sharedInstance().disPlayImp(urlTest, null, 0, new CallBack() {
            @Override
            public void onSuccess(String eventType) {

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });
//        Countly.sharedInstance().onExpose(EXPOSE_URL, null, 0, new CallBack() {
//            @Override
//            public void onSuccess(String exposeUrl) {
//                Logger.i("普通曝光回调：" + exposeUrl);
//            }
//
//            @Override
//            public void onFailed(String errorMessage) {
//
//            }
//        });



    }


    public void toLandingPage(View view) {
        toLandingPage(this, CLICK_URL);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
