# 数字广告监测及验证统一SDK (Android) 部署指南

## 适用范围

Android SDK适用于 **Android 2.3.3（API Level 10）**及以上的设备。

## 集成SDK

### 步骤1: 导入SDK

请根据所用IDE选择导入方式：

#### Eclipse ADT 

将 SDK 中的  `jar` 文件拷贝到工程的 `libs` 文件夹中：右键选择jar包 **-->** **Build Path --> Add to Build Path**。

#### Android Studio

1. 在 Android Studio 项目的 `app` 文件夹中，新建 `libs` 文件夹。
2. 将 SDK `jar` 文件拷贝到新建的 `libs` 文件夹中。 
3. 修改 `app` 文件夹中的 `build.gradle` 文件，添加 `dependencies` 依赖项：

```
dependencies {
      …
      implementation files('libs/mmachina_sdk.jar') 
      …
  }
```

#### 导入签名库

将SDK中的 `libMMASignature.so`  文件拷贝到工程 `libs/armeabi` 目录下， SDK `lib_abi` 目录下对应不同CPU架构下的库文件，建议拷贝该目录下所有到工程 **app/libs **文件夹下，如果考虑APK大小，至少要包含主流架构：`armeabi`、`armeabi-v7a`、`arm64-v8a`。

#### 导入离线配置

将 `sdkconfig.xml` 配置文件拷贝到工程的 `assets` 目录下，在网络不好或者离线状态下读取缺省配置可以正常监测。同时 **配置文件** 上传到  web 服务器，使其可以通过 web 方式访问，便于后期灵活远程变更配置。



#### 配置权限

##### 常规权限

满足基本监测需要如下权限：

| 权限                   | 用途                                 |
| -------------------- | ---------------------------------- |
| INTERNET             | 允许程序联网和上报监测数据。                     |
| ACCESS_NETWORK_STATE | 允许检测网络连接状态，在网络异常状态下避免数据发送，节省流量和电量。 |
| READ_PHONE_STATE     | 允许访问手机设备的信息，通过获取的信息来唯一标识用户。        |
| ACCESS_WIFI_STATE    | 允许读取WiFi相关信息，在合适的网络环境下更新配置。        |

##### 扩展权限

在基本监测的基础上，如果想要回传**位置相关信息**，除了监测代码对应**sdkconfig**内**Company**标签的<isTrackLocation>项设置为**true**，还额外需要如下权限：

| 权限                     | 用途                    |
| ---------------------- | --------------------- |
| ACCESS_FINE_LOCATION   | 通过GPS方式获取位置信息。        |
| ACCESS_COARSE_LOCATION | 通过WiFi或移动基站的方式获取位置信息。 |

示例代码

```
<!--?xml version="1.0" encoding="utf-8"?-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="cn.com.mma.mobile.tracking.demo">
    <!-- SDK 所需常规权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 	<!-- 如果获取位置信息，需要声明以下权限 -->
 	<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 	<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<application ......>
  <activity ......>
  ......
  </activity>
    </application>
</manifest>
```

### 步骤2:使用方法

#### 引入类

在您需要使用 SDK 监测功能的类中，**import** 相关类：

```
import cn.com.mma.mobile.tracking.api.Countly;
```

#### SDK初始化

接口定义：

```
public void init(Context context, String configURL) 
```

参数说明：

| 参数        | 类型      | 说明                  |
| --------- | ------- | ------------------- |
| context   | Context | APP or Activity 上下文 |
| configURL | String  | 更新sdkconfig配置的远程地址  |

代码示例：在您的工程中的 Application 或者 Activity 中的 **onCreate** 中添加如下代码：

```
Countly.sharedInstance().init(this, "sdkconfig远程地址");
```



#### DisPlay曝光监测

曝光的定义：只有广告物料已经加载在客户端并至少已经开始渲染（Begin to render，简称BtR）时，才应称之为“曝光”事件。“渲染”指的是绘制物料的过程，或者指将物料添加到文档对象模型的过程。

如果进行曝光调用，则SDK会查验广告素材是否已开始渲染，如果是，则SDK会向监测方发出曝光上报。

备注：对广告进行可见性监测时，广告必须是满足开始渲染（Begin to render，简称BtR）条件的合法曝光，否则SDK不会执行可见监测。在调用可见曝光监测接口时，SDK会查验广告素材是否已开始渲染，如果是，则SDK会向监测方发出曝光上报，并继续进行可见监测，直到满足可见/不可见条件，再结束可见监测流程；如果不是，则SDK不会执行可见监测流程。

接口定义：

```
public void disPlayImp(String adUrl,View adView, int impType,CallBack callBack)

```

参数说明：

| 参数    | 类型     | 说明    |是否必填    |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位曝光监测代码 |是 |
| adview | View | 广告展示视图对象 |是 |
| type | int |0 代表曝光监测； 1 代表可视化曝光 |是 |
| callBack | CallBack |监测回调对象|是 |



示例代码：

```
   private String DISPLAY_IMP_URL = "https://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATF";
   Countly.sharedInstance().disPlayImp(DISPLAY_IMP_URL, adView, 0 ,new CallBack() {
             /**
              * 监测事件类型
              * @param eventType
              */
             @Override
             public void onSuccess(String eventType) {
                 //监测代码发送成功
             }
             @Override
             public void onFailed(String errorMessage) {
                 //监测代码发送失败

             }
         });
         
```

#### Video曝光监测

接口定义：

```
public void videoImp(String adUrl,View adView, int impType,int palyType, CallBack callBack)
```

参数说明：

| 参数    | 类型     | 说明        |是否必填        |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位曝光监测代码 |是 |
| adview | View | 广告展示视图对象 |是 |
| impType | int |0 代表曝光监测； 1 代表可视化曝光 |是 |
| palyType | int |视频播放类型 1-自动播放，2-手动播放，0-无法识别 |是|
| callBack | CallBack |监测回调对象|是 |



示例代码：

```
	 private String VIDEO_IMP_URL = "https://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATF";
     Countly.sharedInstance().videoImp(VIDEO_IMP_URL, adView, 0 ,1,new CallBack() {
             /**
              * 监测事件类型
              * @param eventType
              */
             @Override
             public void onSuccess(String eventType) {
                 //监测代码发送成功
             }
             @Override
             public void onFailed(String errorMessage) {
                 //监测代码发送失败

             }
         });
         
```

#### WebView 广告素材页面对接

使用说明：
在对WebView实现的广告位执行曝光监测时，andriod sdk会向webview动态注入js代码, 主动监测页面广告元素的BtR的状态,为了精确对某个广告素材进行监测,需在该标签元素上增加 `class ='umid-element'`样式标识.

示例代码：

 ```html5
 <!-- 视频广告 -->
 <video width="300" class="umid-element"  controls playsinline muted>
    <source src="./ad.mp4" type="video/mp4">
    <source src="./ad.webm" type="video/webm">
 </video>
<!-- 图片广告 -->
  <div style="margin-bottom: 0px;">
      <img alt="HTML Display Reference Ad" class="umid-element" src="./ads.png">
  </div>
 ```

#### WebView DisPlay曝光监测

接口定义：

使用说明：
在需要监测的WebView页面添加监听接口

注意：要在页面Finished之前添加接口监听，否则可能造成SDK无法进行Webview曝光监测。


```      
public void SetAddJavascriptMonitor(Context context,WebView webView)

```
参数说明：

| 参数    | 类型     | 说明        |是否必填        |
| ----- | ------ | --------- |--------- |
| context | Context | 上下文环境 |是 |
| webView | WebView | 加载广告的WebView对象 |是 |

示例代码：

```
Countly.sharedInstance().SetAddJavascriptMonitor(getApplicationContext(),webView);

```

在WebView的onPageFinished方法中开启监测，<font color =red>如果选择不注入js开启监测要放到页面加载之前，详情见附件</font>

```
public void webViewImp(String adUrl,View adView, int impType,boolean isInjectJs, CallBack callBack)

```

参数说明：

| 参数    | 类型     | 说明        |是否必填        |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位曝光监测代码 |是 |
| adview | View | 广告展示视图对象 |是 |
| impType | int |0 代表曝光监测； 1 代表可视化曝光 |是 |
| isInjectJs | boolean |true 动态注入js； false 不进行动态注入，需媒体自行在html内添加相关js代码，详见末尾附件 |是 |
| callBack | CallBack |监测回调对象|是 |

示例代码：

```
	  private String WebView_DISPLAY_IMP_URL = "https://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATF";

      Countly.sharedInstance().webViewImp(WebView_DISPLAY_IMP_URL, adView, 0 ,new CallBack() {
             /**
              * 监测事件类型
              * @param eventType
              */
             @Override
             public void onSuccess(String eventType) {
                 //监测代码发送成功
             }
             @Override
             public void onFailed(String errorMessage) {
                 //监测代码发送失败

             }
         });
         
```

#### WebView Video曝光监测

接口定义：

使用说明：
在需要监测的WebView页面添加监听接口

注意：要在页面Finished之前添加接口监听，否则可能造成SDK无法进行Webview曝光监测。

```      
public void SetAddJavascriptMonitor(Context context,WebView webView)

```
参数说明：

| 参数    | 类型     | 说明        |是否必填       |
| ----- | ------ | --------- |--------- |
| context | Context | 上下文环境 |是 |
| webView | WebView | 加载广告的WebView对象 |是 |

示例代码：

```
Countly.sharedInstance().SetAddJavascriptMonitor(getApplicationContext(),webView);

```

在WebView的onPageFinished方法中开启监测,<font color =red>如果选择不注入js开启监测要放到页面加载之前，详情见附件</font>


```
public void webViewVideoImp( String adUrl, View adView,  int impType, int palyType,boolean isInjectJs,CallBack callBack)

```

参数说明：

| 参数    | 类型     | 说明        |是否必填        |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位曝光监测代码 |是 |
| adview | View | 广告展示视图对象 |是 |
| impType | int |0 代表曝光监测； 1 代表可视化曝光 |是 |
| palyType | int |视频播放类型 1-自动播放，2-手动播放，0-无法识别 |是 |
| isInjectJs | boolean |true 动态注入js； false 不进行动态注入，需媒体自行在html内添加相关js代码，详见末尾附件 |是 |
| callBack | CallBack |监测回调对象|是 |

示例代码：

```
	  private String WebView_VIDEO_IMP_URL = "https://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM";
	
	  Countly.sharedInstance().webViewVideoImp(WebView_VIDEO_IMP_URL, adView, 0 ,2,new CallBack() {
	         /**
	          * 监测事件类型
	          * @param eventType
	          */
	         @Override
	         public void onSuccess(String eventType) {
	             //监测代码发送成功
	         }
	         @Override
	         public void onFailed(String errorMessage) {
	             //监测代码发送失败
	
	         }
	     });

```
#### 点击监测

接口定义：

```
public  void onClick(String adURL,CallBack callBack)
```

参数说明：

| 参数    | 类型     | 说明        |是否必填        |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位点击监测代码 |是 |
| callBack | CallBack |监测回调对象|是 |

示例代码：

```
 Countly.sharedInstance().onClick("http://example.com/axxx,bxxxx,c3,i0,h", new CallBack() {
            /**
             * 监测事件类型
             * @param eventType
             */
            @Override
            public void onSuccess(String eventType) {
 					//监测代码发送成功
            }

            @Override
            public void onFailed(String errorMessage) {
 					//监测代码发送失败
            }
        });
```


#### 停止可见性监测

接口定义：

```
public void stop(String adURL)
```

参数说明：

| 参数    | 类型     | 说明        |是否必填       |
| ----- | ------ | --------- |--------- |
| adURL | String | 广告位曝光监测代码 |是 |

SDK提供主动关闭可见性监测的功能，需要传入**已经开启可见性监测的广告位曝光监测代码**，如果传入错位的监测代码可能导致停止不生效。

示例代码：

```
        String adURL = "http://vxyz.admaster.com.cn/w/a86218,b1778712,c2343,i0,m202,8a2,8b2,2j,h";
        Countly.sharedInstance().onExpose(adURL, adView);
        new Handler().postDelayed(new Runnable(){
            public void run() {
                //5s后停止监测
                Countly.sharedInstance().stop(adURL);
            }
        }, 5000);
```




#### 调试模式

调试模式下，SDK会有LOG输出，APP发布时建议不要开启。（请在**初始化之前设置Log开关**，默认为false）。

接口定义：

```
public void setLogState(boolean debugmode)
```

参数说明：

| 参数        | 类型      | 说明                              |是否必填                              |
| --------- | ------- | ------------------------------- |------------------------------- |
| debugmode | boolean | true为打开SDK Log， false为关闭SDK Log |是 |

示例代码：

```
Countly.sharedInstance().setLogState(true);
```

#### 释放内存

SDK提供释放内存的接口，一般在应用即将退出时调用，或者等待系统内存管理自动释放。

接口定义：

```
public  void terminateSDK()
```

示例代码：

```
Countly.sharedInstance().terminateSDK();
```

#### 混淆配置

如果开发者的应用需要混淆，请在 `Proguard` 混淆配置文件中增加以下规则，以避免 SDK 不可用。

```
# SDK用到了v4包里的API，请确保v4相关support包不被混淆
-keep class android.support.v4.** { *; }
-dontwarn android.support.v4.**
```



### 步骤3:验证和调试

SDK 的测试有两个方面：

1. 参数是否齐全，URL 拼接方式是否正确。
2. 请求次数和第三方监测平台是否能对应上。

请联系第三方监测平台完成测试。


### 附件：

#### WebView 广告素材页面对接 (非动态注入js方案)
采用非注入模式添加示例代码如下：

#####步骤1
webView对象添加监听，如下所示：

```
  Countly.sharedInstance().SetAddJavascriptMonitor(getApplicationContext(),webView);
```
#####步骤2
在调用webView的<font color = red>setWebViewClient(new WebViewClient()之前</font>调用监测接口，如下所示：


```
  	  private String WebView_VIDEO_IMP_URL = "https://g.cn.miaozhen.com/x/k=1234567&p=778kb&dx=__IPDX__&rt=2&ns=__IP__&ni=__IESID__&v=__LOC__&xa=__ADPLATFORM";
	
	  Countly.sharedInstance().webViewVideoImp(WebView_VIDEO_IMP_URL, adView, 0 ,2,new CallBack() {
	         /**
	          * 监测事件类型
	          * @param eventType
	          */
	         @Override
	         public void onSuccess(String eventType) {
	             //监测代码发送成功
	         }
	         @Override
	         public void onFailed(String errorMessage) {
	             //监测代码发送失败
	
	         }
	     });
	     
```

 使用说明: 采用不注入的方式,需要事先埋入 js sdk 代码片段。 sdk 默认会主动监测页面广告元素的 BTR 的状态, 如果您想精确对某个广告素材进行监测, 只需加上在该标签元素上增加 `class ='umid-element'`样式标识。

添加JS示例代码:

```html5
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Document</title>
    <!-- js sdk 代码放入 head 元素内 -->
    <script>
    !function(){"use strict";var e=function(e){return JSON.stringify(e)},t=function(e){var t=e.getBoundingClientRect();return 0==t.width||0==t.height};var n,i,s=function(){return(s=Object.assign||function(e){for(var t,n=1,i=arguments.length;n<i;n++)for(var s in t=arguments[n])Object.prototype.hasOwnProperty.call(t,s)&&(e[s]=t[s]);return e}).apply(this,arguments)},o=[],r=new window.Map,a=function(e){var t;navigator.userAgent.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/)?window.webkit.messageHandlers.__mz_Monitor.postMessage(e):((t=navigator.userAgent).indexOf("Android")>-1||t.indexOf("Adr")>-1)&&window.__mz_Monitor.mz_push(e)},d=function(t){var n=e(t);r.has(n)||(r.set(n),o.push(e(s(s({},t),{time:(new Date).toISOString()})))),function(){for(;o.length;){var e=o.shift();e&&a(e)}}()};!function(e){e.JavaScript="javaScript",e.Image="image",e.Video="video",e.AUDIO="audio",e.HTML="html"}(n||(n={})),function(e){e.Impression="impression",e.ImpressionError="impressionError",e.WindowUnload="unload",e.Click="click",e.DisplayChange="displayChange",e.Start="start",e.FirstQuartile="firstQuartile",e.Midpoint="midpoint",e.ThirdQuartile="thirdQuartile",e.Complete="complete",e.Pause="pause",e.Resume="resume",e.BufferStart="bufferStart",e.BufferFinish="bufferFinish"}(i||(i={}));var h=function(){function e(e){this.advertisementType=n.Image,this.el=e,this.onError=this.onError.bind(this),this.onLoad=this.onLoad.bind(this),this.bindEvent(),this.checkVisible()}return e.prototype.checkVisible=function(){this.el.complete&&(t(this.el)?this.onError():this.imageComplete())},e.prototype.imageComplete=function(){var e=new Image;e.src=this.el.src,e.onload=this.onLoad,e.onerror=this.onError},e.prototype.bindEvent=function(){this.el.addEventListener("error",this.onError),this.el.addEventListener("load",this.onLoad)},e.prototype.send=function(e){d(s(s({},e),{advertisementType:this.advertisementType}))},e.prototype.onImpression=function(){this.send({eventType:i.Impression,ImpressionType:"beginToRender",isRender:"1"})},e.prototype.onError=function(){this.send({eventType:i.ImpressionError,ImpressionType:"beginToRender",isRender:"0"})},e.prototype.onLoad=function(){this.onImpression()},e.prototype.onClick=function(){},e.prototype.destroyed=function(){this.el.removeEventListener("error",this.onError),this.el.removeEventListener("load",this.onLoad),this.el=null},e}(),u=function(){function e(e){this.advertisementType=n.AUDIO,this.quartileEvents={"1q":!1,"2q":!1,"3q":!1,"4q":!1},this.isFirst=!1,this.isFullscreen=!1,this.el=e,this.isFirst=!0,this.onPlay=this.onPlay.bind(this),this.onPause=this.onPause.bind(this),this.onWaiting=this.onWaiting.bind(this),this.onTimeupdate=this.onTimeupdate.bind(this),this.onWebkitendfullscreen=this.onWebkitendfullscreen.bind(this),this.onWebkitbeginfullscreen=this.onWebkitbeginfullscreen.bind(this),this.bindEvent(),this.checkElement()}return e.prototype.checkElement=function(){t(this.el)?this.beforeImpression(!1):this.beforeImpression(!0)},e.prototype.bindEvent=function(){this.el.addEventListener("play",this.onPlay),this.el.addEventListener("pause",this.onPause),this.el.addEventListener("waiting",this.onWaiting),this.el.addEventListener("timeupdate",this.onTimeupdate),this.el.addEventListener("webkitendfullscreen",this.onWebkitendfullscreen),this.el.addEventListener("webkitbeginfullscreen",this.onWebkitbeginfullscreen)},e.prototype.onPlay=function(){this.isFirst?this.isFirst=!1:this.send({EventType:i.Resume})},e.prototype.onPause=function(){this.send({eventType:i.Pause})},e.prototype.onWaiting=function(){},e.prototype.onTimeupdate=function(){},e.prototype.onVolumechange=function(){},e.prototype.onWebkitendfullscreen=function(){this.isFullscreen=!1},e.prototype.onWebkitbeginfullscreen=function(){this.isFullscreen=!0},e.prototype.send=function(e){d(s(s({},e),{advertisementType:this.advertisementType,quartileEvents:this.quartileEvents}))},e.prototype.beforeImpression=function(e){this.send({EventType:i.Impression,ImpressionType:"beginToRender",isRender:e?"1":"0"})},e.prototype.destroyed=function(){this.el.removeEventListener("play",this.onPlay),this.el.removeEventListener("pause",this.onPause),this.el.removeEventListener("waiting",this.onWaiting),this.el.removeEventListener("timeupdate",this.onTimeupdate),this.el.removeEventListener("webkitendfullscreen",this.onWebkitendfullscreen),this.el.removeEventListener("webkitbeginfullscreen",this.onWebkitbeginfullscreen),this.el=null},e}(),l=function(){function e(){this.advertisementType=n.HTML,document.body.innerText.length>0?this.send({isRender:"1"}):this.send({isRender:"0"})}return e.prototype.send=function(e){d(s(s({},e),{advertisementType:this.advertisementType,ImpressionType:"beginToRender"}))},e}();var p,c,f=function(){var e,t=document.body.querySelector(".umid-element");if(t)return(e=t).tagName&&"video"===e.tagName.toLowerCase()?new u(t):function(e){return e.tagName&&"img"===e.tagName.toLowerCase()}(t)?new h(t):new l;var n=document.body.querySelector("video");if(n)return new u(n);var i=document.body.querySelectorAll("img");if(i.length){var s=function(e){for(var t=0,n=null,i=0;i<e.length;i++){var s=e[i],o=s.offsetWidth*s.offsetHeight;o>=t&&(t=o,n=s)}return n}(i);return new h(s)}return new l};function m(){f()}if(document.body)m();else{var v=(p=function(){m()},c=!1,function(){c||(c=!0,p.apply(this,arguments))});window.addEventListener("load",v),document.addEventListener("DOMContentLoaded",v)}}();
    </script>
  </head>
  <body>
    <video width="300" class="omid-element" controls playsinline  muted>
      <source src="./ad.mp4" type="video/mp4">
      <source src="./ad.webm" type="video/webm">
    </video>
</body>
</html>

```



