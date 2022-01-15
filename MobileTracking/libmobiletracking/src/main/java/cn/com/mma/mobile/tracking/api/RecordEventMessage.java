package cn.com.mma.mobile.tracking.api;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.AntiConstantStats;
import cn.com.mma.mobile.tracking.util.AppListUploader;
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.LocationCollector;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.Reflection;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStats;

/**
 * 记录事件
 */
public class RecordEventMessage {
    private final Context context;
    private static RecordEventMessage mInstance;
    public static HashMap<String,CallBack> RequestHashMap;
    public static HashMap<String, ViewAbilityHandler.MonitorType> MonitorTypeHashMap;

    private RecordEventMessage(final Context context) {
        if (context == null) {
            throw new NullPointerException("RecordEventMessage context can`t be null!");
        }
        this.context = context;
    }

    public static RecordEventMessage getInstance(Context ctx) {
        if (mInstance == null) {
            synchronized (RecordEventMessage.class) {
                if (mInstance == null) {
                    mInstance = new RecordEventMessage(ctx);
                    RequestHashMap = new HashMap<>();
                    MonitorTypeHashMap = new HashMap<>();
                }
            }
        }
        return mInstance;
    }

    protected synchronized void recordEvent(String originURL, CallBack callBack, ViewAbilityHandler.MonitorType monitorType) {


        String adURL = originURL.trim();
        long timestamp = System.currentTimeMillis();
        Company company = null;

        SDK sdk = SdkConfigUpdateUtil.getSDKConfig(context);
        if (sdk == null || sdk.companies == null) {
            Logger.e("没有读取到监测配置文件,当前事件无法监测!");
            return;
        }
        String host = "";
        try {
            host= CommonUtil.getHostURL(adURL);
            for (Company companyItem : sdk.companies) {
                if (host.endsWith(companyItem.domain.url)) {
                    company = companyItem;
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (company == null) {
            Logger.w("监测链接: \'" + originURL + "\' 没有对应的配置项,请检查sdkconfig.xml");
            return;
        }


        //获取配置文件相关
        AntiConstantStats antiConstantStats = new AntiConstantStats();
        antiConstantStats.setSensorarguments(company.config.sensorarguments);

        Map<String,String> deviceInfoParams = DeviceInfoUtil.getDeviceInfo(context);

        StringBuilder builder = new StringBuilder();
        try {
            String separator = company.separator;
            String equalizer = company.equalizer;

            String filteredURL = adURL;
            List<Argument> arguments = new ArrayList<>();
            //required argument fill in lists
            for (Argument argument : company.config.arguments) {
                if (argument.isRequired && !TextUtils.isEmpty(argument.key)) {
                    String value = argument.value;
                    arguments.add(argument);
                    if (!TextUtils.isEmpty(value)) {
                        //过滤掉URL中和Arguments重复的保留字段去燥
                        String argumentValue = separator + value + equalizer;
                        if (filteredURL.contains(argumentValue)) {
                            String regex = argumentValue + "[^" + separator + "]*";
                            filteredURL = filteredURL.replaceAll(regex, "");
                        }
                    }
                }
            }
            builder.append(filteredURL);

            //deviceinfo
            String redirectUrlValue = "";
            for (Argument argument : arguments) {
                String argumentKey = argument.key;
                String argumentValue = argument.value;
                if (argumentKey.equals(Constant.TRACKING_TIMESTAMP)) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(String.valueOf(company.timeStampUseSecond ? timestamp / 1000 : timestamp));
                } else if (argumentKey.equals(Constant.TRACKING_AAID)) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(CommonUtil.md5(deviceInfoParams.get(argumentKey)));
                } else if (argumentKey.equals(Constant.TRACKING_MUDS)) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append("");
                } else if (argumentKey.equals(Constant.REDIRECTURL)) {
                    //将标识重定向的地址截取出来:链接argumentValue之后所有的内容
                    //优化监测链接里面可能出现重定向字符导致参数重复拼接的问题
                    String regex = separator + argumentValue + company.equalizer +".*";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(originURL);
                    if (matcher.find()) redirectUrlValue = matcher.group(0);
                }else if (argumentKey.equals(Constant.TRACKING_WIFIBSSID)) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(CommonUtil.md5(deviceInfoParams.get(argumentKey)));
                }else if(argumentKey.equals(Constant.TRACKING_LOCATION) && company.sswitch.isTrackLocation) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(LocationCollector.getInstance(context).getLocation());
                }else if(argumentKey.equals(Constant.TRACKING_OAID)){
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(deviceInfoParams.get(argumentKey));
                }else if(argumentKey.equals(Constant.TRACKING_ADID)){   //新增ADID
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(deviceInfoParams.get(argumentKey));
                }
                else {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(CommonUtil.encodingUTF8(deviceInfoParams.get(argumentKey), argument, company));
                }
            }

            String anti = company.antidevice;

            JSONObject result_jsonObject = new JSONObject();

            if(!TextUtils.isEmpty(anti)){
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isRoot))){
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isRoot),DeviceInfoUtil.checkRootFile());
                }
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isSimulator))){
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isSimulator),DeviceInfoUtil.isEmulator(context));
                }
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isHook))){
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isHook),DeviceInfoUtil.getXposedCheckJar());
                }
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isAdb))){
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isAdb),DeviceInfoUtil.getCheckAdb(context));
                }
                builder.append(separator);
                builder.append(anti);
                builder.append(equalizer);
//                builder.append(result_jsonObject.toString());
                builder.append(CommonUtil.encodingUTF8(result_jsonObject.toString()));
            }
            //signature
            if (company.signature != null && company.signature.paramKey != null) {
                //String signStr = CommonUtil.getSignature(context, builder.toString());
                //权限检查
                boolean permCheck = Reflection.checkPermission(context, Manifest.permission.READ_PHONE_STATE) ||
                        Reflection.checkPermissionX(context, Manifest.permission.READ_PHONE_STATE);

                boolean versionCheck = Build.VERSION.SDK_INT < 29;
                //传入的监测代码host之后如果缺失分隔符/,会导致签名时Native处切割host时引起Crash，需要在传入的监测链接时判断或Native层修复
                String checkURL = filteredURL.replace(host, "");
                if (checkURL.contains("/")) {
                    String signStr = "";
                    if(permCheck && versionCheck){
                        signStr = CommonUtil.getSignature(Constant.TRACKING_SDKVS_VALUE, timestamp / 1000,DeviceInfoUtil.getImei(context),DeviceInfoUtil.getPackageName(context),DeviceInfoUtil.getDevice(), builder.toString());
                    }else {
                        signStr = CommonUtil.getSignature(Constant.TRACKING_SDKVS_VALUE, timestamp / 1000,"",DeviceInfoUtil.getPackageName(context),DeviceInfoUtil.getDevice(), builder.toString());
                    }
                    builder.append(separator);
                    builder.append(company.signature.paramKey);
                    builder.append(equalizer);
                    builder.append(CommonUtil.encodingUTF8(signStr));
                } else {
                    Logger.w("The monitor URL format is illegal,signature verification failed!");
                }

            }
            //redirectURL
            builder.append(redirectUrlValue);

        } catch (Exception e) {
            Logger.e(e.getMessage());
        }

        String exposeURL = builder.toString();
        long expirationTime = getEventExpirationTime(company, timestamp);
        //Logger.d(" exposeURL:" + exposeURL + "   expirationTime is:" + expirationTime);

        //可见曝光的普通曝光
        if(monitorType == ViewAbilityHandler.MonitorType.EXPOSEWITHABILITY || monitorType == ViewAbilityHandler.MonitorType.VIDEOEXPOSEWITHABILITY){
            monitorType = ViewAbilityHandler.MonitorType.IMPRESSION;
        }
        //在这里添加立即发送逻辑
        RequestHashMap.put(exposeURL,callBack);
        //保存曝光类型的状态
        MonitorTypeHashMap.put(exposeURL,monitorType);
        SharedPreferencedUtil.putLong(context, SharedPreferencedUtil.SP_NAME_NORMAL, exposeURL, expirationTime);

        //检查是否可以上报APPLIST
        AppListUploader.getInstance(context).sync(originURL, company);

    }

    /**
     * 获取Event超时时间,从当前time+配置的缓存有效期 = n天后的时间
     * 如果修改设备系统时间,会影响正确性
     *
     * @param company
     * @param timestamp event产生时间
     * @return
     */
    private long getEventExpirationTime(Company company, long timestamp) {
        long expiration = 0;
        try {
            if (!TextUtils.isEmpty(company.sswitch.offlineCacheExpiration)) {
                Long cachexpiration = Long.parseLong(company.sswitch.offlineCacheExpiration.trim());
                //秒转化为毫秒
                expiration = cachexpiration * 1000 + timestamp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (expiration == 0) expiration = Constant.TIME_ONE_DAY + timestamp;

        return expiration;
    }




}
