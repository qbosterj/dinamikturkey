package cn.com.mma.mobile.tracking.demo;

import android.content.Intent;
import android.view.View;


/**
 * MMAChinaSDK Example
 */
public class MenuActivity extends BaseActivity {


    @Override
    protected int getContentView() {
        return R.layout.activity_menu;
    }

    @Override
    protected String setActionBar() {
        return null;
    }


    public void toNormal(View view) {
        Intent intent = new Intent(this, NormalActivity.class);
        startActivity(intent);
    }

    public void toDisplay(View view) {
        Intent intent = new Intent(this, DisplayActivity.class);
        startActivity(intent);

    }

    public void toVideo(View view) {
        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }
    //进入页面自动播放，播放类型为1
    public void toAutoVideo(View view) {
        Intent intent = new Intent(this, ClickVideoActivity.class);
        startActivity(intent);
    }
    //进入页面后，点击播放开始播放视频，播放类型为2
    public void ClicktoVideo(View view) {
        Intent intent = new Intent(this, AutoVideoActivity.class);
        startActivity(intent);
    }
    //进入页面自动播放，调用普通曝光
//    public void RegularAutoPlay(View view) {
//        Intent intent = new Intent(this, RegularVideoActivity.class);
//        startActivity(intent);
//    }
    //进入页面手动播放，调用普通曝光
//    public void RegularClickPlay(View view) {
//        Intent intent = new Intent(this, RgCVideoActivity.class);
//        startActivity(intent);
//    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        doDestory();
    }
}
