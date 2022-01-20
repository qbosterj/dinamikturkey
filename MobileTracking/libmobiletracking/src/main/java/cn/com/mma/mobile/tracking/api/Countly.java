package cn.com.mma.mobile.tracking.api;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;


import java.util.Timer;
import java.util.TimerTask;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.LocationCollector;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.NetType;
import cn.com.mma.mobile.tracking.util.OaidUtils;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityEventListener;
import cn.com.mma.mobile.tracking.viewability.webjs.ViewAblityRender;
import cn.com.mma.mobile.tracking.viewability.webjs.ViewJavascriptInterface;

/**
 * MMAChinaSDK Android API 入口类
 *
 */
public class Countly {

    private  SendMessageThread sendNormalMessageThread = null;
    private  SendMessageThread sendFailedMessageThread = null;

    private Timer normalTimer = null;
    private Timer failedTimer = null;

    private ViewAbilityHandler viewAbilityHandler = null;

    private volatile boolean sIsInitialized = false;
    private Context mContext;
    private RecordEventMessage mUrildBuilder;
    private boolean isTrackLocation = false;

    private static final String EVENT_CLICK = "onClick";
    private static final String EVENT_EXPOSE = "onExpose";
    private static final String EVENT_TRACKADS = "onTrackExpose";
    private static final String EVENT_VIEWABILITY_EXPOSE = "onAdViewExpose";
    private static final String EVENT_VIEWABILITY_VIDEOEXPOSE = "onVideoExpose";

    //[本地测试]控制广播的开关
    public static boolean LOCAL_TEST = true;
//    public static boolean ISNEED_OAID = false;

    private SDK sdk;

    public static String ACTION_STATS_EXPOSE = "ACTION_STATS_EXPOSE";
    public static String ACTION_STATS_VIEWABILITY = "ACTION.STATS_VIEWABILITY";
    public static String ACTION_STATS_SUCCESSED = "ACTION.STATS_SUCCESSED";
    private String injectjsname = "mz_viewability_mobile.min.js";
    private ViewJavascriptInterface viewJavascriptInterface;
    private NetType receiver;



    private static Countly mInstance = null;


    public static Countly sharedInstance() {
        if (mInstance == null) {
            synchronized (Countly.class) {
                if (mInstance == null) {
                    mInstance = new Countly();
                }
            }
        }
        return mInstance;
    }


    /**
     * 调试模式,打印Log日志,默认关闭
     * @param isPrintOut 是否开启log输出
     */
    public void setLogState(boolean isPrintOut) {
        Logger.DEBUG_LOG = isPrintOut;
    }


    /**
     * SDK初始化,需要手续
     * @param context 上下文
     * @param configURL 配置文件在线更新地址
     */
    public void init(Context context, String configURL) {
        if (context == null) {
            Logger.e("Countly.init(...) failed:Context can`t be null");
            return;
        }
        Context appContext = context.getApplicationContext();
        mContext = appContext;
        normalTimer = new Timer();
        failedTimer = new Timer();
        mUrildBuilder = RecordEventMessage.getInstance(context);
        try {
            //获取配置
            sdk = SdkConfigUpdateUtil.getSDKConfig(context);
            //初始化可视化监测模块,传入SDK配置文件
            viewAbilityHandler = new ViewAbilityHandler(mContext, viewAbilityEventListener, sdk);
            //初始化标识位放到后面
            if (sIsInitialized) {
                return;
            } else {
                sIsInitialized = true;
            }
            //Location Service
            if (isTrackLocation(sdk)) {
                isTrackLocation = true;
                LocationCollector.getInstance(mContext).syncLocation();
            }

            //监测配置更新
            SdkConfigUpdateUtil.sync(context, configURL);

           //获取ADID;
            DeviceInfoUtil.getDeviceAdid(context,sdk);
//            //初始化时尝试获取oaid
//            OaidUtils.getOaid(context);


        } catch (Exception e) {
            Logger.e("Countly init failed:" + e.getMessage());
        }

        //开启定时器
        startTask();
        receiver = new NetType();

        registerBrocast(context,receiver);
    }

    /**
     * 检测是否有开启Location的配置项
     * 规则:只要有任一Company有开启Location,则返回TRUE
     * @param sdkConfig
     * @return
     */

    private boolean isTrackLocation(SDK sdkConfig) {
        try {
            if (sdkConfig != null && sdkConfig.companies != null) {
                for (Company company : sdkConfig.companies) {
                    //广告监测HOST要和配置文件的公司域名相符
                    if (company.sswitch != null && company.sswitch.isTrackLocation) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 注册网络监听
     * @param context
     * @param receiver
     */
    private void registerBrocast(Context context, NetType receiver){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(receiver, intentFilter);
    }


    /**
     * 普通点击事件监测接口
     * @param adURL 监测链接
     */
    public void onClick(String adURL,CallBack callBack) {

        triggerEvent(EVENT_CLICK, adURL, null,0,callBack);
    }

    /**
     * 普通曝光事件监测接口
     * @param adURL 监测链接
     */
    public void onExpose(String adURL,View adview,int type,CallBack callBack) {
        triggerEvent(EVENT_EXPOSE, adURL, adview,type,callBack);
    }
    public void onTrackExpose(String adURL,View adview,int type,CallBack callBack) {
        triggerEvent(EVENT_TRACKADS, adURL, adview,type,callBack);
    }
    /**
     * 可视化曝光事件监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public void onExpose(String adURL, View adView,CallBack callBack) {
        triggerEvent(EVENT_VIEWABILITY_EXPOSE, adURL, adView,0,callBack);
    }

    /**
     * 可视化视频曝光事件监测接口
     *
     * @param adURL     监测链接
     * @param videoView 监测广告视频对象
     * @param videoPlayType 视频播放类型，1-自动播放，2-手动播放，0-无法识别
     */
    public void onVideoExpose(String adURL, View videoView, int videoPlayType,CallBack callBack) {

        triggerVideoEvent(EVENT_VIEWABILITY_VIDEOEXPOSE, adURL, videoView, videoPlayType,callBack);
    }

    /**
     * 添加监听
     * @param context
     * @param webView
     */
    public void SetAddJavascriptMonitor(Context context,WebView webView){

        viewJavascriptInterface = new ViewJavascriptInterface(context);
        webView.addJavascriptInterface(viewJavascriptInterface,"__mz_Monitor");
    }

    /**
     * 注入js
     * @param context
     * @param webView
     */
//    public void StartInjectJavascript(Context context,WebView webView){
//        ViewAblityRender.injectJavaScript(context,webView,injectjsname);
//
//    }


    /**
     *
     * @param adUrl 监测链接
     * @param adView 监测广告view对象
     * @param impType 曝光类型
     * @param callBack 监测回调对象
     */
    public void disPlayImp(String adUrl,View adView, int impType,CallBack callBack){
        switch (impType){
            case 0:
                //是否配置了BTR
                if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
//                    onExpose(adUrl,adView,0,callBack);
                    onTrackExpose(adUrl,adView,0,callBack);
                    if(ViewAblityRender.isBeginRender(adView)){
                        onExpose(adUrl,adView,1,callBack);
                    }
                }else {
                    //配置文件没有BTR判断参数
                    if(ViewAblityRender.isBeginRender(adView)){
                        onExpose(adUrl,adView,1,callBack);
                    }
                }
                break;
            case 1:
                //是否配置了BTR
//                Logger.i("可视化曝光类型");
                if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
//                    onExpose(adUrl,adView,0,callBack);
                    onTrackExpose(adUrl,adView,0,callBack);
                    if(ViewAblityRender.isBeginRender(adView)){
                        onExpose(adUrl, adView,callBack);
                    }
                }else {
                    //配置文件没有BTR判断参数
                    if(ViewAblityRender.isBeginRender(adView)){
                        onExpose(adUrl, adView,callBack);
                    }
                }
                break;
            default:
                Logger.e("请输入正确的监测类型：0或者1");
                break;

        }
    };

    /**
     *
     * @param adUrl 监测链接
     * @param adView 监测广告view对象
     * @param impType 曝光类型
     * @param palyType 自动播放:1;手动播放:2;无法识别:0
     * @param callBack 监测回调对象
     */
    public void videoImp(String adUrl,View adView, int impType,int palyType, CallBack callBack){

        if(adView == null){
            return;
        }
        switch (impType){
            case 0:
                //是否配置了BTR
                if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
//                    onExpose(adUrl,adView,0,callBack);
                    onTrackExpose(adUrl,adView,0,callBack);
                    if(ViewAblityRender.isBeginRender(adView)){
                        String tempUrl = ViewAblityRender.AssemblingUrl(adUrl,sdk,palyType);
                        if(!TextUtils.isEmpty(tempUrl)){
                            onExpose(tempUrl,adView,1,callBack);
                        }
                    }
                }else {
                    if(ViewAblityRender.isBeginRender(adView)){
                        onExpose(adUrl,adView,1,callBack);
                    }else {
                        //todo 没有BTR的话调用了曝光接口
                        callBack.onFailed("None BtR");
                    }
                }
                break;
            case 1:
                //是否配置了BTR
                if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
//                    onExpose(adUrl,adView,0,callBack);
                    onTrackExpose(adUrl,adView,0,callBack);
                    if(ViewAblityRender.isBeginRender(adView)){
                        onVideoExpose(adUrl,adView,palyType,callBack);
                    }
                }else {
                    if(ViewAblityRender.isBeginRender(adView)){
                        onVideoExpose(adUrl,adView,palyType,callBack);
                    }else {
                        //todo 没有BTR的话调用了可见曝光接口
                        callBack.onFailed("None BtR");
                    }
                }
                break;
            default:
                Logger.e("请输入正确的监测类型：0或者1");
                break;
        }
    }
    /**
     *
     * @param adUrl 监测链接
     * @param adView 监测广告view对象
     * @param impType 曝光类型
     * @param callBack 监测回调对象
     */
    public void webViewImp(String adUrl,View adView, int impType,boolean isInjectJs, CallBack callBack){
                try {
                    if(adView instanceof WebView){
                        if(ViewAblityRender.isBeginRender(adView) && adView != null){
                            WebView tempWebView = (WebView) adView;
                            viewJavascriptInterface.setAdUrl(adUrl);
                            viewJavascriptInterface.setCallBack(callBack);
                            viewJavascriptInterface.setExposeType(impType);
                            viewJavascriptInterface.setWebView(tempWebView);
                            //判断是否注入js代码
                            if(isInjectJs){
                                ViewAblityRender.injectJavaScript(mContext,tempWebView,injectjsname);
                            }
                            //是否配置了BTR
                            if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
//                                onExpose(adUrl,adView,0,callBack);
                                onTrackExpose(adUrl,adView,0,callBack);

                            }
                        }
                    }
                }catch (Throwable e){

                }
    }
    /**
     * @param adUrl 监测链接
     * @param adView 监测广告view对象
     * @param impType 曝光类型
     * @param palyType 自动播放:1;手动播放:2;无法识别:0
     * @param callBack 监测回调对象
     */
    public void webViewVideoImp( String adUrl, View adView,  int impType, int palyType,boolean isInjectJs,CallBack callBack){
        try {
            if(ViewAblityRender.isBeginRender(adView) && adView != null){
                WebView tempWebView = (WebView) adView;
                String tempurl = ViewAblityRender.AssemblingUrl(adUrl,sdk,palyType);
                if(TextUtils.isEmpty(tempurl)){
                    viewJavascriptInterface.setAdUrl(adUrl);
                }
                viewJavascriptInterface.setAdUrl(tempurl);
                viewJavascriptInterface.setCallBack(callBack);
                viewJavascriptInterface.setExposeType(impType);
                viewJavascriptInterface.setWebView(tempWebView);
                viewJavascriptInterface.setPlayType(palyType);
                //判断是否注入js代码
                if(isInjectJs){
                    ViewAblityRender.injectJavaScript(mContext,tempWebView,injectjsname);
                }
                if(ViewAblityRender.isExistenceBtr(adUrl,sdk)){
                    onExpose(adUrl,adView,0,callBack);
                }
            }
        }catch (Throwable e){
        }
    }


    /**
     * 可视化曝光监测停止接口
     *
     * @param adURL 要停止的监测链接
     */
    public void stop(String adURL) {
        if (sIsInitialized == false || viewAbilityHandler == null) {
            Logger.e("The method stop(...) should not be called before calling Countly.init(...)");
            return;
        }
        viewAbilityHandler.stop(adURL);
    }

    /**
     * 可视化曝光JS监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public void onJSExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, false);
    }

    /**
     * 可视化视频曝光JS监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public void onJSVideoExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, true);
    }

    /**
     * 释放SDK接口,在程序完全退出前调用
     */
    public  void terminateSDK() {
        try {
            if (normalTimer != null) {
                normalTimer.cancel();
                normalTimer.purge();
            }
            if (failedTimer != null) {
                failedTimer.cancel();
                failedTimer.purge();
            }
            if (isTrackLocation) LocationCollector.getInstance(mContext).stopSyncLocation();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            normalTimer = null;
            failedTimer = null;
            sendNormalMessageThread = null;
            sendFailedMessageThread = null;
            mUrildBuilder = null;
            if (viewAbilityHandler != null) viewAbilityHandler = null;
            sIsInitialized = false;
            mInstance = null;
        }
    }

    private  void triggerEvent(String eventName, String adURL, View adView,int type,CallBack callBack) {
        triggerEvent(eventName, adURL, adView, 0, type,callBack);

    }

    private  void triggerVideoEvent(String eventName, String adURL, View adView,int videoPlaytype,CallBack callBack) {
        triggerEvent(eventName, adURL, adView, videoPlaytype, 0,callBack);
    }



    private  void triggerEvent(String eventName, String adURL, View adView, int videoPlayType, int type, CallBack callBack) {


        if (sIsInitialized == false || mUrildBuilder == null) {
            Logger.e("The method " + eventName + "(...) should be called before calling Countly.init(...)");
            return;
        }
        if (TextUtils.isEmpty(adURL)) {
            Logger.w("The URL parameter is illegal, it can't be null or empty!");
            return;
        }

        switch (eventName) {
            case EVENT_CLICK:
                viewAbilityHandler.onClick(adURL,callBack);
                break;
            case EVENT_TRACKADS:
                viewAbilityHandler.onTrackExpose(adURL,adView,type,callBack);
                break;
            case EVENT_EXPOSE:
                viewAbilityHandler.onExpose(adURL,adView,type,callBack);
                break;
            case EVENT_VIEWABILITY_EXPOSE:
                viewAbilityHandler.onExpose(adURL, adView,callBack);
                break;
            case EVENT_VIEWABILITY_VIDEOEXPOSE:
                viewAbilityHandler.onVideoExpose(adURL, adView, videoPlayType,callBack);
                break;

        }
    }


    private  void startTask() {
        try {
//            normalTimer.schedule(new TimerTask() {
//                public void run() {
//                    startNormalRun();
//                }
//            }, 0, Constant.ONLINECACHE_QUEUEEXPIRATIONSECS * 1000);

            failedTimer.schedule(new TimerTask() {
                public void run() {
                    startFailedRun();
                }
            }, 0, Constant.OFFLINECACHE_QUEUEEXPIRATIONSECS * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void startNormalRun() {
        try {
            if (sendNormalMessageThread != null && sendNormalMessageThread.isAlive()) return;

            SharedPreferences sp = SharedPreferencedUtil.getSharedPreferences(mContext, SharedPreferencedUtil.SP_NAME_NORMAL);
            if ((sp == null) || (sp.getAll().isEmpty())) return;

            sendNormalMessageThread = new SendMessageThread(SharedPreferencedUtil.SP_NAME_NORMAL, mContext, true);
            sendNormalMessageThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void startFailedRun() {
        try {
            if (sendFailedMessageThread != null && sendFailedMessageThread.isAlive()) return;

            SharedPreferences sp = SharedPreferencedUtil.getSharedPreferences(mContext, SharedPreferencedUtil.SP_NAME_FAILED);
            if ((sp == null) || (sp.getAll().isEmpty())) return;

            sendFailedMessageThread = new SendMessageThread(SharedPreferencedUtil.SP_NAME_FAILED, mContext, false);
            sendFailedMessageThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 接收来自ViewAbilityService的回调,触发监测事件
     *
     * @param adURL 带ViewAbility监测结果的链接
     */
    private ViewAbilityEventListener viewAbilityEventListener = new ViewAbilityEventListener() {
        @Override
        public void onEventPresent(String adURL, CallBack callBack, ViewAbilityHandler.MonitorType monitortype) {
            if (sIsInitialized && mUrildBuilder != null) {
                mUrildBuilder.recordEvent(adURL,callBack,monitortype);
            }
        }
    };

}
