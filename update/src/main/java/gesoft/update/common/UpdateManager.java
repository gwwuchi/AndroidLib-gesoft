package gesoft.update.common;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import gesoft.update.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("HandlerLeak")
public class UpdateManager {
	/* 下载中 */
	private static final int DOWNLOAD = 1;
	/* 下载结束 */
	private static final int DOWNLOAD_FINISH = 2;
	/* 下载保存路径 */
	private String mSavePath;
	/* 记录进度条数量 */
	private int progress;
	/* 是否取消更新 */
	private boolean cancelUpdate = false;

	private Context mContext;
	/* 更新进度条 */
	private ProgressBar mProgress;
	@SuppressWarnings("unused")
	private Dialog mDownloadDialog;
	private AlertDialog dialog;
	
	private String mServerUpdateXML = "";
	
	private NotificationManager nm;
	
	private Handler mHandler = new Handler(){
		@SuppressLint("NewApi")
		@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		public void handleMessage(Message msg){
			switch (msg.what){
			// 正在下载
			case DOWNLOAD:
				// 设置进度条位置
				mProgress.setProgress(progress);
				break;
			case DOWNLOAD_FINISH:
				//nm.cancel(1);
				// 安装文件
				installApk();
				break;
			default:
				break;
			}
		};
	};

	public UpdateManager(Context context, String strServerUpdateXML){
		this.mContext = context;
		this.mServerUpdateXML = strServerUpdateXML;
		nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * 显示软件下载对话框
	 */
	public void  showDownloadDialog(){
		
		final Builder builder = new Builder(mContext);
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = inflater.inflate(R.layout.download_dialog, null);
		mProgress = (ProgressBar) view.findViewById(R.id.update_progress);
		TextView bCancel = (TextView)view.findViewById(R.id.umeng_update_id_cancel);
		bCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
				// 设置取消状态
				cancelUpdate = true;
			}
		});
		builder.setCancelable(false);
		dialog = builder.create();
		dialog.setView(view,0,0,0,0);
		dialog.show();
		downloadApk();
	}

	/**
	 * 下载apk文件
	 */
	private void downloadApk(){
		// 启动新线程下载软件
		new downloadApkThread().start();
	}

	/**
	 * 下载文件线程
	 * 
	 * @author coolszy
	 *@date 2012-4-26
	 *@blog http://blog.92coding.com
	 */
	private class downloadApkThread extends Thread {
		@Override
		public void run(){
			try{
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					// 获得存储卡的路径
					String sdpath = Environment.getExternalStorageDirectory() + "/";
					mSavePath = sdpath + mContext.getPackageName();
					URL url = new URL(mServerUpdateXML);
					// 创建连接
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.connect();
					// 获取文件大小
					int length = conn.getContentLength();
					// 创建输入流
					InputStream is = conn.getInputStream();

					File file = new File(mSavePath);
					// 判断文件目录是否存在
					if (!file.exists()){
						file.mkdir();
					}
					File apkFile = new File(mSavePath, mContext.getPackageName()+".apk");
					FileOutputStream fos = new FileOutputStream(apkFile);
					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					do{
						int numread = is.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) (((float) count / length) * 100);
						Log.e(">>>>>>>>>>>>>>>", progress + "");
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0){
							// 下载完成
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						// 写入文件
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					fos.close();
					is.close();
				}
			} catch (MalformedURLException e){
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	};

	/**
	 * 安装APK文件
	 */
	private void installApk(){
		File apkfile = new File(mSavePath, mContext.getPackageName()+".apk");
		if (!apkfile.exists()){
			return;
		}
		// 通过Intent安装APK文件
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		mContext.startActivity(i);
		Process.killProcess(Process.myPid());
	}
	
}
