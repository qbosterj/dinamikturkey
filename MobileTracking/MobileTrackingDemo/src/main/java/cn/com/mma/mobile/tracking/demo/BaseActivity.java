package cn.com.mma.mobile.tracking.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;


/**
 *
 * MMAChinaSDK Example
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected static String EXPOSE_URL = "";
    protected static String NEW_EXPOSE_URL = "";
    protected static String CLICK_URL = "";

    protected static String DISPLAY_IMP_URL = "";
    protected static String DISPLAY_CLICK_URL = "";

    protected static String VIDEO_EXPOSE_URL = "";
    protected static String VIDEO_CLICK_URL = "";


    /**
     * 内部日志输出开关，0为线上环境，1为测试环境
     */
    private static int LOG_SWITCH = 1;

    static {
        switch (LOG_SWITCH) {
            case 0:
                VIDEO_EXPOSE_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";

                break;
            case 1:
                EXPOSE_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
                NEW_EXPOSE_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";

               CLICK_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
                DISPLAY_IMP_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
               DISPLAY_CLICK_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
                VIDEO_EXPOSE_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
//                MZ_VIDEO_CLICK_URL = "http://r.cn.miaozhen.com/r/k=20416532&p=DuJJC&dx=__IPDX__&rt=2&ni=__IESID__&o=";
                VIDEO_CLICK_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
                break;
        }
    }


    protected abstract int getContentView();

    //protected abstract String onExposed(String exposeURL);

    protected abstract String setActionBar();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        String title = setActionBar();
        if (title != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(title);
            }
        }
        sdkInit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sdkInit() {
        Countly.sharedInstance().setLogState(true);
        Countly.sharedInstance().init(this, "");
    }


    protected void toLandingPage(Context context, String destURL) {
        Countly.sharedInstance().onClick(destURL, new CallBack() {
            @Override
            public void onSuccess(String exposeUrl) {

                Logger.i("点击回调："  + exposeUrl);

            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });
        Intent intent = new Intent(context, LandingPageActivity.class);
        startActivity(intent);
        Toast.makeText(this, "to LandingPage", Toast.LENGTH_SHORT).show();
    }

    protected void doDestory() {
        Countly.sharedInstance().terminateSDK();
    }
}
