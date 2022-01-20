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
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.LRU;
import cn.com.mma.mobile.tracking.util.LocationCollector;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.OaidUtils;
import cn.com.mma.mobile.tracking.util.Reflection;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;

import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getAndroidId;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getAppName;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getDevice;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getImei;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getOSVersion;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getPackageName;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getResolution;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getWiFiBSSID;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.getWifiSSID;
import static cn.com.mma.mobile.tracking.util.DeviceInfoUtil.isWifi;
import static cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil.checkNeedDeviceUpdate;

/**
 * 记录事件
 */
public class RecordEventMessage {
    private final Context context;
    private static RecordEventMessage mInstance;
    public static LRU<String,CallBack> RequestHashMap;
    public static LRU<String, ViewAbilityHandler.MonitorType> MonitorTypeHashMap;
    public static Map<String,String> deviceInfoParams;
    public static Map<String,Long> updateInfoParams;
    private  SendMessageThread sendNormalMessageThread = null;
    private RecordEventMessage(final Context context) {
        if (context == null) {
            throw new NullPointerException("RecordEventMessage context can`t be null!");
        }
        this.context = context;
        deviceInfoParams = new HashMap<>();
        updateInfoParams = new HashMap<>();


    }
    public static RecordEventMessage getInstance(Context ctx) {
        if (mInstance == null) {
            synchronized (RecordEventMessage.class) {
                if (mInstance == null) {
                    mInstance = new RecordEventMessage(ctx);
                    RequestHashMap = new LRU<>(20);
                    MonitorTypeHashMap = new LRU<>(20);
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
        StringBuilder builder = new StringBuilder();
        try {
            String separator = company.separator;
            String equalizer = company.equalizer;

            HashMap<String,String> tempHashMap = DeviceInfoUtil.getUrlRawVaule(adURL,separator,equalizer);
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
            String urlvalue = "";
            for (Argument argument : arguments) {
                String argumentKey = argument.key;
                String argumentValue = argument.value;
                if (argumentKey.equals(Constant.TRACKING_TIMESTAMP)) {
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
//                    builder.append(System.currentTimeMillis());
                    builder.append(String.valueOf(company.timeStampUseSecond ? timestamp / 1000 : timestamp));
                } else if (argumentKey.equals(Constant.TRACKING_AAID)) {
                    String temp_aaid;
                    //判断是否超过24小时
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_aaid = Reflection.getPlayAdId(context);
                        deviceInfoParams.put(argumentKey,temp_aaid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                     temp_aaid = deviceInfoParams.get(argumentKey);

                     urlvalue = tempHashMap.get(argumentValue);
                    //字段不为空才进行参数拼接
                    if(!TextUtils.isEmpty(temp_aaid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.md5(temp_aaid));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
//                else if (argumentKey.equals(Constant.TRACKING_MUDS)) {
//                    builder.append(separator);
//                    builder.append(argumentValue);
//                    builder.append(equalizer);
//                    builder.append("");
//                }
                else if (argumentKey.equals(Constant.REDIRECTURL)) {
                    //将标识重定向的地址截取出来:链接argumentValue之后所有的内容
                    //优化监测链接里面可能出现重定向字符导致参数重复拼接的问题
                    String regex = separator + argumentValue + company.equalizer +".*";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(originURL);
                    if (matcher.find()) redirectUrlValue = matcher.group(0);
                }else if (argumentKey.equals(Constant.TRACKING_WIFIBSSID)) {
                    String temp_wifibssid;
                    //判断是否需要更新
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_wifibssid = getWiFiBSSID(context).replace(":", "").toUpperCase();
                        deviceInfoParams.put(argumentKey,temp_wifibssid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_wifibssid = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_wifibssid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.md5(temp_wifibssid));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }else if (argumentKey.equals(Constant.TRACKING_WIFISSID)) {
                    String temp_wifissid;
                    //判断是否需要更新
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_wifissid = getWifiSSID(context);
                        deviceInfoParams.put(argumentKey,temp_wifissid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_wifissid = deviceInfoParams.get(argumentKey);

                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_wifissid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_wifissid, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }else if (argumentKey.equals(Constant.TRACKING_WIFI)) {
                    String temp_wifi;
                    //判断是否需要更新
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_wifi = isWifi(context);
                        deviceInfoParams.put(argumentKey,temp_wifi);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_wifi = deviceInfoParams.get(argumentKey);

                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_wifi)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_wifi, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                } else if(argumentKey.equals(Constant.TRACKING_LOCATION) && company.sswitch.isTrackLocation) {
                    String temp_lbs;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_lbs = LocationCollector.getInstance(context).getLocation();
                        deviceInfoParams.put(argumentKey,temp_lbs);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_lbs = deviceInfoParams.get(argumentKey);
                    //地理位置更新可能会有延迟
                    if(TextUtils.isEmpty(temp_lbs)){
                        temp_lbs = LocationCollector.getInstance(context).getLocation();
                        deviceInfoParams.put(argumentKey,temp_lbs);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_lbs)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(temp_lbs);
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }else if(argumentKey.equals(Constant.TRACKING_OAID)){
                    String temp_oaid;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_oaid = OaidUtils.getOaid(context);
                        deviceInfoParams.put(argumentKey,temp_oaid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_oaid = deviceInfoParams.get(argumentKey);
                    if(TextUtils.isEmpty(temp_oaid)){
                        temp_oaid = OaidUtils.getOaid(context);
                        deviceInfoParams.put(argumentKey,temp_oaid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_oaid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
//                        builder.append(temp_oaid);
                        builder.append(CommonUtil.encodingUTF8(temp_oaid, argument, company));

                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }else if(argumentKey.equals(Constant.TRACKING_OAID1)){
                    String oaid1 = deviceInfoParams.get(Constant.TRACKING_OAID) ;
                    if(!TextUtils.isEmpty(oaid1)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
//                        builder.append(temp_oaid);
                        builder.append(CommonUtil.md5(oaid1));
                    }else {
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
//                        builder.append(temp_oaid);
                        builder.append(CommonUtil.md5(Constant.TRACKING_OAID));
                    }

                } else if(argumentKey.equals(Constant.TRACKING_ADID)){   //新增ADID
                    String temp_adid;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_adid = DeviceInfoUtil.ADID;
                        deviceInfoParams.put(argumentKey,temp_adid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                     temp_adid = deviceInfoParams.get(argumentKey);
                    if(TextUtils.isEmpty(temp_adid)){
                        temp_adid = DeviceInfoUtil.ADID;
                        deviceInfoParams.put(argumentKey,temp_adid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_adid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(temp_adid);
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_ANDROIDID)){
                    String temp_androidid;
                    //判断是否超过24小时
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_androidid = getAndroidId(context);
                        deviceInfoParams.put(argumentKey,temp_androidid);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_androidid = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_androidid)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_androidid, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_OS_VERION)){
                    String temp_os_version;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_os_version = getOSVersion();
                        deviceInfoParams.put(argumentKey,temp_os_version);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_os_version = deviceInfoParams.get(argumentKey);

                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_os_version)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_os_version, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_TERM)){
                    String temp_term;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_term = getDevice();
                        deviceInfoParams.put(argumentKey,temp_term);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_term = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_term)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_term, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_NAME)){
                    String temp_name;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_name = getAppName(context);
                        deviceInfoParams.put(argumentKey,temp_name);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_name = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_name)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_name, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_KEY)){
                    String temp_key;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_key = getPackageName(context);
                        deviceInfoParams.put(argumentKey,temp_key);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_key = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_key)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_key, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_SCWH)){
                    String temp_scwh;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_scwh = getResolution(context);
                        deviceInfoParams.put(argumentKey,temp_scwh);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_scwh = deviceInfoParams.get(argumentKey);
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_scwh)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_scwh, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if(argumentKey.equals(Constant.TRACKING_OS)){
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(CommonUtil.encodingUTF8("0", argument, company));
                }
                else if(argumentKey.equals(Constant.TRACKING_SDKVS)){
                    builder.append(separator);
                    builder.append(argumentValue);
                    builder.append(equalizer);
                    builder.append(CommonUtil.encodingUTF8(Constant.TRACKING_SDKVS_VALUE, argument, company));
                } else if (argumentKey.equals(Constant.TRACKING_IMEI)) {
                    String temp_imei;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_imei = getImei(context);
                        deviceInfoParams.put(argumentKey,temp_imei);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_imei = deviceInfoParams.get(argumentKey);
                    if(TextUtils.isEmpty(temp_imei)){
                        temp_imei = getImei(context);
                        deviceInfoParams.put(argumentKey,temp_imei);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(temp_imei)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_imei, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else if (argumentKey.equals(Constant.TRACKING_RAWIMEI)) {
                    String temp_raw_imei;
                    if(checkNeedDeviceUpdate(updateInfoParams,argumentKey)){
                        temp_raw_imei = getImei(context);
                        deviceInfoParams.put(argumentKey,temp_raw_imei);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    temp_raw_imei = deviceInfoParams.get(argumentKey);
                    if(TextUtils.isEmpty(temp_raw_imei)){
                        temp_raw_imei = getImei(context);
                        deviceInfoParams.put(argumentKey,temp_raw_imei);
                        updateInfoParams.put(argumentKey,System.currentTimeMillis());
                    }
                    urlvalue = tempHashMap.get(argumentValue);

                    if(!TextUtils.isEmpty(temp_raw_imei)){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(temp_raw_imei, argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
                else {
                    urlvalue = tempHashMap.get(argumentValue);
                    if(!TextUtils.isEmpty(deviceInfoParams.get(argumentKey))){
                        builder.append(separator);
                        builder.append(argumentValue);
                        builder.append(equalizer);
                        builder.append(CommonUtil.encodingUTF8(deviceInfoParams.get(argumentKey), argument, company));
                    }else {
                        if(urlvalue != null){
                            builder.append(separator);
                            builder.append(argumentValue);
                            builder.append(equalizer);
                            builder.append(urlvalue);
                        }
                    }
                }
            }

            String anti = company.antidevice;
            JSONObject result_jsonObject = new JSONObject();
            if(!TextUtils.isEmpty(anti)){
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isRoot))){
                    String temp_isroot;
                    if(checkNeedDeviceUpdate(updateInfoParams,AntiConstantStats.isRoot)){
                        temp_isroot = DeviceInfoUtil.checkRootFile() + "";
                        deviceInfoParams.put(AntiConstantStats.isRoot,temp_isroot);
                        updateInfoParams.put(AntiConstantStats.isRoot,System.currentTimeMillis());
                    }
                     temp_isroot = deviceInfoParams.get(AntiConstantStats.isRoot);
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isRoot),temp_isroot);
                }
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isSimulator))){
                    String temp_isSimulator;
                    if(checkNeedDeviceUpdate(updateInfoParams,AntiConstantStats.isSimulator)){
                        temp_isSimulator = DeviceInfoUtil.isEmulator(context)+ "";
                        deviceInfoParams.put(AntiConstantStats.isSimulator,temp_isSimulator);
                        updateInfoParams.put(AntiConstantStats.isSimulator,System.currentTimeMillis());
                    }
                     temp_isSimulator = deviceInfoParams.get(AntiConstantStats.isSimulator);
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isSimulator),temp_isSimulator);
                }
                if(!TextUtils.isEmpty(antiConstantStats.get(AntiConstantStats.isHook))){
                    String temp_ishook;
                    if(checkNeedDeviceUpdate(updateInfoParams,AntiConstantStats.isHook)){
                        temp_ishook = DeviceInfoUtil.getXposedCheckJar() +"";
                        deviceInfoParams.put(AntiConstantStats.isHook,temp_ishook);
                        updateInfoParams.put(AntiConstantStats.isHook,System.currentTimeMillis());
                    }
                    temp_ishook = deviceInfoParams.get(AntiConstantStats.isHook);
                    result_jsonObject.put(antiConstantStats.get(AntiConstantStats.isHook),temp_ishook);
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
                        signStr = CommonUtil.getSignature(Constant.TRACKING_SDKVS_VALUE, timestamp / 1000, getImei(context), getPackageName(context), getDevice(), builder.toString());
                    }else {
                        signStr = CommonUtil.getSignature(Constant.TRACKING_SDKVS_VALUE, timestamp / 1000,"", getPackageName(context), getDevice(), builder.toString());
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


//            Logger.i("redirectUrlValue:" + redirectUrlValue);

        } catch (Throwable e) {
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

        sendNormalMessageThread = new SendMessageThread(SharedPreferencedUtil.SP_NAME_NORMAL,context,true);
        sendNormalMessageThread.start();

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
