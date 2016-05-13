package jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;

/**
 * 送信したファイルがサーバにコピーされたかをファイルサイズで比較し<br>
 * アップロードの進行状態を確認するクラス
 * @author 1.0 kouno作成<br>
 * @version 1.0
 */
public class CompareLogFileThread extends Thread {
	private UploadLog uLog;
	private LoggingLog lLog;
	private WatchComparisonThread wcThread;

	/**
	 * コンストラクタ
	 * @param uLog アップロードサービスのクラスオブジェクト
	 * @param lLog ログ書き込み用オブジェクト
	 */
	public CompareLogFileThread(UploadLog uLog, LoggingLog lLog){
		this.uLog = uLog;
		this.lLog = lLog;
		lLog.writeActionLog("照合スレッド:スレッドが作成されました");
	}

	/**
	 * 送信完了を確認後に監視スレッドに割り込むためにクラスオブジェクトを保持するためのメソッド
	 * @param wcThread このクラスの進行状況を監視するためのクラスオブジェクト
	 */
	public void setWatchThread(WatchComparisonThread wcThread){
		this.wcThread = wcThread;
	}

	@Override
	public void run(){//修正
		try{
			checkUploadedFile(uLog.serverDirectoryPath_AppLog, uLog.appLogList, DirectoryTree.DIRECTORY_SENTAPPLOG, lLog);

			boolean flagTemp = checkUploadedFile(uLog.serverDirectoryPath_TempLogging, uLog.tempLoggingList, DirectoryTree.DIRECTORY_SENTTEMPLOGGING, lLog);
			boolean flagUnsent = checkUploadedFile(uLog.serverDirectoryPath_UnsentLog, uLog.unsentLogList, DirectoryTree.DIRECTORY_SENTLOG, lLog);

			while(flagTemp == false || flagUnsent == false)
			{
				lLog.writeActionLog("照合スレッド:照合未完了。1分後に再照合します");
				sleep(1 * 60 * 1000);

				checkUploadedFile(uLog.serverDirectoryPath_AppLog, uLog.appLogList, DirectoryTree.DIRECTORY_SENTAPPLOG, lLog);
				flagTemp = checkUploadedFile(uLog.serverDirectoryPath_TempLogging, uLog.tempLoggingList, DirectoryTree.DIRECTORY_SENTTEMPLOGGING, lLog);
				flagUnsent = checkUploadedFile(uLog.serverDirectoryPath_UnsentLog, uLog.unsentLogList, DirectoryTree.DIRECTORY_SENTLOG, lLog);
			}
		}catch(InterruptedException e){
			lLog.writeActionLog("照合スレッド:sleep()がinterruptされました");
			return;
		} catch (Exception e) {
			//サーバに接続できなかったときにここへ飛ぶ
			e.printStackTrace();
			lLog.writeActionLog("照合スレッド:ITSサーバに接続できませんでした");
			uLog.errorNumber = UploadLog.ERROR_NO_CONNECTION;
		}finally{
			wcThread.interrupt();
		}
	}

	/**
	 * Android端末に記録されたファイルとサーバ上のファイルが同一かチェックする<br>
	 * 同一ならばファイルリストからファイルインスタンスを削除しそのファイルインスタンスをフォルダ移動する<br>
	 * 端末とサーバのログファイルの大きさが等しい時に成功とする<br>
	 * @param serverSideDirectoryPath サーバのパス
	 * @param fileList 送信確認を行うファイルリスト
	 * @param sentLogDirectory 送信するファイルが存在するディレクトリ
	 * @param lLog ログ書き込み用オブジェクト
	 * @return 送信完了したらtrue 未完了ならfalse
	 * @throws Exception サーバに接続できない場合例外を投げる
	 */
	private boolean checkUploadedFile(String serverSideDirectoryPath, ArrayList<FileAndLength> fileList, File sentLogDirectory, LoggingLog lLog) throws Exception{
		sentLogDirectory.mkdirs();

		if(fileList.size() == 0)
		{
			lLog.writeActionLog("照合スレッド:" + sentLogDirectory.getName() + "にファイルが存在しません");
			return true;
		}

		for(Iterator<FileAndLength> i = fileList.iterator(); i.hasNext();){
			FileAndLength f = i.next();
			try {
				String serverFilePath = serverSideDirectoryPath + "/" + f.file.getName();
				SmbFile serverFile = new SmbFile(serverFilePath);
				long serverFileLength = serverFile.length();

				lLog.writeActionLog("照合スレッド: " + f.file.getName() + " = " + f.file.length() + " : " + serverFile.length());

				if(f.file.length() == serverFileLength){
					if(!f.file.getName().equals(DirectoryTree.FILENAME_AUTOCONTROLGDLLOG) && !f.file.getName().equals(DirectoryTree.FILENAME_RECEIVERTOSTARTUPLOADLOG ) && !f.file.getName().equals(DirectoryTree.FILENAME_CONFIGACCESSLOG ))
						f.file.renameTo(new File(sentLogDirectory, f.file.getName()));

					i.remove();
				}
				else
				{
					f.serverFileLength = serverFileLength;
				}
			} catch (SmbException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				String errorMessage = e.getMessage();
				if(errorMessage.startsWith(UploadLog.ERROR_FAILED_CONNECTION)){
					lLog.writeActionLog("照合スレッド:サーバに接続できません");
					throw new Exception();
				}else{
					lLog.writeActionLog(e.getMessage());
					lLog.writeActionLog("照合スレッド:指定されたファイルがサーバ上に存在しません or ファイルがTempフォルダからUnsentフォルダに移動された可能性があります");
					lLog.writeActionLog("ファイル名：" + f.file.getName());
				}
			} catch (MalformedURLException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				lLog.writeActionLog("照合スレッド:URLの指定方法が間違っています");
			}
		}


		if(fileList.size() == 0)
		{
			lLog.writeActionLog("照合スレッド:" + sentLogDirectory.getName() + "のファイルの送信完了を確認しました");

			return true;
		}
		else
		{
			lLog.writeActionLog("照合スレッド:" + sentLogDirectory.getName() + "の残りファイル数:" + fileList.size());
			return false;
		}
	}
}
