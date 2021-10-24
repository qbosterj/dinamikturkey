package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import cn.com.mma.mobile.tracking.api.Countly;


/**
 * MMAChinaSDK Example
 */
public class DisplayActivity extends BaseActivity {

    private TextView adView;
    @Override
    protected int getContentView() {
        return R.layout.activity_display;
    }

    @Override
    protected String setActionBar() {
        return "Display可见曝光示例";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adView = (TextView) findViewById(R.id.adview);

        //动画
//        Animation anim = new AnimationUtils().loadAnimation(this, R.anim.anim_scale);
//        anim.setFillAfter(true);//动画执行完毕后停留在最后一帧
//        adView.startAnimation(anim);

        Countly.sharedInstance().onExpose(DISPLAY_EXPOSE_URL, adView );

//        new Handler().postDelayed(new Runnable(){
//            public void run() {
//                //execute the task
//                Log.d(TAG, "[StopViewAbilityExpose]：" + MZ_DISPLAY_EXPOSE_URL);
//                Countly.sharedInstance().stop(MZ_DISPLAY_EXPOSE_URL);
//            }
//        }, 5000);

    }


    public void toLandingPage(View view) {

        toLandingPage(this, DISPLAY_CLICK_URL);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
