package cn.com.mma.mobile.tracking.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;

/**
 * Author:zhangqian
 * Time:2020/11/20
 * Version:
 * Description:MobileTracking
 */
public class DisplayPrenderAdViableHtml extends Activity {

    private static final String Sucess_HTML_URL = "http://omi-api-test.cn.miaozhen.com/btr-beta/plan-b/html_video_autoplay.html";
    private WebView webView;
    private String EXPOSE_URL = "http://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM__&tr=__REQUESTID__&mo=__OS__&m11=__OAID__&m0=__OPENUDID__&m0a=__DUID__&m1=__ANDROIDID1__&m1a=__ANDROIDID__&m2=__IMEI__&m4=__AAID__&m5=__IDFA__&m6=__MAC1__&m6a=__MAC__&nd=__DRA__&np=__POS__&nn=__APP__&nc=__VID__&m10=__HHHHHHH__&nf=__FLL__&ne=__SLL__&ng=__CTREF__&nx=__TRANSID__&vv=1&o=";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.html_ad);
        init();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    public void init(){

        webView = (WebView)findViewById(R.id.sample_display_webview);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.getSettings().setDomStorageEnabled(true);

        webView.getSettings().setAppCacheEnabled(false);

        Countly.sharedInstance().SetAddJavascriptMonitor(getApplicationContext(),webView);

        Countly.sharedInstance().webViewVideoImp(EXPOSE_URL, webView, 1,2, false,new CallBack() {
            @Override
            public void onSuccess(String eventType) {

            }
            @Override
            public void onFailed(String errorMessage) {

            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

            }

        });


        try {
            webView.loadUrl(Sucess_HTML_URL);
//            Thread.sleep(5000);


        }catch (Exception e){

        }


    }
}
