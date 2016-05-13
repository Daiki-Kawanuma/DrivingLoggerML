package jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant;

import java.io.File;

import android.os.Environment;

/**
 * androidのストレージ内に保存するフォルダ・ファイルの名前とツリー構造を定義するクラス<br>
 * 特定のフォルダにファイルを作りたいときはここの変数を利用
 * @author 1.0 kouno作成<br>
 *         1.1 hagimoto追加 変数の追加<br>
 * @version 1.1
 */
public class DirectoryTree {
	//各ディレクトリ名
	public static final String DIRECTORYNAME_ECOLOG_CONFIG	= "ECOLOG_Config";
	public static final String DIRECTORYNAME_ECOLOG_LOGDATA	= "ECOLOG_LogData";
	public static final String DIRECTORYNAME_TEMPLOGGING			= "DrivingLoggerTempLogging";
	public static final String DIRECTORYNAME_UNSENTLOG			= "DrivingLoggerUnsentLog";
	public static final String DIRECTORYNAME_APPLOG					= "DrivingLoggerAppLog";
	public static final String DIRECTORYNAME_SENTLOG					= "DrivingLoggerSentLog";
	public static final String DIRECTORYNAME_SENTTEMPLOGGING= "DrivingLoggerSentTempLogging";
	public static final String DIRECTORYNAME_SENTAPPLOG			= "DrivingLoggerSentAppLog";

	//各ファイル名
	public static final String FILENAME_STATEFILE									= "DrivingLoggerStateFile.txt";
	public static final String FILENAME_APPLOG										= "AppLog.txt";
	public static final String FILENAME_METADATALOG								= "MetaDataLog.txt";
	public static final String FILENAME_AUTOCONTROLGDLLOG				= "AutoControlGDLTimestamp.txt";
	public static final String FILENAME_RECEIVERTOSTARTUPLOADLOG	= "ReceiverToStartUploadLog.txt";
	public static final String FILENAME_LASTLOCATIONFILE						= "LastLocationFile.txt";
	public static final String FILENAME_RETRYTIMEFILE							= "RetryTimeFile.txt";
	public static final String FILENAME_BATTERYSTATEFILE = "BatteryStateFile.txt";
	public static final String FILENAME_CONFIGACCESSLOG = "ConfigAccessLog.txt";

	//各ノードディレクトリインスタンス
	public static final File DIRECTORY_ECOLOG_CONFIG					= new File(Environment.getExternalStorageDirectory(), DIRECTORYNAME_ECOLOG_CONFIG);
	public static final File DIRECTORY_ECOLOG_LOGDATA				= new File(Environment.getExternalStorageDirectory(), DIRECTORYNAME_ECOLOG_LOGDATA);
	public static final File DIRECTORY_UNSENTLOG							= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_UNSENTLOG);
	public static final File DIRECTORY_TEMPLOGGING						= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_TEMPLOGGING);
	public static final File DIRECTORY_APPLOG									= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_APPLOG);
	public static final File DIRECTORY_SENTLOG								= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_SENTLOG);
	public static final File DIRECTORY_SENTTEMPLOGGING				= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_SENTTEMPLOGGING);
	public static final File DIRECTORY_SENTAPPLOG							= new File(DIRECTORY_ECOLOG_LOGDATA, DIRECTORYNAME_SENTAPPLOG);

	//各ファイル
	public static final File FILE_STATEFILE					= new File(DIRECTORY_ECOLOG_CONFIG, FILENAME_STATEFILE);
	public static final File FILE_LASTLOCATIONFILE	= new File(DIRECTORY_ECOLOG_CONFIG, FILENAME_LASTLOCATIONFILE);
	public static final File FILE_RETRYTIMEFILE			= new File(DIRECTORY_ECOLOG_CONFIG, FILENAME_RETRYTIMEFILE);
	//public static final File FILE_BATTERYSTATEFILE			= new File(DIRECTORY_APPLOG, FILENAME_BATTERYSTATEFILE);
}
