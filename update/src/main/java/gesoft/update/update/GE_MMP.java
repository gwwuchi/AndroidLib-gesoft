package gesoft.update.update;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.io.IOException;

import gesoft.update.R;
import gesoft.update.common.UpdateManager;

/**
 * Created by Administrator on 2016/7/14.
 */

public class GE_MMP {

    private static String GE_MMP_URL = "http://www.gytaobao.cn:9218/GE_MMP";
    private static final String URL_VERSION = GE_MMP_URL + "/json/version_get.action";
    private static NameValuePair[] parts;
    private static Context mContext;
    private static int sign;
    private static JSONObject json;
    private static AlertDialog dialog;

    /**
     *获取版本信息
     * @param context 上下文
     * @param i 0 首页调用，1设置调用
     */
    public static void initialize(Context context, int i){
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //IMEI码
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = telManager.getDeviceId();

        parts = new NameValuePair[]{new NameValuePair("package_name", context.getPackageName()),
                new NameValuePair("phone_type",  Build.MODEL+"_"+ Build.VERSION.RELEASE),
                new NameValuePair("os_type",  "Android"),
                new NameValuePair("device_id",  device_id),
                new NameValuePair("app_version", packageInfo.versionName)};

        mContext = context;

        sign = i;
        new Thread(runnable).start();
    }

    static Runnable runnable = new Runnable() {

        @Override
        public void run() {
            try {

                json = new JSONObject(GetInfoHttpPost(URL_VERSION, parts));
                if(null != json && json.length()>0){
                    handler.obtainMessage(1,true).sendToTarget();
                }else{
                    handler.obtainMessage(0,true).sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    static Handler handler = new Handler(){

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Toast.makeText(mContext, "操作失败！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    checkVersion(mContext,json,sign);
                    break;
            }
        }
    };


    /**
     * 系统更细
     *
     * @author Administrator
     * @date 2015年12月2日
     */
    private static void checkVersion(Context context, JSONObject j, int sign){
        JSONObject json = j.optJSONObject("data");
        String url = json.optString("url");
        String m = json.optString("remark");
        int force =  json.optInt("force_update");
        double v = json.optDouble("version");

        if(TextUtils.isEmpty(m)){
            m = "发现新的版本，请及时更新下载！";
        }
        if( v > Double.parseDouble(getVersionCode(context)) && force == 0){
            final UpdateManager mUpdateManager = new UpdateManager(context, url);
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.update_dialog, null);
            TextView content = (TextView)view.findViewById(R.id.umeng_update_content);
            TextView bSure = (TextView)view.findViewById(R.id.umeng_update_id_ok);
            TextView bCancel = (TextView)view.findViewById(R.id.umeng_update_id_cancel);
            content.setText(m);
            bSure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mUpdateManager.showDownloadDialog();
                    dialog.dismiss();
                }
            });
            bCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            builder.setCancelable(false);
            dialog = builder.create();
            dialog.setView(view,0,0,0,0);
            dialog.show();

        }else if( v > Double.parseDouble(getVersionCode(context)) && force == 1){
            final UpdateManager mUpdateManager = new UpdateManager(context, url);
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.update_dialog, null);
            TextView content = (TextView)view.findViewById(R.id.umeng_update_content);
            TextView bSure = (TextView)view.findViewById(R.id.umeng_update_id_ok);
            LinearLayout llDividerView = (LinearLayout)view.findViewById(R.id.llDividerView);
            LinearLayout llNoUpdate = (LinearLayout)view.findViewById(R.id.llNoUpdate);
            content.setText(m);
            builder.setView(view);
            bSure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mUpdateManager.showDownloadDialog();
                    dialog.dismiss();
                }
            });
            llDividerView.setVisibility(View.GONE);
            llNoUpdate.setVisibility(View.GONE);
            builder.setCancelable(false);
            dialog = builder.create();
            dialog.setView(view,0,0,0,0);
            dialog.show();

        }else{
            if(sign==1){
                Toast.makeText(context,"已经是最新版本！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @Title: getVersionName
     * @Description: 获得 文件版本信息
     * @author yg
     * @date 2016-07-14 下午9:45:31
     * @param @return    设定文件
     * @return String    返回类型
     * @throws
     */
    @SuppressWarnings("unused")
    private static String getVersionName(Context context) {
        try {
            String pkName = context.getPackageName();
            String versionName = context.getPackageManager().getPackageInfo(pkName,0).versionName;
            return versionName;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    /**
     * @Title: getVersionName
     * @Description: 获得 文件版本信息
     * @author yg
     * @date 2016-07-14 下午9:45:31
     * @param @return    设定文件
     * @return String    返回类型
     * @throws
     */
    private static String getVersionCode(Context context) {
        try {
            String pkName = context.getPackageName();
            String versionCode = String.valueOf(context.getPackageManager().getPackageInfo(pkName,0).versionCode);
            return versionCode;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    /**
     * @Title: GetInfoHttpPost
     * @Description: 模拟POST获得返回数据通用接口
     * @author yg
     * @date 2016-7-20 下午2:06:08
     * @param @param url
     * @param @param pair
     * @param @return    设定文件
     * @return String    返回类型
     * @throws
     */
    private static String GetInfoHttpPost(String url, NameValuePair[] pair) {

        String returnStatus = null;
        PostMethod filePost = new PostMethod(url);
        filePost.setRequestBody(pair);
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        try {
            client.executeMethod(filePost);						// 执行连通方法
            returnStatus = filePost.getResponseBodyAsString();	// 获得服务端的返回值
            filePost.releaseConnection(); 						// 释放连接
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnStatus;
    }

}
