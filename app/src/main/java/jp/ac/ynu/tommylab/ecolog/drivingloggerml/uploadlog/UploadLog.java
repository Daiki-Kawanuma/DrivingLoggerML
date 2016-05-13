package jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.AccessLastLocationFile;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.Coords;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Action;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.ComponentName;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * アップロードをリトライするときのデータを管理する構造体のクラス
 * @author kouno
 * @version 1.0
 */
final class AlarmInfo {
	private int retryTime;
	private boolean isReserved;

	public AlarmInfo(int retryTime, boolean isReserved){
		this.retryTime = retryTime;
		this.isReserved = isReserved;
	}

	public int getRetryTime(){
		return retryTime;
	}

	public boolean isReserved(){
		return isReserved;
	}
}

/**
 * ログファイルのアップロードするためのIntentServiceを提供するクラス
 * @author 1.0 kouno作成<br>
 * 		   1.1 kouno,hagimoto修正 リトライ回数に制限、アップロードする地点に制限、アップロード監査アルゴリズムの見直し<br>
 * 		   1.2 hagimoto修正 サーバ接続確認の手順の変更、内部温度によるアップロードの制限<br>
 * 		   1.3 hagimoto修正 バッテリー残量によるアップロード制限,ログ書き込み箇所の追加<br>
 * 		   1.4 hagimoto修正 電池残量が少量でも手動アップロードは可能に変更<br>
 * @version 1.4
 */
public class UploadLog extends IntentService {
	//サーバURL
	public static final String URLPart = "smb://tommylab.ynu.ac.jp;demo:q1q1Q!Q!@133.34.154.116/ECOLOG_LogData_itsserver";
	//public static final String URLPart = "smb://133.34.154.22;kanri:4r4r$R$R@133.34.154.116/ECOLOG_LogData_itsserver";
	//public static final String URLPart = "smb://133.34.154.22;demo:q1q1Q!Q!@133.34.154.116/ECOLOG_LogData_itsserver";
	//public static final String URLPart = "smb://133.34.154.22;administrator:q1q1Q!Q!@133.34.154.116/ECOLOG_LogData_itsserver";

	//エラーメッセージ
	public static final String ERROR_DIRECTORY_ALREADY_EXISTS = "Cannot create a file when that file already exists.";
	public static final String ERROR_FAILED_CONNECTION = "Failed to connect";
	//tommyairstationSSID
	public static final String labAccessPointSSID = "tommylabairstation";//"tommyairstation";
	//総合研究棟の緯度経度
	public static final double GRBLatitude = 35.472233;
	public static final double GRBLongitude = 139.586715;
	//接続確認を行う時間
	public static final long checkConnectionTime = 5 * 60 * 1000;
	//ログ取得オブジェクト
	//protected LoggingLog ULLogWriter;
	//未送信ログファイルリスト
	protected ArrayList<FileAndLength> unsentLogList;
	protected ArrayList<FileAndLength> tempLoggingList;
	protected ArrayList<FileAndLength> appLogList;
	//ITSサーバ上のディレクトリのパス
	protected String DirectoryPath_UserAndCarID;
	protected String serverDirectoryPath_UnsentLog;
	protected String serverDirectoryPath_TempLogging;
	protected String serverDirectoryPath_AppLog;
	//ITSサーバ上のディレクトリ
	protected SmbFile serverDirectory_UnsentLog;
	protected SmbFile serverDirectory_TempLogging;
	protected SmbFile serverDirectory_AppLog;
	//エラーフラグ
	protected int errorNumber;
	protected static final int ERROR_NOTHING = 0;
	protected static final int ERROR_NO_CONNECTION = 1;
	protected static final int ERROR_TIME_OUT = 2;
	protected static final int ERROR_COMPARISON_STAGNATION = 3;
	//危険バッテリー温度
	private static final int TEMPERATURE_LIMIT = 450;

	public UploadLog(String name) {
		super(name);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public UploadLog() {
		super(ComponentName.UploadLog);
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		//ログアップロード時間
		long startTime = System.currentTimeMillis();
		String startTimeString = TimeStamp.getTimeString(startTime);

		//共有オブジェクトの初期化
		//ログライタ
		LoggingLog ULLogWriter = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, startTimeString + ComponentName.UploadLog + ".txt", true);
		DirectoryTree.DIRECTORY_APPLOG.mkdirs();

		//未送信ファイル一覧
		//unsentLogList = new ArrayList<FileAndLength>();
		//tempLoggingList = new ArrayList<FileAndLength>();
		//appLogList = new ArrayList<FileAndLength>();

		//サーバ上ディレクトリパス
		DirectoryPath_UserAndCarID = null;
		serverDirectoryPath_UnsentLog= null;
		serverDirectoryPath_TempLogging = null;
		serverDirectoryPath_AppLog = null;

		//ITSサーバ上のディレクトリ
		serverDirectory_UnsentLog = null;
		serverDirectory_TempLogging = null;
		serverDirectory_AppLog = null;

		//エラーフラグ
		errorNumber = ERROR_NOTHING;

		DeviceInfo dInfo = (DeviceInfo)this.getApplication();
//		DeviceInfo dInfo = AccessDrivingLoggerStateFile.readStateFile();
		String userID = dInfo.getUserID();
		String carID = dInfo.getCarID();
		String sensorID = dInfo.getSensorID();

		String process = arg0.getExtras().getString("process");

		if(process.equals("auto"))
		{
			ULLogWriter.writeActionLog("[オートアップロード]");
		}
		else if (process.equals("manual"))
		{
			ULLogWriter.writeActionLog("[手動アップロード]");
		}
		else if (process.equals("retry"))
		{
			ULLogWriter.writeActionLog("[リトライアップロード]");
		}

		//アラームキャンセル
		cancelNextUpload(ULLogWriter);

		//バッテリー情報を取得
		int temperature = getBattery(ULLogWriter);

		if(dInfo.getState().equals(DeviceInfo.underLogging) || dInfo.getState().equals(DeviceInfo.waitLogging)){
			ULLogWriter.writeActionLog("ロギング中のためアップロードを中止します");
			ULLogWriter.closeWriter();
			return;
		}

		broadcastMessage("アップロード開始");

		if(temperature > TEMPERATURE_LIMIT)
		{
			ULLogWriter.writeActionLog("バッテリーの温度が高温なためアップロードを中止します");
			ULLogWriter.closeWriter();

			broadcastMessage("バッテリーの温度が高温なためアップロードを中止します");
			dInfo.setUploadState(DeviceInfo.UPLOAD_HIGH_HEATED_BATTERY);
			dInfo.setTemperature(temperature);
			return;
		}

		int batteryPower = getBatteryPower();

		if(batteryPower < 20 && !(process.equals("manual")))
		{
			ULLogWriter.writeActionLog("バッテリーの残量が少量なためアップロードを中止します");
			ULLogWriter.closeWriter();

			broadcastMessage("バッテリーの残量が少量なためアップロードを中止します");
			dInfo.setUploadState(DeviceInfo.UPLOAD_LOW_BATTERY);
			dInfo.setTemperature(temperature);
			return;
		}

		//現在地が総合研究棟かをチェック
		//違うならアップロードは行わない
		if(process.equals("auto") || process.equals("retry"))
		{
			Location lastLocation = AccessLastLocationFile.readLastLocationFile();
			double distance = Coords.calcDistHubeny(GRBLatitude, GRBLongitude, lastLocation.getLatitude(), lastLocation.getLongitude());
			ULLogWriter.writeActionLog("dist:" + distance + "\r\n");
			if(distance > 100)
			{
				ULLogWriter.writeActionLog("現在地が総合研究棟前の駐車場でないと判断されました。UploadLogコンポーネントを終了します");
				ULLogWriter.closeWriter();
				broadcastMessage("総合研究棟前の駐車場でないためアップロードはされませんでした");
				return;
			}
		}

		//強制画面ON
		PowerManager pManager = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wakeLock = pManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "wakeLockToUpload");
		//WakeLock wakeLock = pManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeLockToUpload");
		wakeLock.acquire();
		ULLogWriter.writeActionLog("強制画面ON\r\n");

		//接続チェック
		//if(checkConnection(ULLogWriter))
		//	ULLogWriter.writeActionLog("ITSサーバへの接続確認\r\n");

		if(checkConnection(ULLogWriter)){
			ULLogWriter.writeActionLog("ITSサーバへの接続確認\r\n");
			broadcastMessage("ネットワーク接続成功");
		}
		else
		{
			ULLogWriter.writeActionLog("ITSサーバへの接続が確認できませんでした");
			makeErrorLog(startTimeString, ERROR_NO_CONNECTION);

			//バッテリー情報を取得
			temperature = getBattery(ULLogWriter);
			broadcastMessage("ネットワーク接続に失敗しました");
			dInfo.setUploadState(DeviceInfo.UPLOAD_NETWORK_ERROR);
			dInfo.setTemperature(temperature);

			if(temperature > TEMPERATURE_LIMIT)
			{
				ULLogWriter.writeActionLog("バッテリーの温度が高温なためアップロードを中止します");
			}
			else
			{
				reserveNextUpload(ULLogWriter,process);
			}

			ULLogWriter.writeActionLog("強制画面ON解除\r\n");
			ULLogWriter.closeWriter();
			wakeLock.release();

			return;
		}


		//アップロード先のフォルダインスタンスを作成する
		DirectoryPath_UserAndCarID = URLPart + "/" + userID + "/" + carID + "/" + sensorID;
		serverDirectoryPath_UnsentLog = DirectoryPath_UserAndCarID + "/" + DirectoryTree.DIRECTORYNAME_UNSENTLOG;
		serverDirectoryPath_TempLogging = DirectoryPath_UserAndCarID + "/" + DirectoryTree.DIRECTORYNAME_TEMPLOGGING;
		serverDirectoryPath_AppLog = DirectoryPath_UserAndCarID + "/" + DirectoryTree.DIRECTORYNAME_APPLOG;

		try {
			serverDirectory_UnsentLog = new SmbFile(serverDirectoryPath_UnsentLog);
			serverDirectory_TempLogging = new SmbFile(serverDirectoryPath_TempLogging);
			serverDirectory_AppLog = new SmbFile(serverDirectoryPath_AppLog);
			ULLogWriter.writeActionLog("ログアップロード先フォルダのインスタンスを作成しました");
		} catch (MalformedURLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			ULLogWriter.writeActionLog("サーバURLの書式が間違っています");
			ULLogWriter.writeActionLog("強制画面ON解除\r\n");
			ULLogWriter.closeWriter();
			wakeLock.release();

			return;
		}

		//送信すべきファイルのファイルオブジェクトを取得する
		unsentLogList = listupFile(DirectoryTree.DIRECTORY_UNSENTLOG, ULLogWriter);
		tempLoggingList = listupFile(DirectoryTree.DIRECTORY_TEMPLOGGING, ULLogWriter);
		appLogList = listupFile(DirectoryTree.DIRECTORY_APPLOG, ULLogWriter);

		try{
			//はじめにアップロード予定のファイルを送信
			String s = "アップロード予定ファイル\r\n";
			s += makeUploadFileList(unsentLogList);
			s += makeUploadFileList(tempLoggingList);
			s += makeUploadFileList(appLogList);
			uploadFileList(serverDirectory_AppLog, s,startTimeString, ULLogWriter);
			
			
			//ログファイルを送信する
			uploadFile(serverDirectory_UnsentLog, unsentLogList, ULLogWriter);
			uploadFile(serverDirectory_TempLogging, tempLoggingList, ULLogWriter);
			uploadFile(serverDirectory_AppLog, appLogList, ULLogWriter);
		}catch(Exception e){
			ULLogWriter.writeActionLog("ITSサーバに接続できませんでした");
			//バッテリー情報を取得
			getBattery(ULLogWriter);
			ULLogWriter.writeActionLog("強制画面ON解除\r\n");
			ULLogWriter.closeWriter();
			reserveNextUpload(ULLogWriter,process);
			wakeLock.release();

			return;
		}

		broadcastMessage("ログファイルを送信しました\r\n送信状況に監視に入ります");

		ULLogWriter.writeActionLog("アップロード状態の監視を開始します");

		//照合スレッド・監視スレッドを開始する
		CompareLogFileThread cLogFileThread = new CompareLogFileThread(this, ULLogWriter);
		WatchComparisonThread wcThread = new WatchComparisonThread(this, ULLogWriter);
		cLogFileThread.setWatchThread(wcThread);
		wcThread.setComparisonThread(cLogFileThread);
		cLogFileThread.start();
		wcThread.start();

		try {
			long tempTime = System.currentTimeMillis();
			cLogFileThread.join(15 * 60 * 1000);
			if(System.currentTimeMillis() - tempTime >= 15 * 60 * 1000){
				ULLogWriter.writeActionLog("タイムアウトしました");
				ULLogWriter.writeActionLog("スレッドを強制終了させます");
				broadcastMessage("15分経過したのでタイムアップしました");

				cLogFileThread.interrupt();
				wcThread.interrupt();
				errorNumber = ERROR_TIME_OUT;
			}
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		if(errorNumber != 0){
			makeErrorLog(startTimeString, errorNumber);
			reserveNextUpload(ULLogWriter,process);

			//バッテリー情報を取得
			getBattery(ULLogWriter);
			broadcastMessage("アップロードに失敗しました");
			dInfo.setUploadState(DeviceInfo.UPLOAD_FILE_SEND_ERROR);
			dInfo.setTemperature(temperature);
		}
		else
		{
			ULLogWriter.writeActionLog("アップロードは正常に終了しました");

			AccessLastLocationFile.resetLocationFile();

			//バッテリー情報を取得
			getBattery(ULLogWriter);
			broadcastMessage("アップロードの正常終了を確認しました");
			dInfo.setUploadState(DeviceInfo.UPLOAD_COMPLETED);
			dInfo.setTemperature(temperature);
		}

		ULLogWriter.writeActionLog("Uploadlogコンポーネントの終了");
		ULLogWriter.writeActionLog("強制画面ON解除\r\n");
		ULLogWriter.closeWriter();
		wakeLock.release();

	}

	/**
	 * アップロード状態を表示するメッセージをDrivingLoggerUIにBroadcastする
	 * @param str アップロード状況を表す文字列
	 */
	private void broadcastMessage(String str) {
		Intent broadcastIntent = new Intent(Action.ACTION_UPLOADVIEW);
		broadcastIntent.putExtra("message",str);

		String temperature = getTemparature()/10 + "℃";
		broadcastIntent.putExtra("temperature",temperature);
		getBaseContext().sendBroadcast(broadcastIntent);
	}

	/**
	 * バッテリの状態を取得しログに書き込むメソッド
	 * @param ULLogWriter ログ書き込み用オブジェクト
	 * @return 端末の内部温度
	 */
	private int getBattery(LoggingLog ULLogWriter) {
		// ACTION_BATTERY_CHANGEDを受け取るためのフィルタを生成
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);

		// 同期的に電池の残量を取得
		Intent battery = getApplicationContext().registerReceiver(null, filter);

		// 電池の残量を取得
		int level = battery.getIntExtra("level", 0);
		// ケーブルの接続状態を取得
		int plugged = battery.getIntExtra("plugged", 0);
		// バッテリの有無を取得
		boolean present = battery.getBooleanExtra("present", false);
		// 充電の状態を取得
		int status = battery.getIntExtra("status", 0);
		// 電池の温度を取得
		int temperature = battery.getIntExtra("temperature", 0);

		ULLogWriter.writeActionLog("電池の残量：" + level);

		switch (plugged)
		{
		case BatteryManager.BATTERY_PLUGGED_AC:
			ULLogWriter.writeActionLog("ケーブルの接続状態: AC");
			break;
		case BatteryManager.BATTERY_PLUGGED_USB:
			ULLogWriter.writeActionLog("ケーブルの接続状態: USB");
			break;
		}

		ULLogWriter.writeActionLog("電池の有無：" + present);

		switch (status)
		{
		case BatteryManager.BATTERY_STATUS_CHARGING:
			ULLogWriter.writeActionLog("充電の状態 : CHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_DISCHARGING:
			ULLogWriter.writeActionLog("充電の状態 : DISCHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_FULL:
			ULLogWriter.writeActionLog("充電の状態 : FULL");
			break;
		case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
			ULLogWriter.writeActionLog("充電の状態 : NOT CHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_UNKNOWN:
			ULLogWriter.writeActionLog("充電の状態 : UNKNOWN");
			break;
		}

		ULLogWriter.writeActionLog("電池の温度：" + (float)temperature + "\r\n");

		return temperature;
	}

	/**
	 * 電池の内部温度だけを取得するメソッド
	 * @return 端末の内部温度
	 */
	private int getTemparature() {
		// ACTION_BATTERY_CHANGEDを受け取るためのフィルタを生成
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);

		// 同期的に電池の残量を取得
		Intent battery = getApplicationContext().registerReceiver(null, filter);

		// 電池の温度を取得
		int temperature = battery.getIntExtra("temperature", 0);

		return temperature;
	}

	/**
	 * 端末のバッテリー残量を取得するメソッド
	 * @return 端末のバッテリー残量
	 */
	private int getBatteryPower() {
		// ACTION_BATTERY_CHANGEDを受け取るためのフィルタを生成
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);

		// 同期的に電池の残量を取得
		Intent battery = getApplicationContext().registerReceiver(null, filter);

		// 電池の残量を取得
		int level = battery.getIntExtra("level", 0);

		return level;
	}

//	private int getACState() {
//		// ACTION_BATTERY_CHANGEDを受け取るためのフィルタを生成
//		IntentFilter filter = new IntentFilter();
//		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
//
//		// 同期的に電池の残量を取得
//		Intent battery = getApplicationContext().registerReceiver(null, filter);
//
//		// 充電の状態を取得
//		int status = battery.getIntExtra("status", 0);
//
//		return status;
//	}

	/**
	 * 5分間、2分間隔でsingleCheckConnectionを呼び、ITSサーバとの接続が確立されているかをチェックする<br>
	 * @param lLog 
	 * @return ネットワーク接続成功ならtrue 失敗ならfalse
	 */
	private boolean checkConnection(LoggingLog lLog){
		long startConnectionCheckTime = System.currentTimeMillis();
		WifiManager wManager = (WifiManager) getSystemService(WIFI_SERVICE);
		try
		{
			while(!singleCheckConnection(lLog,wManager)){
				if(System.currentTimeMillis() - startConnectionCheckTime >= checkConnectionTime){
					lLog.writeActionLog("接続確認タイムアップ");
					return false;
				}
				wManager.disconnect();
				Thread.sleep(5 * 1000);
				wManager.reconnect();
				//wManager.reassociate();

				broadcastMessage("ネットワーク接続確認中");

				Thread.sleep(2 * 60 * 1000);
			}
		} catch (InterruptedException e) {
			lLog.writeActionLog("接続チェック(InterruptedException)：" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 端末がITSサーバにアクセスできるかをチェックする
	 * @param lLog ログ書き込み用オブジェクト
	 * @param wManager 利用するWifiManager
	 * @return ネットワーク接続成功ならtrue 失敗ならfalse
	 */
	private boolean singleCheckConnection(LoggingLog lLog,WifiManager wManager){
		try {
			SmbFile sFile = new SmbFile(URLPart);
			jcifs.Config.setProperty("jcifs.netbios.cachePolicy","0");
			jcifs.Config.setProperty("jcifs.smb.client.responseTimeout", "20000");
			jcifs.Config.setProperty("jcifs.netbios.baddr", "133.34.154.255 ");

			lLog.writeActionLog("接続チェック開始");
			broadcastMessage("ネットワーク接続確認中");
			WifiInfo wInfo = wManager.getConnectionInfo();
			DhcpInfo dhcpInfo = wManager.getDhcpInfo();

			SupplicantState state = wInfo.getSupplicantState();
			int ipint = wInfo.getIpAddress();

			lLog.writeActionLog("回線速度：" + wInfo.getLinkSpeed());
			lLog.writeActionLog("IPアドレス：" + makeIPaddress(ipint));
			lLog.writeActionLog("状態：" + state);
			lLog.writeActionLog("状態2：" + WifiInfo.getDetailedStateOf(state));

			lLog.writeActionLog("DNS1：" + makeIPaddress(dhcpInfo.dns1));
			lLog.writeActionLog("DNS2：" + makeIPaddress(dhcpInfo.dns2));
			lLog.writeActionLog("ゲートウェイ：" + makeIPaddress(dhcpInfo.gateway));
			lLog.writeActionLog("ネットマスク：" + makeIPaddress(dhcpInfo.netmask));
			lLog.writeActionLog("DHCPサーバアドレス：" + makeIPaddress(dhcpInfo.serverAddress));

			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo nInfo = cm.getActiveNetworkInfo();

			if(wInfo.getSSID() != null)
			{
				lLog.writeActionLog("接続先アクセスポイント：" + wInfo.getSSID());
			}
			else
			{
				lLog.writeActionLog("アクセスポイント未接続");
				lLog.writeActionLog("Wifiに接続されていません\r\n");
				broadcastMessage("Wifiに接続できませんでした");
				return false;
			}

			if(nInfo != null)
			{
				lLog.writeActionLog("isConnected：" + nInfo.isConnected());
			}
			else
			{
				lLog.writeActionLog("isConnected：null");
				lLog.writeActionLog("ネットワークに接続されていません\r\n");
				broadcastMessage("IPアドレスが取得できませんでした");
				return false;
			}

			if(nInfo.isConnected() == true)
			{
				lLog.writeActionLog(Integer.toString(WifiManager.calculateSignalLevel(wInfo.getRssi(), 5)));
				return sFile.exists();
			}
			else
			{
				return false;
			}
		} catch (MalformedURLException e) {
			lLog.writeActionLog("接続チェック(MalformedURLException)：" + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (SmbException e) {
			lLog.writeActionLog("接続チェック(SmbException)：" + e.getMessage());
			lLog.writeActionLog("接続チェック(SmbException)：" + e.getNtStatus());
			e.printStackTrace();
			return false;
		}
	}

	private String makeIPaddress(int ipint) {
		return ((ipint >> 0) & 0xFF) + "." + ((ipint >> 8) & 0xFF) + "." + ((ipint >> 16) & 0xFF) + "." + ((ipint >> 24) & 0xFF);
	}

	/**
	 * サーバーサイドのディレクトリを作成するメソッド
	 * @param file 作成するディレクトリ
	 * @param lLog ログ書き込み用オブジェクト
	 * @return 作成に成功or既に存在するならtrue 失敗ならfalse
	 */
	private boolean makeServerDirectory(SmbFile file, LoggingLog lLog){
		try {
			file.mkdirs();
			lLog.writeActionLog(file.getName() + "の作成に成功しました");
			//lLog.writeActionLog("ディレクトリ名：" + file.getName());
			return true;
		} catch (SmbException e) {
			if(e.getMessage().equals(ERROR_DIRECTORY_ALREADY_EXISTS)){
				lLog.writeActionLog("すでに" + file.getName() + "は作成されています");
				return true;
			}
			e.printStackTrace();
			lLog.writeActionLog(file.getName() + "の作成に失敗しました");
			return false;
		}
	}

	/**
	 * 引数のファイルオブジェクトで表されるディレクトリ直下のファイルオブジェクトをArrayList形式で返す
	 * @param parentDirectory ファイルオブジェクトを取得するディレクトリ
	 * @param lLog ログ書き込み用オブジェクト
	 * @return ファイルオブジェクトのArrayList
	 */
	private ArrayList<FileAndLength> listupFile(File parentDirectory, LoggingLog lLog){//修正
		File[] files = parentDirectory.listFiles();
		ArrayList<FileAndLength> fileList = new ArrayList<FileAndLength>();
		int index = 0;

		if(files != null){
			for(File f : files)
			{
				fileList.add(index,new FileAndLength(f, 0));
				index++;
			}

			lLog.writeActionLog(parentDirectory.getName() + "のファイル取得が完了しました");
		}else{
			lLog.writeActionLog("アップロードするファイルが存在しません。");
		}

		return fileList;
	}

	
	private String makeUploadFileList(ArrayList<FileAndLength> fileList)
	{
		String str = "";
		
		for(FileAndLength f :fileList){
			File file = f.file;
			str += file.getName() + "\r\n";
		}
		
		return str;
	}
	
	/**
	 * 引数のArrayListに格納されたファイルをサーバにアップロードする
	 * @param serverSideDirectory サーバにアップロードする対象ディレクトリ
	 * @param fileList アップロードするファイルリスト
	 * @param lLog ログ書き込み用オブジェクト
	 */
	private void uploadFileList(SmbFile serverSideDirectory, String str, String startTimeString, LoggingLog lLog){
		makeServerDirectory(serverSideDirectory, lLog);

		lLog.writeActionLog("アップロードファイルリストのアップロード");

		File directory = DirectoryTree.DIRECTORY_APPLOG;
		
		
		directory.mkdirs();

		File logFile = new File(directory, startTimeString + "UploadList.txt");
		try {
			logFile.createNewFile();
			BufferedWriter bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), "Shift_JIS"));
			
			bWriter.write(str);
			bWriter.flush();
			bWriter.close();
			
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
		try {
			FileInputStream iStream = new FileInputStream(logFile);
			SmbFile sFile = new SmbFile(serverSideDirectory.getPath() + "/" + logFile.getName());
			OutputStream oStream = sFile.getOutputStream();
			int response = 0;
			byte[] buffer = new byte[1024];

			lLog.writeActionLog("ファイルアップロード：" + logFile.getName());

			while((response = iStream.read(buffer, 0, buffer.length)) != -1){
					oStream.write(buffer, 0, response);
			}
			
			iStream.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			lLog.writeActionLog("アップロードに失敗しました。");
			lLog.writeActionLog("失敗したファイル名：" + logFile.getName());
			//throw new Exception();
		}
	}
	
	/**
	 * 引数のArrayListに格納されたファイルをサーバにアップロードする
	 * @param serverSideDirectory サーバにアップロードする対象ディレクトリ
	 * @param fileList アップロードするファイルリスト
	 * @param lLog ログ書き込み用オブジェクト
	 */
	private void uploadFile(SmbFile serverSideDirectory, ArrayList<FileAndLength> fileList, LoggingLog lLog){
		makeServerDirectory(serverSideDirectory, lLog);

		lLog.writeActionLog(serverSideDirectory.getName() + "のファイルアップロード");

		for(FileAndLength f :fileList){
			File file = f.file;
			try {
				FileInputStream iStream = new FileInputStream(file);
				SmbFile sFile = new SmbFile(serverSideDirectory.getPath() + "/" + file.getName());
				OutputStream oStream = sFile.getOutputStream();
				int response = 0;
				byte[] buffer = new byte[1024];

				lLog.writeActionLog("ファイルアップロード：" + file.getName());

				while((response = iStream.read(buffer, 0, buffer.length)) != -1){
						oStream.write(buffer, 0, response);
				}
				
				iStream.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				lLog.writeActionLog("アップロードに失敗しました。");
				lLog.writeActionLog("失敗したファイル名：" + file.getName());
				//throw new Exception();
			}
		}
	}

	/**
	 * アップロード失敗に関するエラーログを出力する
	 * @param startTimeString サービスを開始した時刻
	 * @param errorNumber エラーコード
	 */
	private void makeErrorLog(String startTimeString, int errorNumber){
		LoggingLog lLog = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG,
																	startTimeString + "UploadErrorLog.txt", true);

		switch(errorNumber){
			case ERROR_NO_CONNECTION:
				lLog.writeActionLog("接続エラーによりアップロードは異常終了しました");
				break;
			case ERROR_TIME_OUT:
				lLog.writeActionLog("アップロードはタイムアウトしました");
				break;
			case ERROR_COMPARISON_STAGNATION:
				lLog.writeActionLog("照合スレッドの異常停止によりアップロードは異常終了しました");
				break;
		}
	}

	/**
	 * AlarmManagerに次回のアップロード予約を登録する
	 * @param ULLogWriter ログ書き込み用オブジェクト
	 * @param process このサービスが呼び出され方(自動アップロード、手動アップロード)
	 */
	private void reserveNextUpload(LoggingLog ULLogWriter,String process){
		AlarmInfo aInfo = readRetryTimeFile(ULLogWriter);
		int retryCount = aInfo.getRetryTime();

		if(retryCount < 10){
			//15分毎にUploadLogを呼び出すようにAlarmManagerに登録

			//ULLogWriter.writeActionLog("オートアップロードの設定開始 リトライ回数:" + retryCount);

			Intent uploadLogIntent = new Intent(this, ReceiverToStartUploadLog.class);
			PendingIntent uploadLogPendingIntent = PendingIntent.getBroadcast(this, 0, uploadLogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60 * 1000, uploadLogPendingIntent);

			if(process.equals("manual"))
			{
				writeRetryTimeFile(new AlarmInfo(retryCount, true),ULLogWriter);
			}
			else
			{
				writeRetryTimeFile(new AlarmInfo(retryCount + 1, true),ULLogWriter);
			}
			ULLogWriter.writeActionLog("オートアップロードのアラームを設定しました(リトライ回数:" + retryCount + ")");
		}else{
			ULLogWriter.writeActionLog("リトライ回数が10回を超えたのでオートアップロードを停止します");
			writeRetryTimeFile(new AlarmInfo(0, false),ULLogWriter);
		}
	}

	/**
	 * reserveNextUpload()で予約したアラームを取り消す
	 * @param ULLogWriter ログ書き込み用オブジェクト
	 */
	private void cancelNextUpload(LoggingLog ULLogWriter){
		//ULLogWriter.writeActionLog("オートアップロードの取り消し開始");

		Intent uploadLogIntent = new Intent(this, ReceiverToStartUploadLog.class);
		PendingIntent uploadLogPendingIntent = PendingIntent.getBroadcast(this, 0, uploadLogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.cancel(uploadLogPendingIntent);

		AlarmInfo aInfo = readRetryTimeFile(ULLogWriter);
		if(aInfo.isReserved()){
			writeRetryTimeFile(new AlarmInfo(aInfo.getRetryTime(), false),ULLogWriter);
		}
		else
		{
			writeRetryTimeFile(new AlarmInfo(0, false),ULLogWriter);
		}

		ULLogWriter.writeActionLog("オートアップロードのアラームを取り消しました");
	}

	/**
	 * アップロードのリトライ回数を読み込むメソッド
	 * @param ULLogWriter ログ書き込み用オブジェクト
	 * @return AlermInfoオブジェクト(リトライ回数を含む)
	 */
    private AlarmInfo readRetryTimeFile(LoggingLog ULLogWriter){
    	String tag = "AccessRetryFile.readRetryFile";
    	int retryTime;
    	boolean isReserved;
    	AlarmInfo aInfo;

    	try {
			BufferedReader bReader = new BufferedReader(new FileReader(DirectoryTree.FILE_RETRYTIMEFILE));
	    	retryTime				= Integer.parseInt(bReader.readLine());
	    	isReserved				= Boolean.parseBoolean(bReader.readLine());

	    	aInfo = new AlarmInfo(retryTime, isReserved);
	    	bReader.close();
		} catch (FileNotFoundException e) {
			//状態ファイルが存在しない場合は新しくファイルを作って、仮のデバイス情報を書き込む
			Log.e(tag, "リトライ回数ファイルが見つかりません");
			ULLogWriter.writeActionLog("リトライ回数ファイルが見つかりません");

			DirectoryTree.DIRECTORY_ECOLOG_CONFIG.mkdirs();
			aInfo = new AlarmInfo(0, false);
			writeRetryTimeFile(aInfo,ULLogWriter);
		} catch (IOException e) {
			//状態ファイルが読み込めない場合も新しくファイルを作り直して、仮のデバイス情報を書き込む
			Log.e(tag, "リトライ回数ファイルが読み込めません");
			ULLogWriter.writeActionLog("リトライ回数ファイルが読み込めません");

			aInfo = new AlarmInfo(0, false);
			writeRetryTimeFile(aInfo,ULLogWriter);
		}
		return aInfo;
    }

    /**
     * 何回目のアップロードかをファイルに書き込むメソッド
     * @param aInfo AlermInfoオブジェクト(リトライ回数を含む)
     * @param ULLogWriter ログ書き込み用オブジェクト
     * @return 書き込み成功ならtrue 失敗ならfalse
     */
    public boolean writeRetryTimeFile(AlarmInfo aInfo,LoggingLog ULLogWriter){
    	String tag = "AccessStateFile.WriteStateFile";

    	try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(DirectoryTree.FILE_RETRYTIMEFILE));

			bWriter.write(aInfo.getRetryTime() + "\r\n");//修正
			bWriter.write(aInfo.isReserved() + "\r\n");//修正

			bWriter.flush();
			bWriter.close();
		} catch (IOException e) {
			Log.e(tag, "データの書き込みに失敗しました。");
			ULLogWriter.writeActionLog("データの書き込みに失敗しました。");
			return false;
		}
		return true;
    }
}


