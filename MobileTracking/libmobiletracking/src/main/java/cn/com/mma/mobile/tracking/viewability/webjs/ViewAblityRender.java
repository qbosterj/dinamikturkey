package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStats;

import static cn.com.mma.mobile.tracking.viewability.webjs.JSBridgeLoader.writeToArr;

/**
 * Author:zhangqian
 * Time:2020/11/27
 * Version:
 * Description:MobileTracking
 */
public class ViewAblityRender {

    private Context context;
    private SDK sdkconfig;

    public static boolean isExistenceBtr(String adURL, SDK sdkconfig){
        try {
            Company company = null;
            List<Argument> arguments = new ArrayList<>();

            if (sdkconfig == null || sdkconfig.companies == null) {
                Logger.e("没有读取到监测配置文件,当前事件无法监测!");
                return false;
            }
            String host = "";
            host= CommonUtil.getHostURL(adURL);
            for (Company companyItem : sdkconfig.companies) {
                if (host.endsWith(companyItem.domain.url)) {
                    company = companyItem;
                    break;
                }
            }
//            arguments = company.config.arguments;
            for (Argument argument : company.config.arguments) {
                if (argument.isRequired && !TextUtils.isEmpty(argument.key)) {
//                    String value = argument.value;
//                    arguments.add(argument);
                    if("STA".equals(argument.key)){
                        return true;
                    }
                }
            }

        }catch (Throwable e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isBeginRender(View adView){
        try {
            if(adView == null){
                return false;
            }
            //AdView尺寸:宽X高
            int width = adView.getWidth();
            int height = adView.getHeight();
//            Logger.i("width:" + width);
//            Logger.i("height:" + height);
            if(width * height < 0){
                return false;
            }
            if(adView instanceof ImageView){
                ImageView tempImage =(ImageView) adView;
                Bitmap drawable = ((BitmapDrawable)tempImage.getDrawable()).getBitmap();
                if(drawable == null){
                    return false;
                }
            }
//            Logger.i("BTR等于1");
            return true;
        }catch (Throwable e){
            return false;
        }

    }

    /**
     * 注入JS代码
     * @param webView
     * @param
     */
    public static void injectJavaScript(Context context, WebView webView, String filename){
        try {
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            final String vbJs = getJsFromAssets(context,filename);
            webView.loadUrl("javascript:"+ vbJs);

        }catch (Throwable e){

        }


    }

    public static String AssemblingUrl(String url, SDK sdkconfig, int type){
        try {
            Company company = null;
            List<Argument> arguments = new ArrayList<>();
            if (sdkconfig == null || sdkconfig.companies == null) {
                Logger.e("没有读取到监测配置文件,当前事件无法监测!");
                return "";
            }
            String host = "";
            host= CommonUtil.getHostURL(url);
            for (Company companyItem : sdkconfig.companies) {
                if (host.endsWith(companyItem.domain.url)) {
                    company = companyItem;
                    break;
                }
            }
            //将配置文件的arguments映射到JavaBean<HashMap>
            ViewAbilityStats abilityStats = new ViewAbilityStats();
            abilityStats.setSeparator(company.separator);
            abilityStats.setEqualizer(company.equalizer);
            abilityStats.setViewabilityarguments(company.config.viewabilityarguments);
            String videotype = abilityStats.get(ViewAbilityStats.ADVIEWABILITY_VIDEO_PLAYTYPE);
            if(!TextUtils.isEmpty(videotype)){
                //[1] 截取出原监测链接u参数之后所有的内容 REDIRECTURL
                String redirectPrefix = getRedirectIdentifier(company) + company.equalizer;
                String redirectStr = "";
                String patternStr = company.separator + redirectPrefix + ".*"; //,u=.*
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    redirectStr = matcher.group(0);
                }

//                Logger.i("redirectStr:::" + redirectStr);

                //[2] 监测链接截取掉REDIRECTURL字段之后的所有内容,重新组装为withoutRedirectURL
                String withoutRedirectURL;
                StringBuilder sb = new StringBuilder();
                try {
                    String[] splits = url.split(company.separator);
                    for (String item : splits) {
                        //如果遇到redirect标识,之后的内容都移除掉
                        if (item.startsWith(redirectPrefix)) {
                            break;
                        }
                        sb.append(item);
                        sb.append(company.separator);
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    withoutRedirectURL = sb.toString();

                } catch (Exception e) {
                    e.printStackTrace();
                    withoutRedirectURL = url;
                }
//                Logger.i("withoutRedirectURL:::" + withoutRedirectURL);

                StringBuffer stringBuffer = new StringBuffer(withoutRedirectURL);
                stringBuffer.append(company.separator);
                stringBuffer.append(videotype);
                stringBuffer.append(company.equalizer);
                stringBuffer.append(type+"");
                return stringBuffer.toString() + redirectStr;
            }else {
                return url;
            }
        }catch (Throwable e){
            return url;
        }

    }

    /**
     * 获取配置文件config.arguments标签内的REDIRECTURL的值
     *
     * @param company
     * @return
     * @throws Exception
     */
    private static String getRedirectIdentifier(Company company) {
        String redirectIdentifier = "u";
        List<Argument> arguments = company.config.arguments;
        if (arguments != null) {
            for (Argument argument : arguments) {
                if (argument != null && !TextUtils.isEmpty(argument.key)) {
                    if (argument.key.equals("REDIRECTURL")) {
                        redirectIdentifier = argument.value;
                        break;
                    }
                }
            }
        }
        return redirectIdentifier;
    }

    private static String getJsFromAssets(Context mContext, String filename) {
        InputStream is = null;
        String jsdata = "";
        try {
            is = mContext.getAssets().open(filename);
//            is = mContext.getAssets().open("mz_viewability_mobile.js");
            if (is != null) {
                byte[] buffer = writeToArr(is);
                jsdata = new String(buffer);
                return jsdata;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return jsdata;
                }
            }
        }
        return jsdata;
    }






}
