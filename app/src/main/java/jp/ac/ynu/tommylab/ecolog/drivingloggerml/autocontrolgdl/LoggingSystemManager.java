package jp.ac.ynu.tommylab.ecolog.drivingloggerml.autocontrolgdl;

import java.util.ArrayList;
import java.util.List;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Action;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog.GetDrivingLog;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 電源接続・切断時に呼び出され、その時の状態によりロギングサービスを起動・停止準備・リスタートの管理するサービス
 * @author 1.0 hagimoto作成
 * @version 1.0
 */
public class LoggingSystemManager extends Service {

	DeviceInfo dInfo;
	LoggingLog lLog;
	static final Intent setLayoutIntent = new Intent(Action.ACTION_SET_LAYOUT);
	private LoggingControlThread controlThread = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	/**
	 * 生成時に呼び出される(何もしない)
	 */
	public void onCreate() {

	}

	@Override
	/**
	 * サービスがスタートされた時に呼び出され、ロギングサービスの管理を行う
	 */
	public int onStartCommand (Intent intent, int flags, int startId) {

		//電源接続ログ書き込みオブジェクト作成
		DirectoryTree.DIRECTORY_APPLOG.mkdirs();

		String power = intent.getExtras().getString(Key.KEY_POWER);
		lLog = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, DirectoryTree.FILENAME_AUTOCONTROLGDLLOG, true);
		dInfo = (DeviceInfo)this.getApplication();

		//自動ロギング機能を利用しない場合は何もせずリターンする
		if(!dInfo.isAutoControlGDL())
		{
			lLog.writeActionLog("電源状態の変化を検知しましたが自動ロギング設定がされてません");
			stopSelf();

			return START_STICKY_COMPATIBILITY;
		}

		ArrayList<String> serviceList = new ArrayList<String>();
		ActivityManager activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);

		List<RunningServiceInfo> runningService = activityManager.getRunningServices(100);
		if(runningService != null) {
			for(RunningServiceInfo srvInfo : runningService) {
				serviceList.add(srvInfo.service.getShortClassName());
			}
		}

		if(power.equals("ON")){
			lLog.writeActionLog("電源接続検知");

			if(dInfo.getState().equals(DeviceInfo.underLogging)){
				long startTime;

				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("GetDrivingLogサービスは起動中です");
					lLog.writeActionLog("異常終了の可能性があるためロギングサービスをリスタートします");

					Intent finish = new Intent(this, GetDrivingLog.class);
					startTime = System.currentTimeMillis();
					finish.putExtra(Key.KEY_START_TIME, startTime);
					this.stopService(finish);
					dInfo.setState(DeviceInfo.noLogging);
				}
				else{
					lLog.writeActionLog("GetDrivingLogサービスは起動していません");
					lLog.writeActionLog("ロギングサービスを開始します");
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Intent gdlIntent = new Intent(this, GetDrivingLog.class);
				startTime = System.currentTimeMillis();
				gdlIntent.putExtra(Key.KEY_START_TIME, startTime);
				this.startService(gdlIntent);
				dInfo.setState(DeviceInfo.underLogging);
			}else if(dInfo.getState().equals(DeviceInfo.waitLogging)){
				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("ロギング停止待ち状態です ロギングサービスを継続します");
					dInfo.setState(DeviceInfo.underLogging);
				}
				else{
					lLog.writeActionLog("ロギング停止待ち状態ですがGetDrivingLogサービスが起動していません");
					lLog.writeActionLog("異常終了の可能性があるためロギングサービスを起動します");

					Intent gdlIntent = new Intent(this, GetDrivingLog.class);
					long startTime = System.currentTimeMillis();
					gdlIntent.putExtra(Key.KEY_START_TIME, startTime);
					this.startService(gdlIntent);
					dInfo.setState(DeviceInfo.underLogging);
				}


			}else if(dInfo.getState().equals(DeviceInfo.noLogging)){
				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("ロギング停止状態でがロギングサービスが起動しています");
					lLog.writeActionLog("異常終了の可能性があるためロギングサービスをリスタートします");

					Intent finish = new Intent(this, GetDrivingLog.class);
					long startTime = System.currentTimeMillis();
					finish.putExtra(Key.KEY_START_TIME, startTime);
					this.stopService(finish);
					dInfo.setState(DeviceInfo.noLogging);
				}
				else{
					lLog.writeActionLog("ロギング停止状態です ロギングサービスを開始します");
					Intent gdlIntent = new Intent(this, GetDrivingLog.class);
					long startTime = System.currentTimeMillis();
					gdlIntent.putExtra(Key.KEY_START_TIME, startTime);
					this.startService(gdlIntent);
					dInfo.setState(DeviceInfo.underLogging);
				}

			}
		}
		else if(power.equals("OFF")){
			lLog.writeActionLog("電源切断検知");

			if(dInfo.getState().equals(DeviceInfo.underLogging)){
				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("ロギング中です 切断待ちスレッドを生成します");
					//waitForPower();
					controlThread = new LoggingControlThread(lLog,dInfo,this);
					controlThread.start();
				}
				else{
					lLog.writeActionLog("ロギング中ですがGetDrivingLogサービスは起動していません");
					lLog.writeActionLog("停止状態に移行します");
					dInfo.setState(DeviceInfo.noLogging);
				}

			}else if(dInfo.getState().equals(DeviceInfo.waitLogging)){
				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("ロギング停止待ち状態です");
				}
				else{
					lLog.writeActionLog("ロギング停止待ち状態ですがGetDrivingLogサービスは起動していません");
					lLog.writeActionLog("停止状態に移行します");
				}

			}else if(dInfo.getState().equals(DeviceInfo.noLogging)){
				if(serviceList.contains(".getdrivinglog.GetDrivingLog")){
					lLog.writeActionLog("ロギング停止状態ですがGetDrivingLogサービスが起動しています");
					lLog.writeActionLog("GetDrivingLogサービスを停止させます");

					Intent finish = new Intent(this, GetDrivingLog.class);
					long startTime = System.currentTimeMillis();
					finish.putExtra(Key.KEY_START_TIME, startTime);
					this.stopService(finish);
				}
				else{
					lLog.writeActionLog("ロギング停止状態です");
				}

			}
		}
		this.sendBroadcast(setLayoutIntent);
		stopSelf();

		return START_STICKY_COMPATIBILITY;
	}

	/**
	 * サービスが破棄されたときに呼び出される(実際には呼び出されない)
	 */
	@Override
	public void onDestroy(){
		//lLog.writeActionLog("LoggingSystemManagerが破棄されました");
	}
}
