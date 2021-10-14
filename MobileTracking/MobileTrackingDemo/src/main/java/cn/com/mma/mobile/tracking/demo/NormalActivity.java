package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.view.View;

import cn.com.mma.mobile.tracking.api.Countly;


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
        Countly.sharedInstance().onExpose(EXPOSE_URL, null, 0);
    }


    public void toLandingPage(View view) {
        toLandingPage(this, CLICK_URL);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
