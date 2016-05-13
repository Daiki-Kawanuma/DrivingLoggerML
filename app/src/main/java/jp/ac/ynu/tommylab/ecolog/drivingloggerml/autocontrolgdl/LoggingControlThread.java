package jp.ac.ynu.tommylab.ecolog.drivingloggerml.autocontrolgdl;

import android.content.Context;
import android.content.Intent;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Action;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog.GetDrivingLog;

/**
 * 電源供給が不安定な時に発生するチャタリングによる接続・切断の繰り返しを防ぐため<br>
 * 電源切断から一定時間はログを取り続け、その後ロギングサービスを停止させるためのスレッド<br>
 * もし、一定時間内に電源接続がされた場合はロギング続行する
 * @author 1.0 hagimoto作成 <br>
 * @version 1.0
 */
public class LoggingControlThread extends Thread{

	private LoggingLog lLog;
	private Context context;
	private DeviceInfo dInfo;
	static final Intent setLayout = new Intent(Action.ACTION_SET_LAYOUT);

	/**
	 * コンストラクタ
	 * @param lLog ログ記述用オブジェクト
	 * @param dInfo アプリケーションのパラメータを管理するオブジェクト
	 * @param context
	 */
	public LoggingControlThread(LoggingLog lLog,DeviceInfo dInfo,Context context)
	{
		this.lLog = lLog;
		this.dInfo = dInfo;
		this.context = context;
	}

	@Override
	public void run()
	{
		try
		{
			dInfo.setState(DeviceInfo.waitLogging);
			context.sendBroadcast(setLayout);

			long tempTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - tempTime <= 60 * 1000)
			{

				//適度なスリープを入れ負荷を減らす
				sleep(10);

				//電源が接続された場合はロギング継続
				if(dInfo.getState().equals(DeviceInfo.underLogging))
				{
					lLog.writeActionLog("1分以内に電源接続されたのでロギングを継続します。");
					return;
				}
			}
			
			//1分間電源が接続されなかったときはロギング停止処理を行う
			lLog.writeActionLog("1分経過したのでロギングを停止します。");

			Intent finish = new Intent(context, GetDrivingLog.class);
			long startTime = System.currentTimeMillis();
			finish.putExtra(Key.KEY_START_TIME, startTime);

			//GetDrivingLogを停止させる
			context.stopService(finish);

			dInfo.setState(DeviceInfo.noLogging);
			context.sendBroadcast(setLayout);
		}
		catch(Exception e)//未確認のエラー検知用の例外処理
		{
			lLog.writeActionLog("LoggingControlThread(Exception):現在対応していないエラーが検出されました。");
			lLog.writeActionLog(e.getMessage());
			lLog.writeActionLog(e.getCause().toString());
			lLog.writeActionLog(e.getStackTrace().toString());
		}
	}
}