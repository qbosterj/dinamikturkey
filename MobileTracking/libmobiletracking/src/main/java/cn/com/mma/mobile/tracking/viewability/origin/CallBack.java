package cn.com.mma.mobile.tracking.viewability.origin;

import java.io.Serializable;

/**
 * Author:zhangqian
 * Time:2020-05-20
 * Version:
 * Description:MobileTracking
 */
public interface CallBack extends Serializable {
    void onSuccess(String eventType);
    void onFailed(String errorMessage);
}
