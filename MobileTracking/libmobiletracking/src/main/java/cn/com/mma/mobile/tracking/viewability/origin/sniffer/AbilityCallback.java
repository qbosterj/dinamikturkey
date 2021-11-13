package cn.com.mma.mobile.tracking.viewability.origin.sniffer;


import cn.com.mma.mobile.tracking.api.ViewAbilityHandler;
import cn.com.mma.mobile.tracking.viewability.origin.CallBack;

/**
 * Created by mma on 17/6/19.
 */
public interface AbilityCallback {


    /* 触发MMA 曝光监测 */
    void onSend(String trackURL, CallBack callBack, ViewAbilityHandler.MonitorType monitorType);

    /* 移除监测任务 */
    void onFinished(String taskID);

}
