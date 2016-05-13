package jp.ac.ynu.tommylab.ecolog.drivingloggerml.autocontrolgdl;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 電源の切断・接続イベントを検知するBroadcastReceiver
 * @author 1.0 kouno作成<br>
 *         2.0 hagimoto作成 LoggingSystemManagerでロギングの管理を行うよう変更したため電源イベントを検知のみを行う<br>
 * @version 2.0 
 */
public class AutoControlGDL extends BroadcastReceiver {

	/**
	 * 電源の接続イベントが発生したときに呼び出されるメソッド
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		if(intent == null){
			//lLog.writeActionLog("intentがnullです");
			return;
		}

		try
		{

			//発生した電源接続イベントをLoggingSystemManagerの変数として組み込む。
			if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
				Intent manegerintent = new Intent(context, LoggingSystemManager.class);
				manegerintent.putExtra(Key.KEY_POWER, "ON");

				context.startService(manegerintent);
			}else if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
				Intent manegerintent = new Intent(context, LoggingSystemManager.class);
				manegerintent.putExtra(Key.KEY_POWER, "OFF");

				context.startService(manegerintent);

			}
		}catch (Exception e)
		{
//			lLog.writeActionLog("電源関係で予期せぬエラーが発生しました");
//			lLog.writeActionLog(e.getMessage());
//			lLog.writeActionLog(e.getStackTrace().toString());
		}
	}

	
//1.0のバージョン	
//	//---------------------------------------------------------
//		// public void onReceive(Context context, Intent intent)
//		// 電源の接続イベントが発生したときに呼び出されるメソッド
//		//---------------------------------------------------------
//		public void onReceive(Context context, Intent intent) {
//
//			DeviceInfo dInfo;
//			int count = 0;
//			boolean flag = false;
//
//			//電源接続ログ書き込みオブジェクト作成
//			DirectoryTree.DIRECTORY_APPLOG.mkdirs();
//			LoggingLog lLog = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, DirectoryTree.FILENAME_AUTOCONTROLGDLLOG, true);
//
//
//			if(intent == null){
//				lLog.writeActionLog("intentがnullです");
//				return;
//			}
//
//			do
//			{
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//
//				}
//				dInfo = AccessDrivingLoggerStateFile.readStateFile();
//				if(dInfo.checkDeviceInfo() == true){
//					flag = true;
//				}
//				count++;
//			}while(flag == false && count < 5);
//
//			if(flag == false)
//			{
//				return;
//			}
//
//			try
//			{
//				//自動ロギング機能を利用しない場合は何もせずリターンする
//				if(!dInfo.isAutoControlGDL())
//				{
//					lLog.writeActionLog("自動ロギング機能を利用していないため");
//					return;
//				}
//
//				//発生した電源接続イベントごとに動作を変える。
//				//電源に接続された場合は、GetDrivingLogクラスを起動
//				//電源から切断された場合は、GetDrivingLogクラスを終了する
//				if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
//					lLog.writeActionLog("電源接続検知");
//
//					//すでにロギングが開始されている場合は何もしない
//					if(dInfo.getState().equals(DeviceInfo.waitLogging)){
//						dInfo.setState(DeviceInfo.underLogging);
//						lLog.writeActionLog("電源供給が再開されました");
//					}
//					else if(dInfo.getState().equals(DeviceInfo.noLogging)){
//						Intent start = new Intent(context, GetDrivingLog.class);
//						long startTime = System.currentTimeMillis();
//						start.putExtra(Key.KEY_START_TIME, startTime);
//
//						context.startService(start);
//						dInfo.setState(DeviceInfo.underLogging);
//					}else
//						return;
//
//					context.sendBroadcast(setLayoutIntent);
//				}else if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
//
//					lLog.writeActionLog("電源切断検知");
//					if(dInfo.getState().equals(DeviceInfo.underLogging)){
//						lLog.writeActionLog("切断待ちスレッドを生成します");
//						controlThread = new LoggingControlThread(lLog,dInfo,context);
//						controlThread.start();
//					}
//					else
//					{
//						lLog.writeActionLog("すでにロギングが終了しています");
//						return;
//					}
//
//				}
//			}catch (Exception e)
//			{
//				lLog.writeActionLog("電源関係で予期せぬエラーが発生しました");
//				lLog.writeActionLog(e.getMessage());
//				lLog.writeActionLog(e.getStackTrace().toString());
//			}
//			//context.sendBroadcast(setLayoutIntent);
//		}
}
