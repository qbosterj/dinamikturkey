package cn.com.mma.mobile.tracking.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Author:zhangqian
 * Time:2021/1/29
 * Version:
 * Description:
 */
public class OaidUtils {

    private static String oaid_result = "";
    private static boolean iscontain = false;
    private static boolean isnewversion = false;

    public static String getOaid(Context context){
        try {
            if(!TextUtils.isEmpty(oaid_result)){
                return oaid_result;
            }
            getOaid(context, new MzCallback() {
                @Override
                public void callback(String oaid) {
                    oaid_result = oaid;
                }
            });
            if(isnewversion || TextUtils.isEmpty(oaid_result)){
                //新版本
                getNewOaid(context, new MzCallback() {
                    @Override
                    public void callback(String oaid) {
                        oaid_result = oaid;
                    }
                });
            }
            //新版本未获取到旧版本获取
            if(iscontain){
                getOldOaid(context, new MzCallback() {
                    @Override
                    public void callback(String oaid) {
                        oaid_result = oaid;
                    }
                });
            }
        }catch (Throwable e){
        }
        return oaid_result;

    }

    private static String getNewOaid(Context context, final MzCallback mzCallback){
        try {
            if(!TextUtils.isEmpty(oaid_result)){
                return oaid_result;
            }
            Class<?> mdidSdkHelper = Class.forName("com.bun.miitmdid.core.MainMdidSdk");
            Class<?> mCallback = Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");//接口
            Object instance = Proxy.newProxyInstance(
                    mCallback.getClassLoader(),
                    new Class[]{mCallback}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args){
                            try {
                                if (null != args && args.length>0){
                                for (Object arg:args){
                                    if("com.bun.miitmdid.provider.DefaultProvider".equals(arg.getClass().getName())){
                                        Class<?> mdidSdk = Class.forName("com.bun.miitmdid.provider.DefaultProvider");
                                        Method methodtest = mdidSdk.getDeclaredMethod("getOAID",null);
                                        String oaid = (String)methodtest.invoke(arg,null);
                                        mzCallback.callback(oaid);
                                        oaid_result = oaid;
                                        return oaid;
                                    }
                                }
                            }
                            }catch (Throwable e){
                                iscontain = true;
                            }
                            return null;
                        }
                    }
            );
            Method method1 = mdidSdkHelper.getDeclaredMethod("OnInit", Context.class, mCallback);
            method1.invoke(mdidSdkHelper.newInstance(), context,instance);
        }catch (Throwable e){
            iscontain = true;
            return "";
        }
        return "";
    }

    private static String getOldOaid(Context context, final MzCallback mzCallback){
        try {
            Class<?> jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
            Method oninit = jLibrary.getDeclaredMethod("InitEntry", Context.class);
            oninit.invoke(jLibrary.newInstance(),context);
            Class<?> mdidSdkHelper = Class.forName("com.bun.miitmdid.core.MdidSdk");
            Class<?> mCallback = Class.forName("com.bun.miitmdid.core.IIdentifierListener");
            Method init = mdidSdkHelper.getDeclaredMethod("InitSdk", Context.class,mCallback);
            Object instance = Proxy.newProxyInstance(
                    mCallback.getClassLoader(),
                    new Class[]{mCallback}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            for(Object arg:args){
                                if(arg.getClass().getName().contains("com.bun.miitmdid")){
                                    try {
                                        Class<?> mdidSdk = Class.forName(arg.getClass().getName());
                                        Method methodtest = mdidSdk.getDeclaredMethod("getOAID",null);
                                        String oaid = (String)methodtest.invoke(arg,null);
                                        mzCallback.callback(oaid);
                                    }catch (Exception e){
                                    }
                                }
                            }
                            return null;
                        }
                    }
            );
            init.invoke(mdidSdkHelper.newInstance(),context,instance);
        }catch (Throwable e){
            return "";
        }
        return "";
    }

    private static String getOaid(Context context, MzCallback mzCallback) {
        try {
            Class<?> mdidSdkHelper = Class.forName("com.bun.miitmdid.core.MdidSdkHelper");
            Class<?> mCallback = Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");//接口
            Object instance = Proxy.newProxyInstance(
                    mCallback.getClassLoader(),
                    new Class[]{mCallback}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            try {
                                for (Object arg : args) {
                                    if ("com.bun.miitmdid.pojo.IdSupplierImpl".equals(arg.getClass().getName())) {
                                        Class<?> mdidSdk = Class.forName("com.bun.miitmdid.interfaces.IdSupplier");
                                        Method methodtest = mdidSdk.getDeclaredMethod("getOAID");
                                        String oaid = (String) methodtest.invoke(arg);
                                        mzCallback.callback(oaid);
                                    }
                                }
                            }catch (Throwable e){
                                isnewversion = true;
                            }
                            return null;
                        }
                    });
            Method method = mdidSdkHelper.getDeclaredMethod("InitSdk", Context.class,boolean.class, mCallback);
            method.invoke(mdidSdkHelper.newInstance(), context,false, instance);
        } catch (Throwable e) {
            isnewversion = true;
//            e.printStackTrace();
            return "";
        }
        return "";
    }
}
