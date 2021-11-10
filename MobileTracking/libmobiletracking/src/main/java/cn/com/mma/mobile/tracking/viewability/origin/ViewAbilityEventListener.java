package cn.com.mma.mobile.tracking.viewability.origin;


import cn.com.mma.mobile.tracking.api.ViewAbilityHandler;

/**
 * Created by yangxiaolong on 17/6/20.
 */
public interface ViewAbilityEventListener {

    void onEventPresent(String destUrl, CallBack callBack, ViewAbilityHandler.MonitorType monitorType);
}
