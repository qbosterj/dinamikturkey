package cn.com.mma.mobile.tracking.util;

/**
 * Author:zhangqian
 * Time:2021/1/12
 * Version:
 * Description:MobileTracking
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.api.RecordEventMessage;

public class NetType extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String netType = getNetworkConnectionType(context);
        RecordEventMessage.deviceInfoParams.put(Constant.TRACKING_WIFI,netType);
        RecordEventMessage.updateInfoParams.put(Constant.TRACKING_WIFI,System.currentTimeMillis());
    }

    public String getNetworkConnectionType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return "2";
        //获取网络连接信息
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return "1";
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "0";
            }
        }
        return "2";
    }
}