package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.AccessLastLocationFile;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.R;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.ComponentName;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.drivingloggerui.DrivingLoggerUI;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog.UploadLog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * GPSと加速度のログを記録するクラス<br>
 * 呼び出されるとフォアグラウンド化やスリープの無効化を行いロギングを開始する<br>
 * ロギングはサービスを停止するまで続ける
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto修正 メタデータの追加、フォアグラウンド化の変数の変更<br>
 * @version 1.1 
 *
 */
public class GetDrivingLog extends Service {
	//センサ関連
	SensorManager sManager;
	LocationManager lManager;
	static final int SENSOR_FREQ_TYPE = SensorManager.SENSOR_DELAY_UI;
	private static final String ACCELEROMETER = "Accel";
	private static final String GPS = "GPS";
	private static final String NMEA = "Nmea";

	//リスナ
	AccelListener aListener;
	GPSListener gListener;
	CustomNmeaListener nmeaListener;
	//ウェイクロック
	WakeLock wakeLock;
	//ハンドラスレッド
	HandlerThread accelHandlerThread;
	HandlerThread gpsHandlerThread;
	//ロギング開始時間
	long startTime;
	String startTimeString;
	//ログ関連
	ArrayList<Object> accelLogDataList;
	ArrayList<Object> gpsLogDataList;
	ArrayList<Object> nmeaLogDataList;
	ArrayList<ArrayList<Object>> failedRecordingAccelLogDataList;
	ArrayList<ArrayList<Object>> failedRecordingGPSLogDataList;
	ArrayList<ArrayList<Object>> failedRecordingNmeaLogDataList;
	static final int ARRAY_SIZE = 2000;
	LoggingLog GDLLogWriter;
	File accelLogFile;
	File gpsLogFile;
	File nmeaLogFile;
	//オートセーブ
	AutoSave aSave;
	//自動アップロード感覚
	public static final long autoUploadLogSpan = 15 * 60 * 1000;

	/**
	 * GetDrivingLogクラスを作成するときに実行されるメソッド
	 * グローバルなオブジェクトの定義を行う
	 */
	@Override
	public void onCreate(){
		super.onCreate();

		accelLogDataList = new ArrayList<Object>(ARRAY_SIZE);
		gpsLogDataList = new ArrayList<Object>(ARRAY_SIZE);
		nmeaLogDataList = new ArrayList<Object>(ARRAY_SIZE);
		failedRecordingAccelLogDataList = new ArrayList<ArrayList<Object>>();
		failedRecordingGPSLogDataList = new ArrayList<ArrayList<Object>>();
		failedRecordingNmeaLogDataList = new ArrayList<ArrayList<Object>>();

		sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		lManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeLock");

		accelHandlerThread = new HandlerThread("AccelLoop");
		gpsHandlerThread = new HandlerThread("GPSLoop");


		aSave = new AutoSave();
		aSave.setName("AutoSave");
	}

	/**
	 * GetDrivingLogクラスを作成されたあとに実行されるメソッド<br>
	 * 前処理とスレッドの生成を行う
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String tag = ComponentName.GetDrivingLog + "#onStartCommand()";

		startTime = intent.getExtras().getLong(Key.KEY_START_TIME);
		startTimeString = TimeStamp.getTimeString(startTime);
		DirectoryTree.DIRECTORY_TEMPLOGGING.mkdirs();

		//内部動作ログファイルを作成
		DirectoryTree.DIRECTORY_APPLOG.mkdirs();
		GDLLogWriter = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, startTimeString + ComponentName.GetDrivingLog + ".txt", true);

		//GetDrivingLogのフォアグラウンド化
		GDLLogWriter.writeActionLog("");
		Notification notification = new Notification(R.mipmap.ic_launcher, "DrivingLogger", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, DrivingLoggerUI.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), "ロギング中...", pendingIntent);
		//↓この引数を0にするとフォアグラウンド化が実行されない
		startForeground(1, notification);

		//CPUスリープの無効化
		wakeLock.acquire();
		GDLLogWriter.writeActionLog("スリープの無効化");

		//加速度・GPSログファイルの作成
		String accelLogFileName = makeTempLoggingFileName(ACCELEROMETER, startTime);
		String GPSLogFileName = makeTempLoggingFileName(GPS, startTime);
		String nmeaLogFileName = makeTempLoggingFileName(NMEA, startTime);
		accelLogFile = new File(DirectoryTree.DIRECTORY_TEMPLOGGING, accelLogFileName);
		gpsLogFile = new File(DirectoryTree.DIRECTORY_TEMPLOGGING, GPSLogFileName);
		nmeaLogFile = new File(DirectoryTree.DIRECTORY_TEMPLOGGING, nmeaLogFileName);
		DirectoryTree.DIRECTORY_TEMPLOGGING.mkdir();
		try {
			accelLogFile.createNewFile();
			gpsLogFile.createNewFile();
			nmeaLogFile.createNewFile();
		} catch (IOException e) {
			Log.e(tag, "ファイルが作成できません");
		}

		//加速度値・位置座標受信スレッド作成
		accelHandlerThread.start();
		gpsHandlerThread.start();

		//加速度値・位置座標の要求
		Handler accelHandler = new Handler(accelHandlerThread.getLooper());
		Sensor aSensor = sManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
		aListener = new AccelListener(this);
		gListener = new GPSListener(this);
		nmeaListener = new CustomNmeaListener(this);

		sManager.registerListener(aListener, aSensor, SENSOR_FREQ_TYPE, accelHandler);
		lManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gListener, gpsHandlerThread.getLooper());
		lManager.addNmeaListener(nmeaListener);

		DeviceInfo dInfo = (DeviceInfo)this.getApplication();


		//ログメタデータの作成
		LoggingLog metaDataLog = new LoggingLog(DirectoryTree.DIRECTORY_TEMPLOGGING,TimeStamp.getTimeString(startTime) + DirectoryTree.FILENAME_METADATALOG, true);
		metaDataLog.writeLog("MODEL:" + Build.MODEL);
		metaDataLog.writeLog("Frequency:" + convertSenserDelayToFrequency(SENSOR_FREQ_TYPE));
		metaDataLog.writeLog("-------------------------------------------------------------");
		metaDataLog.writeLog("加速度センサ情報");
		metaDataLog.writeLog("MaximumRange:" + aSensor.getMaximumRange());
		metaDataLog.writeLog("MinDelay:" + aSensor.getMinDelay());
		metaDataLog.writeLog("Name:" + aSensor.getName());
		metaDataLog.writeLog("Power:" + aSensor.getPower());
		metaDataLog.writeLog("Resolution:" + aSensor.getResolution());
		metaDataLog.writeLog("Type:" + aSensor.getType());
		metaDataLog.writeLog("Vender:" + aSensor.getVendor());
		metaDataLog.writeLog("Version:" + aSensor.getVersion());
		metaDataLog.writeLog("-------------------------------------------------------------");
		metaDataLog.writeLog("ID情報");
		metaDataLog.writeLog("DriverID:" + dInfo.getUserID());
		metaDataLog.writeLog("CarID:" + dInfo.getCarID());
		metaDataLog.writeLog("SensorID:" + dInfo.getSensorID());
		metaDataLog.writeLog("-------------------------------------------------------------");
		metaDataLog.writeLog("電池情報");

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

		metaDataLog.writeLog("電池の残量:" + level);

		switch (plugged)
		{
		case BatteryManager.BATTERY_PLUGGED_AC:
			metaDataLog.writeLog("ケーブルの接続状態:AC");
			break;
		case BatteryManager.BATTERY_PLUGGED_USB:
			metaDataLog.writeLog("ケーブルの接続状態:USB");
			break;
		}

		metaDataLog.writeLog("電池の有無:" + present);

		switch (status)
		{
		case BatteryManager.BATTERY_STATUS_CHARGING:
			metaDataLog.writeLog("充電の状態:CHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_DISCHARGING:
			metaDataLog.writeLog("充電の状態:DISCHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_FULL:
			metaDataLog.writeLog("充電の状態:FULL");
			break;
		case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
			metaDataLog.writeLog("充電の状態:NOT CHARGING");
			break;
		case BatteryManager.BATTERY_STATUS_UNKNOWN:
			metaDataLog.writeLog("充電の状態:UNKNOWN");
			break;
		}
		metaDataLog.writeLog("電池の温度:" + (float)temperature + "\r\n");

		metaDataLog.closeWriter();

		GDLLogWriter.writeActionLog("メタデータの作成完了");

		//オートセーブの開始
		aSave.start();

		GDLLogWriter.writeActionLog("Autosaveスレッド作成完了");

		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * GetDrivingLogクラスが破棄されるときに呼び出されるメソッド<br>
	 * 終了処理を行う
	 */
	@Override
	public void onDestroy(){
		GDLLogWriter.writeActionLog("ロギング終了処理の開始");
		//フォアグラウンド化の終了
		stopForeground(true);

		//CPUスリープ無効化の解除
		wakeLock.release();
		GDLLogWriter.writeActionLog("スリープの無効化解除");

		//加速度値・位置座標取得の終了
		sManager.unregisterListener(aListener);
		lManager.removeUpdates(gListener);
		lManager.removeNmeaListener(nmeaListener);

		accelHandlerThread.quit();
		gpsHandlerThread.quit();

		//オートセーブの終了
		stopAutoSave();

		//最後のGPS座標をファイルに記録
		if(lManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null)
			AccessLastLocationFile.writeLastLocationFile(lManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		else
			AccessLastLocationFile.writeLastLocationFile(new Location(LocationManager.GPS_PROVIDER));

		//蓄積したログデータをログファイルに書き込む
		recordLogData();

		//ロギングが完了したログファイルをUnsentLogに移動する
		DirectoryTree.DIRECTORY_UNSENTLOG.mkdirs();
		accelLogFile.renameTo(new File(DirectoryTree.DIRECTORY_UNSENTLOG,TimeStamp.getTimeString(startTime) + "Unsent" +convertSenserDelayToFrequency(SENSOR_FREQ_TYPE) + "Hz" +"Accel.csv"));
		gpsLogFile.renameTo(new File(DirectoryTree.DIRECTORY_UNSENTLOG,TimeStamp.getTimeString(startTime) + "UnsentGPS.csv"));
		nmeaLogFile.renameTo(new File(DirectoryTree.DIRECTORY_UNSENTLOG, TimeStamp.getTimeString(startTime) + "UnsentNmea.csv"));
		File metaDataLogFile = new File(DirectoryTree.DIRECTORY_TEMPLOGGING,TimeStamp.getTimeString(startTime) + DirectoryTree.FILENAME_METADATALOG);
		metaDataLogFile.renameTo(new File(DirectoryTree.DIRECTORY_UNSENTLOG,TimeStamp.getTimeString(startTime) + DirectoryTree.FILENAME_METADATALOG));

		//オートアップロードを行う設定なら、
		//UploadLogを呼び出す
		DeviceInfo dInfo = (DeviceInfo)this.getApplication();

		if(dInfo.isAutoUpload()){
			//UploadLogコンポーネントの呼び出し
			Intent intent = new Intent(this, UploadLog.class);
			intent.putExtra(Key.KEY_START_TIME, startTime);
			intent.putExtra("process","auto");
			startService(intent);
		}

		GDLLogWriter.writeActionLog("ロギング終了処理完了");
		GDLLogWriter.closeWriter();
	}

	/**
	 * accelLogDataListに加速度データを追加する
	 * @param time 内部時刻
	 * @param x X軸加速度
	 * @param y Y軸加速度
	 * @param z Z軸加速度
	 */
	public void addAccelLog(long time, float x, float y, float z){
		AccelLogData data = new AccelLogData(time, x, y, z);
		accelLogDataList.add(data);
	}


	/**
	 * gpsLogDataListに位置情報データを追加する
	 * @param GPSTime GPS時刻
	 * @param PhoneTime 内部時刻
	 * @param latitude 緯度
	 * @param longitude 経度
	 * @param altitude 高度
	 */
	public void addGPSLog(long GPSTime, long PhoneTime, double latitude, double longitude, double altitude){
		GPSLogData data = new GPSLogData(GPSTime, PhoneTime, latitude, longitude, altitude);
		gpsLogDataList.add(data);
	}

	/**
	 * gpsLogDataListに位置情報データを追加する
	 * @param GPSTime GPS時刻
	 * @param PhoneTime 内部時刻
	 * @param latitude 緯度
	 * @param longitude 経度
	 * @param altitude 高度
	 * @param accuracy 精度
	 */
	public void addGPSLog(long GPSTime, long PhoneTime, double latitude, double longitude, double altitude,double accuracy){
		GPSLogData data = new GPSLogData(GPSTime, PhoneTime, latitude, longitude, altitude, accuracy);
		//GpsStatus status = lManager.getGpsStatus(null);
		//int satellites = status.getMaxSatellites();
		//GPSLogData data = new GPSLogData(GPSTime, PhoneTime, latitude, longitude, altitude, accuracy, satellites);
		gpsLogDataList.add(data);
	}

	public void addNmeaLog(String timestamp, String jsonRaw){

		NmeaLogData data = new NmeaLogData(timestamp, jsonRaw);
		nmeaLogDataList.add(data);
	}

	/**
	 * センサ名と現在時刻から加速度・位置座標ログファイルの名前を作成する
	 * @param sensorName センサ名
	 * @param tempTime 時刻
	 * @return ファイル名のString
	 */
	public static String makeTempLoggingFileName(String sensorName, long tempTime){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(tempTime);

		StringBuilder s = new StringBuilder(100);
		s.append(TimeStamp.getTimeString(cal));
		s.append("TempLogging");

		if(sensorName.equals(ACCELEROMETER)){
			s.append(convertSenserDelayToFrequency(SENSOR_FREQ_TYPE));
			s.append("Hz");
		}
		s.append(sensorName);
		s.append(".csv");
		return s.toString();
	}

	/**
	 * SensorManager.Sensor_Delay_~~の値を周波数に置き換える
	 * @param sensorDelay センサの遅延速度の設定項目
	 * @return Hz(この値は目安であり機種やOSバージョンによって変化する)
	 */
	public static int convertSenserDelayToFrequency(int sensorDelay){
		switch(SENSOR_FREQ_TYPE){
		case SensorManager.SENSOR_DELAY_FASTEST:
			return 100;
		case SensorManager.SENSOR_DELAY_GAME:
			return 50;
		case SensorManager.SENSOR_DELAY_UI:
			return 16;
		case SensorManager.SENSOR_DELAY_NORMAL:
			return 6;
		default :
			return sensorDelay;
		}
	}

	/**
	 * オートセーブを終了させる
	 */
	public void stopAutoSave(){
		aSave.isRunning = false;
		aSave.interrupt();
		GDLLogWriter.writeActionLog("オートセーブの終了");
	}

	/**
	 * 蓄積したログデータをログファイルに書き込むメソッド
	 */
	public synchronized void recordLogData() {
		GDLLogWriter.writeActionLog("加速度データの書き込み開始");
		recordAccelLogData();
		GDLLogWriter.writeActionLog("GPSデータの書き込み開始");
		recordGPSLogData();
		GDLLogWriter.writeActionLog("Nmeaデータの書き込み開始");
		recordNmeaLogData();
		GDLLogWriter.writeActionLog("ログデータの書き込み終了");
	}

	/**
	 * 加速度データをファイルに書き込むメソッド
	 */
	private synchronized void recordAccelLogData(){
		GDLLogWriter.writeActionLog("recordAccelLogData開始");
		ArrayList<Object> dbBuffer = null;
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(accelLogFile, true));
		} catch (IOException e) {
			return;
		}

		//書き込みに失敗していたデータをログファイルに書き込む
		if(failedRecordingAccelLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込み");
			for(ArrayList<Object> a : failedRecordingAccelLogDataList){
				for(Object d : a){
					try {
						writer.write(d.toString());
					} catch (IOException e) {
						GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込みに失敗しました");
						try {
							writer.close();
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						return;
					}
				}
			}
		}

		if(accelLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("ログデータのバッファ書き込み");
			dbBuffer = accelLogDataList;
			accelLogDataList = new ArrayList<Object>(ARRAY_SIZE);
			for(Object d : dbBuffer){
				try {
					writer.write(d.toString());
				} catch (IOException e) {
					failedRecordingAccelLogDataList.add(dbBuffer);
					GDLLogWriter.writeActionLog("ログデータのバッファ書き込みに失敗しました");
					try {
						writer.close();
					} catch (IOException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
					return;
				}
			}
		}

		try {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュ");
			writer.flush();
		} catch (IOException e1) {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュに失敗しました");
			if(dbBuffer != null)
				failedRecordingAccelLogDataList.add(dbBuffer);

			try {
				writer.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			return;
		}

		try {
			failedRecordingAccelLogDataList = new ArrayList<ArrayList<Object>>();
			writer.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
	/**
	 * 加速度データをファイルに書き込むメソッド
	 */
	private synchronized void recordGPSLogData(){
		GDLLogWriter.writeActionLog("recordGPSLogData開始");
		ArrayList<Object> dbBuffer = null;
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(gpsLogFile, true));
		} catch (IOException e) {
			return;
		}

		//書き込みに失敗していたデータをログファイルに書き込む
		if(failedRecordingGPSLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込み");
			for(ArrayList<Object> a : failedRecordingGPSLogDataList){
				for(Object d : a){
					try {
						writer.write(d.toString());
					} catch (IOException e) {
						GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込みに失敗しました");
						try {
							writer.close();
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						return;
					}
				}
			}
		}

		if(gpsLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("ログデータのバッファ書き込み");
			dbBuffer = gpsLogDataList;
			gpsLogDataList = new ArrayList<Object>(ARRAY_SIZE);
			for(Object d : dbBuffer){
				try {
					writer.write(d.toString());
				} catch (IOException e) {
					failedRecordingGPSLogDataList.add(dbBuffer);
					GDLLogWriter.writeActionLog("ログデータのバッファ書き込みに失敗しました");
					try {
						writer.close();
					} catch (IOException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
					return;
				}
			}
		}

		try {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュ");
			writer.flush();
		} catch (IOException e1) {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュに失敗しました");
			if(dbBuffer != null)
				failedRecordingGPSLogDataList.add(dbBuffer);

			try {
				writer.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			return;
		}

		try {
			failedRecordingGPSLogDataList = new ArrayList<ArrayList<Object>>();
			writer.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	private synchronized void recordNmeaLogData(){
		GDLLogWriter.writeActionLog("recordNmeaLogData開始");
		ArrayList<Object> dbBuffer = null;
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(nmeaLogFile, true));
		} catch (IOException e) {
			return;
		}

		//書き込みに失敗していたデータをログファイルに書き込む
		if(failedRecordingNmeaLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込み");
			for(ArrayList<Object> a : failedRecordingNmeaLogDataList){
				for(Object d : a){
					try {
						writer.write(d.toString());
					} catch (IOException e) {
						GDLLogWriter.writeActionLog("書き込み失敗データのバッファ書き込みに失敗しました");
						try {
							writer.close();
						} catch (IOException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
						return;
					}
				}
			}
		}

		if(nmeaLogDataList.size() > 0){
			GDLLogWriter.writeActionLog("ログデータのバッファ書き込み");
			dbBuffer = nmeaLogDataList;
			nmeaLogDataList = new ArrayList<Object>(ARRAY_SIZE);
			for(Object d : dbBuffer){
				try {
					writer.write(d.toString());
				} catch (IOException e) {
					failedRecordingNmeaLogDataList.add(dbBuffer);
					GDLLogWriter.writeActionLog("ログデータのバッファ書き込みに失敗しました");
					try {
						writer.close();
					} catch (IOException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
					return;
				}
			}
		}

		try {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュ");
			writer.flush();
		} catch (IOException e1) {
			GDLLogWriter.writeActionLog("書き込みデータバッファのフラッシュに失敗しました");
			if(dbBuffer != null)
				failedRecordingNmeaLogDataList.add(dbBuffer);

			try {
				writer.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			return;
		}

		try {
			failedRecordingNmeaLogDataList = new ArrayList<ArrayList<Object>>();
			writer.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	/**
	 * ロギング中のデータをオートセーブするクラス
	 * @author kouno 1.0
	 * @version 1.0 初期実装
	 */
	protected class AutoSave extends Thread{
		protected long saveInterval = 1 * 60 * 1000;
		protected boolean isRunning;

		public AutoSave(){
			isRunning = true;
		}

		@Override
		public void run(){
			String tag = "AutoSave.run";
			while(isRunning){
				try {
					GDLLogWriter.writeActionLog("スリープ開始");
					sleep(saveInterval);

					GDLLogWriter.writeActionLog("オートセーブ実行");
					recordLogData();
				} catch (InterruptedException e) {
					Log.e(tag, "スレッドがインタラプトされました");
					GDLLogWriter.writeActionLog("スレッドがインタラプトされました");
					break;
				}
			}
		}
	}



	@Override
	public IBinder onBind(Intent arg0) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
}
