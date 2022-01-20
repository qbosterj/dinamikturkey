package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;
import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;

/**
 * Author:zhangqian
 * Time:2020/11/20
 * Version:
 * Description:MobileTracking
 */
public class ViewJavascriptInterface {

    private Context context;
    private CallBack callBack;
    private String adUrl;
    private int exposeType = 100;
    private int playType;
    private WebView webView;

    public ViewJavascriptInterface(Context context) {
        this.context = context;
    }

    /**
     * 动态注入js代码到ad html
     * @param data
     */
    @JavascriptInterface
    public void mz_push(String data) {
        try {
            JSONObject jsonObject =  new JSONObject(data);
            String strisRender = jsonObject.optString("isRender");
            if(!TextUtils.isEmpty(strisRender) && "1".equals(strisRender)){
//                isRender = true;
//                Logger.i("BTR:::" + isRender + playType);
                int impType = this.getExposeType();
                int palyType = this.getPlayType();
                CallBack callBack = this.getCallBack();
                WebView webView = this.getWebView();
                String adurl = this.getAdUrl();
//                Logger.i("曝光类型" + impType);

                switch (impType){
                    case 0:
                        Countly.sharedInstance().onExpose(adurl,webView,1,callBack);
                        break;
                    case 1:
                        if(palyType == 100){
                            Countly.sharedInstance().onExpose(adurl, webView,callBack);
                        }else {
                            Countly.sharedInstance().onVideoExpose(adurl,webView,palyType,callBack);
                        }
                        break;
                    default:
                        Logger.e("请输入正确的监测类型：0或者1");
                }

            }else {
                callBack.onFailed("None BtR");
            }

//            String impressionType = jsonObject.optString("ImpressionType");
//            String media = jsonObject.optString("Media");
//            Logger.e("MMAChinaSDK", "----ImpressionType:::" + impressionType);
//            Logger.e("MMAChinaSDK", "----Media:::" + media);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Logger.e("MMAChinaSDK", "----有参" + data);
    }


    public CallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public String getAdUrl() {
        return adUrl;
    }

    public void setAdUrl(String adUrl) {
        this.adUrl = adUrl;
    }

    public int getExposeType() {
        return exposeType;
    }

    public void setExposeType(int exposeType) {
        this.exposeType = exposeType;
    }

    public int getPlayType() {
        return playType;
    }

    public void setPlayType(int playType) {
        this.playType = playType;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }











}
