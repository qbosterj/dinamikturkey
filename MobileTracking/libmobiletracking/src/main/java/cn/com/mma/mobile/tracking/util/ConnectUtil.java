package cn.com.mma.mobile.tracking.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class ConnectUtil {

    //private static final String CHARSET = "UTF-8";
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    private static final int CONNECT_TIMEOUT = 30 * 1000;
    private static final int READ_TIMEOUT = 30 * 1000;

    private static ConnectUtil instance;


    private ConnectUtil() {
        try {

            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllManager, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static ConnectUtil getInstance() {
        if (instance == null) {
            synchronized (ConnectUtil.class) {
                if (instance == null) {
                    instance = new ConnectUtil();
                }
            }
        }
        return instance;
    }

//    public HttpURLConnection getHttpURLConnection(String url) {
//        try {
//            String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
//            HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
//            conn.setConnectTimeout(CONNECT_TIMEOUT);
//            conn.setReadTimeout(READ_TIMEOUT);
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestMethod("GET");
//            // not allow auto controll redirect,仅作用于当前URLConnection对象
//            conn.setInstanceFollowRedirects(false);
//            return conn;
//        } catch (Exception e) {
//        }
//        return null;
//    }
//
//    public String doRequest(String url) {
//        String result = null;
//        InputStream is = null;
//        BufferedReader br = null;
//        HttpURLConnection conn = null;
//        try {
//            conn = getHttpURLConnection(url);
//            if (conn == null) {
//                return null;
//            }
//            is = conn.getInputStream();
//            if (is == null) {
//                return null;
//            }
//            br = new BufferedReader(new InputStreamReader(is, CHARSET));
//            String line;
//            StringBuffer sb = new StringBuffer();
//            while ((line = br.readLine()) != null) {
//                sb.append(line);
//            }
//            result = sb.toString();
//        } catch (Exception e) {
//        } finally {
//            try {
//                if (is != null)
//                    is.close();
//            } catch (IOException e) {
//            }
//            try {
//                if (br != null)
//                    br.close();
//            } catch (IOException e) {
//            }
//            if (conn != null) {
//                conn = null;
//            }
//        }
//        return result;
//    }

    public byte[] performGet(String destURL) {
        //Logger.d("Attempting Get to " + destURL + "\n");
        byte[] response = null;
        HttpURLConnection httpConnection = null;
        InputStream is = null;

        try {
            String encodedUrl = Uri.encode(destURL, ALLOWED_URI_CHARS);

            URL url = new URL(encodedUrl);

            httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            int statusCode = httpConnection.getResponseCode();



            if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {

                try {
                    is = httpConnection.getInputStream();
                    response = writeToArr(is);
                } catch (Exception e) {
                    response = new byte[]{};
                }

                //redirect
                String redirectURL = httpConnection.getHeaderField("Location");
                if (!TextUtils.isEmpty(redirectURL)) {
                    //SDK访问o后面的落地页，并不能真正的打开具体页面，跳转的具体动作媒体自己完成
                    httpConnection = (HttpURLConnection) new URL(redirectURL).openConnection();
                    statusCode = httpConnection.getResponseCode();

                    //Logger.d("redirect statusCode::" + statusCode);
                }
            }
        } catch (Exception e) {

//            System.out.println("upload监测链接异常:" + e.toString());


        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (final IOException e) {
                }
            if (null != httpConnection)
                
                httpConnection.disconnect();
        }

        return response;
    }

    public byte[] performPost(String destURL, String data,boolean useGzip) {
        //Logger.d("Attempting Post to " + destURL + "\n");

        byte[] response = null;

        OutputStream os = null;
        BufferedOutputStream bos = null;
        HttpURLConnection httpConnection = null;
        InputStream is = null;

        try {
            URL url = new URL(destURL);
            httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "text/plain");
            //httpConnection.setRequestProperty("Accept-Encoding", "gzip,deflate");

            if (useGzip) {
                httpConnection.setRequestProperty("Content-Encoding", "gzip");
            }

            os = httpConnection.getOutputStream();//upload

            if (useGzip) {
                byte[] buffer = eGzip(data.getBytes("UTF-8"));
                os.write(buffer);
                os.flush();
            } else {
                bos = new BufferedOutputStream(os);
                bos.write(data.getBytes("UTF-8"));
                bos.flush();
            }

            int statusCode = httpConnection.getResponseCode();


            if (statusCode == HttpURLConnection.HTTP_OK) {
                // 使用普通流读取
                is = httpConnection.getInputStream();
                response = writeToArr(is);
            }
        } catch (Exception e) {
        } finally {
            if (null != bos) {
                try {
                    bos.close();
                } catch (final IOException e) {
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (final IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }

            if (null != httpConnection)
                httpConnection.disconnect();
        }

        return response;
    }

    /**
     * 读取到Buffer内，转换成byte[]数组
     */
    private static byte[] writeToArr(final InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        // buffer.close();
        return buffer.toByteArray();
    }

    /**
     * 把byte 通过GZipStream封装
     * @param content
     * @return
     */
    private static byte[] eGzip(byte[] content) {
        GZIPOutputStream gos = null;
        try {
            // 通过一个缓冲的byte[] 对标准输出流进行封装,不需要主动close
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            gos = new GZIPOutputStream(baos);
            gos.write(content);
            gos.finish();
            gos.close();
            gos = null;
            return baos.toByteArray();
        } catch (Exception e) {
        } finally {
            if (gos != null) {
                try {
                    gos.close();
                } catch (IOException e) {
                }
                gos = null;
            }
        }
        return null;
    }


    /**
     * 网络ID获取
     * @param urlString
     * @return
     */
    public static String requestID(String urlString){
        String value ="空值";
//        changeUI("正在网络请求ID");
        String resultData = "";
        try {

            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);

            URL url = new URL(urlString);
            HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
            //设置超时时间
            urlConn.setConnectTimeout(CONNECT_TIMEOUT);
            urlConn.setRequestMethod("GET");

            int code =urlConn.getResponseCode();

            //获取所有Header
            Map<String, List<String>> map = urlConn.getHeaderFields();
            List<String> cookies = map.get("Set-Cookie");
            for (int i=0; i < cookies.size();i++){
                String a =cookies.get(i);
                if(a!= null && a.contains("a=")){
                    value = a.split(";")[0];
                    value = value.split("=")[1];
//                    request.completed(value);
                }
            }
            //            storeCookies(urlConn);
            //关闭http连接
            urlConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }


    private static TrustManager[] trustAllManager = {new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkServerTrusted(X509Certificate[] certs, String authType) {

        }
    }};

    private static class NullHostNameVerifier implements HostnameVerifier {
        public NullHostNameVerifier() {
        }

        @SuppressLint("BadHostnameVerifier")
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * 网络ID获取
     * @param urlString
     * @return
     */
    public String requestID(Context context, String urlString, RequestSuccess request){
        String value ="unknow";
//        changeUI("正在网络请求ID");
        String resultData = "";
        try {

            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);

            URL url = new URL(urlString);
            HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
            //设置超时时间
            urlConn.setConnectTimeout(CONNECT_TIMEOUT);
            urlConn.setRequestMethod("GET");

            int code =urlConn.getResponseCode();

//            Log.d("lincoln","请求code"+code);
            //获取所有Header
            Map<String, List<String>> map = urlConn.getHeaderFields();
            List<String> cookies = map.get("Set-Cookie");
            for (int i=0; i < cookies.size();i++){
                String a =cookies.get(i);
                if(a!= null && a.contains("a=")){
                    value = a.split(";")[0];
                    value = value.split("=")[1];

                    request.completed(value);
                }
            }
            //            storeCookies(urlConn);
            //关闭http连接
            urlConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
//            Log.d("qianqian","请求code"+e.toString());

        }

        return value;
    }

    /**
     * 请求ADID成功后的回调接口
     */
    public interface RequestSuccess{
        void completed(String result);
    }

}
