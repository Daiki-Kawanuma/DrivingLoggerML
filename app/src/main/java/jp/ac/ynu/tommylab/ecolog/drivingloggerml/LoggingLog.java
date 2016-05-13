package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

//import android.util.Log;

/**
 * 指定したロケーションにあるログファイルにログを追記する<br>
 * 1つのオブジェクトからのファイルアクセスは同期は取れるが、<br>
 * 2つ以上のオブジェクトから1つのファイルにアクセスした場合同期は取れないので注意<br>
 * <使い方><br>
 * 		writeLog()で書き込む<br>
 * 		closeWriter()でバッファを閉じる<br>
 * <br>
 * 		Android端末のSDカードに保存する場合は<br>
 * 		android.permission.WRITE_EXTERNAL_STORAGE が必要なことに注意
 * @author 1.0 kouno作成<br>
 *         1.1 hagimoto修正 未使用のメソッドをコメントアウト
 * @version 1.1
 */
public class LoggingLog {
	private BufferedWriter bWriter = null;
	//private StringBuilder stringBuilder;
	public static final int INFO = 0;
	public static final int ERROR = 1;

//	/**
//	 * ログファイルのロケーションを設定し、バッファを開く
//	 * @param directoryPath 書き込むファイルパス
//	 * @param fileName ファイル名
//	 * @param isAppend 上書き可能ならtrue 不可能ならfalse
//	 */
//	public LoggingLog(String directoryPath, String fileName, boolean isAppend){
//		File dp = new File(directoryPath);
//		prepare(dp, fileName, isAppend);
//	}

	/**
	 * ログファイルのロケーションを設定し、バッファを開く
	 * @param logDirectory 書き込むディレクトリ名
	 * @param fileName ファイル名
	 * @param isAppend 上書き可能ならtrue 不可能ならfalse
	 */
	public LoggingLog(File logDirectory, String fileName, boolean isAppend){
		prepare(logDirectory, fileName, isAppend);
	}

	/**
	 * 文字列バッファの作成、ログファイルの親ディレクトリの作成、ログファイルの作成をする
	 * @param directory 書き込むディレクトリ名
	 * @param fileName ファイル名
	 * @param isAppend 上書き可能ならtrue 不可能ならfalse
	 */
	private void prepare(File directory, String fileName, boolean isAppend){
		//stringBuilder = new StringBuilder();

		directory.mkdirs();

		File logFile = new File(directory, fileName);
		try {
			logFile.createNewFile();
			bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, isAppend), "Shift_JIS"));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	/**
	 * 引数の文字列をファイルに書き込む
	 * @param logString 書き込む文字列
	 */
	public synchronized void writeLog(String logString){
		try {
			if(bWriter != null){
				bWriter.write(logString);
				bWriter.write("\r\n");
				bWriter.flush();
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

//	/**
//	 * 引数の文字列をバッファに接続する
//	 * @param logString 書き込む文字列
//	 */
//	public synchronized void writeBuffer(String logString){
//		stringBuilder.append(logString);
//	}

//	/**
//	 * バッファをログファイルに出力する
//	 */
//	public void flush(){
//		writeLog(stringBuilder.toString());
//		stringBuilder = new StringBuilder();
//	}

	/**
	 * １．時刻<br>
	 * ２．スレッド名<br>
	 * ３．メソッド名<br>
	 * ４．動作内容<br>
	 * をログファイルに書き込むメソッド
	 * @param action 書き込む文字列
	 */
	public synchronized void writeActionLog(String action){
		//インデックス0はAndroidアプリケーションを動かしているVMについて
		//インデックス1はgetStackTrace()について
		//インデックス2はwriteActionLog()についての情報を表しているので
		//その親メソッドの情報はインデックス3で取得できる
		StackTraceElement stElement = Thread.currentThread().getStackTrace()[3];
		StringBuilder s = new StringBuilder();

		s.append(TimeStamp.getSplitedTimeStringFromYearTilllMillis());
		s.append(",");
		s.append(Thread.currentThread().getName());
		s.append(",");
		s.append(stElement.getMethodName());
		s.append(",");
		s.append(action);

		writeLog(s.toString());
	}

//	/**
//	 * １．時刻<br>
//	 * ２．スレッド名<br>
//	 * ３．メソッド名<br>
//	 * ４．動作内容<br>
//	 * をログファイルに書き込むメソッド
//	 * @param action 書き込む文字列
//	 * @param actionType Logcatに書き込むログの種類
//	 */
//	public synchronized void writeActionLog(String action, int actionType){
//		StackTraceElement stElement = Thread.currentThread().getStackTrace()[3];
//		StringBuilder s = new StringBuilder();
//
//		s.append(TimeStamp.getSplitedTimeStringFromYearTilllMillis());
//		s.append(",");
//		s.append(Thread.currentThread().getName());
//		s.append(",");
//		s.append(stElement.getMethodName());
//		s.append(",");
//		s.append(action);
//
//		writeLog(s.toString());
//
//		switch(actionType){
//		case INFO:
//			Log.i(stElement.getMethodName(), action);
//		case ERROR:
//			Log.e(stElement.getMethodName(), action);
//		}
//	}

	/**
	 * writerを閉じる
	 */
	public void closeWriter(){
		try {
			if(bWriter != null)
				bWriter.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
