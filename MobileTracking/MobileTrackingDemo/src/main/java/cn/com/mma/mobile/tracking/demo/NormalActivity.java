package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;


/**
 * MMAChinaSDK Example
 */
public class NormalActivity extends BaseActivity {


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
        Countly.sharedInstance().onExpose(EXPOSE_URL, null, 0, new CallBack() {
            @Override
            public void onSuccess(String exposeUrl) {
                Logger.i("普通曝光回调：" + exposeUrl);
            }

            @Override
            public void onFailed(String errorMessage) {

            }
        });



    }


    public void toLandingPage(View view) {
        toLandingPage(this, CLICK_URL);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
