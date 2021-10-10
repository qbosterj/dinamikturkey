package cn.com.mma.mobile.tracking.demo;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import cn.com.mma.mobile.tracking.api.Countly;


/**
 * MMAChinaSDK Example
 * 单独界面播放视频
 *
 */
public class AutoVideoActivity extends BaseActivity {
    private VideoView videoView;
    private TextView statusView;
    private int currentPos = -1;
    private boolean patchFinished = false;
    private GestureDetector mGesture = null;
    private Button videoButton;
    private int  current_position = 0;
    private Bitmap mCachedBitmap = null;
    private final int mClientDensity = DisplayMetrics.DENSITY_DEFAULT;



    @Override
    protected int getContentView() {
        return R.layout.activity_auto_video;
    }

    @Override
    protected String setActionBar() {
        return "Video可见曝光示例";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏播放
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        videoView = (VideoView) findViewById(R.id.video_video);
        statusView = (TextView) findViewById(R.id.statusView);
        videoButton = (Button) findViewById(R.id.video_button);

        final String uri = "android.resource://" + getPackageName() + "/" + R.raw.patch;



        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(!videoView.isPlaying()){
                        videoButton.setText("暂停");
                        videoView.setVideoURI(Uri.parse(uri));
//                        System.out.println("播放进度：" + current_position);
                        videoView.seekTo(current_position);
                        videoView.start();
//                        videoView.resume();
                        videoView.requestFocus();
                        statusView.setText("status：前贴片广告播放中...");
                        //点击监听事件，跳转到LandingPage
                        videoView.setOnTouchListener(videoClickListener);
                        //进度监听事件
                        videoView.setOnCompletionListener(videoProgressListener);
                        //进入页面后
                        Countly.sharedInstance().onVideoExpose(VIDEO_EXPOSE_URL, videoView, 2);

                    }else {
                        videoButton.setText("播放");
                        current_position = videoView.getCurrentPosition();
//                        System.out.println("暂停时播放进度：" + current_position);
                        videoView.pause();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });



    }



    MediaPlayer.OnCompletionListener videoProgressListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            videoView.seekTo(0);
            currentPos = -1;
            //第一个视频播放完成后，播放第二个视频
            if (!patchFinished) {
                videoView.setOnTouchListener(null);
                //前贴片广告播放完成后，如果还未达成可见，主动关闭（停止监测，上报不可见）
                Countly.sharedInstance().stop(VIDEO_EXPOSE_URL);

                String uri1 = "android.resource://" + getPackageName() + "/" + R.raw.content;
                videoView.setVideoURI(Uri.parse(uri1));
                videoView.start();
                statusView.setText("status：视频播放中...");
                patchFinished = true;
            }
        }
    };

    View.OnTouchListener videoClickListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mGesture == null) {
                mGesture = new GestureDetector(AutoVideoActivity.this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        //返回false的话只能响应长摁事件
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        super.onLongPress(e);
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        return super.onScroll(e1, e2, distanceX, distanceY);
                    }
                });
                mGesture.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        toLandingPage(AutoVideoActivity.this, VIDEO_CLICK_URL);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTapEvent(MotionEvent e) {
                        return false;
                    }
                });
            }
            return mGesture.onTouchEvent(event);
        }
    };



    @Override
    protected void onPause() {
        super.onPause();
        videoView.pause();
        currentPos = videoView.getCurrentPosition();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (currentPos != -1) {
            videoView.seekTo(currentPos);
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView = null;
    }

}
