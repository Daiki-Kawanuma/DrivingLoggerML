package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import android.app.Application;
import android.util.Log;

/**
 * アプリケーション共通のインスタンスとしてConfigファイルの読み込み、書き込み、Configデータの提供を行う
 * @author 1.0 kouno作成 <br>
 * 		   2.0 hagimoto作成 旧DeviceInfoとAccessDrivingLoggerStateFileを統合して作成<br>
 * 		   2.1 hagimoto修正 readStateFile()のConfigファイルが存在しないときにreadlockをかけたまま<br>
 * 		   2.2 hagimoto修正 Configファイルが存在しないときにwritelockをかけようとして待ち続けるバグを改善
 * @version 2.2
 */
public class DeviceInfo extends Application{
	//Configファイルの読み込みと書き込みの排他ロック用のオブジェクト
	private ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	//Configファイルアクセス履歴を書き込むオブジェクト
	private LoggingLog lLog;

	//Configデータの項目
	private String userID;
	private String carID;
	private String sensorID;
	private String loggingState;
	private String autoControlGDL;
	private String autoUpload;
	private String uploadState;
	private String temperature;

	//Configデータの状態定義
	public static final String noLogging = "NoLogging";
	public static final String underLogging = "UnderLogging";
	public static final String waitLogging = "waitLogging";
	public static final String yes = "yes";
	public static final String no = "no";
	public static final int UPLOAD_COMPLETED = 0;
	public static final int UPLOAD_NETWORK_ERROR = 1;
	public static final int UPLOAD_FILE_SEND_ERROR = 2;
	public static final int UPLOAD_HIGH_HEATED_BATTERY = 3;
	public static final int UPLOAD_LOW_BATTERY = 4;

	/**
	 * アプリケーション起動時に呼び出され、Configデータを読み込む
	 */
	@Override
    public void onCreate() {
		lLog = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, DirectoryTree.FILENAME_CONFIGACCESSLOG, true);
		lLog.writeActionLog("onCreate()");
		readStateFile();
    }

	/**
	 * 実機ではこのメソッドは呼ばれない
	 */
	@Override
    public void onTerminate() {
//		writeStateFile();
//		lLog.writeActionLog("onTerminate()");
//		lLog.closeWriter();
    }

	/**
	 * ユーザ名を取得する
	 * @return ユーザ名
	 */
	public String getUserID(){
		return userID;
	}

	/**
	 * ユーザ名を設定する
	 * @param userName 設定するユーザ名
	 */
	public void setUserID(String userName){
		this.userID = userName;
		writeStateFile();
	}

	/**
	 * 車種を取得する
	 * @return 車種
	 */
	public String getCarID(){
		return carID;
	}

	/**
	 * 車種を設定する
	 * @param carID 設定する車種
	 */
	public void setCarID(String carID){
		this.carID = carID;
		writeStateFile();
	}

	/**
	 * センサ名を取得する
	 * @return センサ名
	 */
	public String getSensorID() {
		return sensorID;
	}

	/**
	 * センサ名を設定する
	 * @param sensorID 設定するセンサ名
	 */
	public void setSensorID(String sensorID){
		this.sensorID = sensorID;
		writeStateFile();
	}

	/**
	 * ロガーの状態を取得する
	 * @return ロガーの状態
	 */
	public String getState(){
		return loggingState;
	}

	/**
	 * ロガーの状態を設定する
	 * @param state
	 */
	public void setState(String state){
		if(state.equals(noLogging) || state.equals(underLogging) || state.equals(waitLogging)){
			loggingState = state;
			writeStateFile();
		}else
			throw new IllegalArgumentException("引数はDeviceInfo.noLoggingかDeviceInfo.underLoggingを指定してください");
	}

	/**
	 * 自動アップロードが設定されているかを調べる
	 * @return 自動アップロードする場合はtrue しない場合falseを返す
	 */
	public boolean isAutoUpload(){
		if(autoUpload.equals(yes))
			return true;
		else if(autoUpload.equals(no))
			return false;
		else
			throw new IllegalArgumentException("autoUploadの値に不正な値が入っています");
	}

	/**
	 * 自動アップロードの設定を変更する
	 * @param isAutoUpload 自動アップロードの設定値
	 */
	public void setAutoUpload(String isAutoUpload){
		if(isAutoUpload.equals(yes) || isAutoUpload.equals(no)){
			this.autoUpload = isAutoUpload;
			writeStateFile();
		}
		else
			throw new IllegalArgumentException("引数はDeviceInfo.yesかDeviceInfo.noを指定してください");
	}

	/**
	 * 自動ロギングが設定されているかを調べる
	 * @return 自動ロギングする場合はtrue しない場合falseを返す
	 */
	public boolean isAutoControlGDL(){
		if(autoControlGDL.equals(yes))
			return true;
		else if(autoControlGDL.equals(no))
			return false;
		else
			throw new IllegalArgumentException("autoControlGDLの値に不正な値が入っています");
	}

	/**
	 * 自動ロギングの設定を変更する
	 * @param isAutoControlGDL 自動ロギングの設定値
	 */
	public void setAutoControlGDL(String isAutoControlGDL){
		if(isAutoControlGDL.equals(yes) || isAutoControlGDL.equals(no)){
			this.autoControlGDL = isAutoControlGDL;
			writeStateFile();
		}
		else
			throw new IllegalArgumentException("引数はDeviceInfo.yesかDeviceInfo.noを指定してください");
	}

	/**
	 * 前回のアップロード状況を取得する
	 * @return 前回のアップロード状況
	 */
	public String getUploadState(){
		return uploadState;
	}

	/**
	 * 最新のアップロード状況を記録する
	 * @param uploadState 最新のアップロード状況
	 */
	public void setUploadState(int uploadState){
		switch (uploadState) {
		case UPLOAD_COMPLETED:
			this.uploadState = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + "アップロードは正常に終了しました";
			break;
		case UPLOAD_NETWORK_ERROR:
			this.uploadState = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + "ITSサーバへの接続が確認できませんでした";
			break;
		case UPLOAD_FILE_SEND_ERROR:
			this.uploadState = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + "アップロードに失敗しました";
			break;
		case UPLOAD_HIGH_HEATED_BATTERY:
			this.uploadState = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + "バッテリーの温度が高温なためアップロードを中止しました";
			break;
		case UPLOAD_LOW_BATTERY:
			this.uploadState = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + "バッテリーの残量が少量なためアップロードを中止しました";
			break;
		default:
			break;
		}

		writeStateFile();
	}

	/**
	 * アップロード時の内部温度を取得する
	 * @return アップロード時の内部温度
	 */
	public String getTemperature() {
		return temperature;
	}

	/**
	 * アップロード時の内部温度を記録する
	 * @param temperature アップロード時の内部温度
	 */
	public void setTemperature(int temperature) {
		this.temperature = TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + temperature/10 + "℃";
		writeStateFile();
	}

	/**
	 * Configファイルを読み込む
	 */
	public void readStateFile(){
		this.readLock.lock();
		lLog.writeActionLog("readlock開始");
		//ログ書き込み用オブジェクト

		try {
			BufferedReader bReader = new BufferedReader(new FileReader(DirectoryTree.FILE_STATEFILE));
			String line;

			//データ確認用フラグ
			boolean userFlag = false;
			boolean carFlag = false;
			boolean sensorFlag = false;
			boolean stateFlag = false;
			boolean controlGDLFlag = false;
			boolean autoUploadFlag = false;
			boolean uploadFlag = false;
			boolean temperatureFlag = false;

			//コンフィグファイル読み込みループ
			while ((line = bReader.readLine()) != null) {
				String[] array = line.split(":");

				if(array[0].equals("UserID")){
					userID = array[1];
					userFlag = true;
				}else if(array[0].equals("CarID")){
					carID = array[1];
					carFlag = true;
				}else if(array[0].equals("SensorID")){
					sensorID = array[1];
					sensorFlag = true;
				}else if(array[0].equals("LoggingState")){
					if(array[1].equals("null") || array[1] == null)
					{
						loggingState = DeviceInfo.noLogging;
					}
					else {
						loggingState = array[1];
					}
					stateFlag = true;
				}else if(array[0].equals("AutoLogging")){
					if(array[1].equals("null") || array[1] == null)
					{
						autoControlGDL = DeviceInfo.no;
					}
					else {
						autoControlGDL = array[1];
					}

					controlGDLFlag = true;
				}else if(array[0].equals("AutoUpload")){
					if(array[1].equals("null") || array[1] == null)
					{
						autoUpload = DeviceInfo.no;
					}
					else {
						autoUpload = array[1];
					}

					autoUploadFlag = true;
				}else if(array[0].equals("UploadState")){
					if(array[1] == null || array[1].equals("null")){
						uploadState = array[1];
						uploadFlag = true;
					}else{
						uploadState = array[1] + ":" + array[2] + ":" + array[3];
						uploadFlag = true;
					}
				}else if(array[0].equals("BatteryTemperature")){
					if(array[1] == null || array[1].equals("null")){
						temperature = array[1];
						temperatureFlag = true;
					}else{
						temperature = array[1] + ":" + array[2] + ":" + array[3];
						temperatureFlag = true;
					}
				}
			}

			bReader.close();

			//読み込みデータ確認
			if(userFlag == false || carFlag == false || sensorFlag == false || stateFlag == false || controlGDLFlag == false || autoUploadFlag == false || uploadFlag == false || temperatureFlag == false){
				lLog.writeActionLog("configファイルにエラーがありました");
				if(userFlag == false)
				{
					lLog.writeActionLog("UserIDにエラーがありました\\デフォルト設定にします");
					userID = "NoName";
				}
				if(carFlag == false)
				{
					lLog.writeActionLog("CarIDにエラーがありました\\デフォルト設定にします");
					carID = "Car";
				}
				if(sensorFlag == false)
				{
					lLog.writeActionLog("SensorIDにエラーがありました\\デフォルト設定にします");
					sensorID = "DefaltSensor";
				}
				if(stateFlag == false)
				{
					lLog.writeActionLog("LoggingStateにエラーがありました\\デフォルト設定にします");
					loggingState = DeviceInfo.noLogging;
				}
				if(controlGDLFlag == false)
				{
					lLog.writeActionLog("AutoLoggingにエラーがありました\\デフォルト設定にします");
					autoControlGDL = DeviceInfo.no;
				}
				if(autoUploadFlag == false)
				{
					lLog.writeActionLog("AutoUploadにエラーがありました\\デフォルト設定にします");
					autoUpload = DeviceInfo.no;
				}
				if(uploadFlag == false)
				{
					lLog.writeActionLog("UploadStateにエラーがありました\\デフォルト設定にします");
					uploadState = "null";
				}
				if(temperatureFlag == false)
				{
					lLog.writeActionLog("BatteryTemperatureにエラーがありました\\デフォルト設定にします");
					temperature = "null";
				}
			}
			else{
				lLog.writeActionLog("正常にConfigファイルを読み込みました");
			}
			
			this.readLock.unlock();
			lLog.writeActionLog("readlock解除");
			
		} catch (FileNotFoundException e) {
			//状態ファイルが存在しない場合は新しくファイルを作って、仮のデバイス情報を書き込む
			//Log.e(tag, "状態ファイルが見つかりません");
			lLog.writeActionLog("Configファイルが見つかりません\\新規にファイルを作成します");
			DirectoryTree.DIRECTORY_ECOLOG_CONFIG.mkdirs();
			resetConfig();
			this.readLock.unlock();
			lLog.writeActionLog("readlock解除");
			writeStateFile();
		} catch (IOException ioe) {
			//状態ファイルが読み込めない場合も新しくファイルを作り直して、仮のデバイス情報を書き込む
			//Log.e(tag, "状態ファイルが読み込めません");
			lLog.writeActionLog("Configファイルが読み込めません\\新規にファイルを作成します");
			resetConfig();
			this.readLock.unlock();
			lLog.writeActionLog("readlock解除");
			writeStateFile();
		} catch (Exception e){
			lLog.writeActionLog(e.getClass().getName());
			lLog.writeActionLog(e.getMessage());

			java.lang.StackTraceElement[] stack = e.getStackTrace();

			for (int i=0; i<stack.length; i++){
				lLog.writeActionLog(stack[i].toString());
			}
			
			this.readLock.unlock();
			lLog.writeActionLog("readlock解除");
		}
    }

    /**
     * Configファイルにデータを書き込む
     * @return 書き込み成功 true 書き込み失敗 false
     */
    public boolean writeStateFile(){

    	this.writeLock.lock();
    	lLog.writeActionLog("writelock開始");
    	String tag = "AccessStateFile.WriteStateFile";

    	try {
    		//データ書き込み処理
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(DirectoryTree.FILE_STATEFILE));
			bWriter.write("UserID:" + userID + "\r\n");
			bWriter.write("CarID:" + carID + "\r\n");
			bWriter.write("SensorID:" + sensorID + "\r\n");
			bWriter.write("LoggingState:" + loggingState + "\r\n");
			bWriter.write("AutoLogging:" + autoControlGDL + "\r\n");
			bWriter.write("AutoUpload:" + autoUpload + "\r\n");
			bWriter.write("UploadState:" + uploadState + "\r\n");
			bWriter.write("BatteryTemperature:" + temperature + "\r\n");

			bWriter.flush();
			bWriter.close();
			lLog.writeActionLog("正常にConfigファイルに書き込みました");
		} catch (FileNotFoundException e) {
			Log.e(tag, "状態ファイルが見つかりません");
			lLog.writeActionLog("Configファイルが見つかりません");
			return false;
		} catch (IOException e) {
			Log.e(tag, "データの書き込みに失敗しました。");
			lLog.writeActionLog("Configファイルの書き込みに失敗しました");
			return false;
		}finally{
			this.writeLock.unlock();
			lLog.writeActionLog("writelock解除");
		}
		return true;
    }

    /**
     * Configファイルをリセットする
     */
    private void resetConfig()
    {
    	userID = "NoName";
    	carID = "Car";
    	sensorID = "DefaltSensor";
    	loggingState = DeviceInfo.noLogging;
    	autoControlGDL = DeviceInfo.no;
    	autoUpload = DeviceInfo.no;
    	uploadState = "null";
    	temperature = "null";
    	lLog.writeActionLog("Configファイルをデフォルト設定にリセットしました");
    }
}
