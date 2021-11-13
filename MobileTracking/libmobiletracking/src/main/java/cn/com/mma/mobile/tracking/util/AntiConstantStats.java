package cn.com.mma.mobile.tracking.util;


import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;

import cn.com.mma.mobile.tracking.bean.Argument;

public class AntiConstantStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /* 设备是否root */
    public static final String isRoot = "isRoot";
    /* 是否是模拟器 */
    public static final String isSimulator = "isSimulator";
    /* 是否存在hook*/
    public static final String isHook = "isHook";
    /* 是否开启ADB调试 */
    public static final String isAdb = "isAdb";

    /* 存储<sensorarguments>标签内所有的属性 */
    private HashMap<String, String> sensorarguments;
    /* 对应配置项<separator>标签 属性分隔符 default=, */
    private String separator;
    /* 对应配置项<equalizer>标签 属性链接符 default=空字符*/
    private String equalizer;


    public String get(String key) {
        return sensorarguments.get(key);
    }

    public void setSensorarguments(HashMap<String, Argument> arguments ) {

        HashMap<String, String> viewabilityarguments = new HashMap<>();

        if (arguments != null && arguments.size() > 0) {
            for (String argumentKey : arguments.keySet()) {
                String key = argumentKey;
                if (!TextUtils.isEmpty(key)) {
                    String value = arguments.get(key).value;
                    boolean isNeed = arguments.get(key).isRequired;
                    if (!TextUtils.isEmpty(value) && isNeed)
                        viewabilityarguments.put(key, value);
                }
            }
        }
        this.sensorarguments = viewabilityarguments;
    }


}
