package jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog;

import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
/**
 * CompareLogFileThreadを監視するためのクラス
 * @author 1.0 kouno作成 
 * @version 1.0
 */
public class WatchComparisonThread extends Thread {
	private UploadLog uLog;
	private LoggingLog lLog;
	private CompareLogFileThread cLogFile;
	private ArrayList<FileAndLength> previousUnsentLogList;
	private ArrayList<FileAndLength> previousTempLoggingList;
	private ArrayList<FileAndLength> previousAppLogList;
	private boolean isWorkingComparisonThread;

	/**
	 * コンストラクタ
	 * @param uLog アップロードサービスのクラスオブジェクト
	 * @param lLog ログ書き込み用オブジェクト
	 */
	public WatchComparisonThread(UploadLog uLog, LoggingLog lLog){
		this.uLog = uLog;
		this.lLog = lLog;
		this.previousUnsentLogList = copyArrayList(uLog.unsentLogList);
		this.previousTempLoggingList = copyArrayList(uLog.tempLoggingList);
		this.previousAppLogList = copyArrayList(uLog.appLogList);
		isWorkingComparisonThread = true;
		lLog.writeActionLog("監視スレッド:スレッドが作成されました");
	}

	/**
	 * 送信ファイル比較が正常に動作しているかを監視するためにCompareLogFileThreadクラスを保持するためのメソッド
	 * @param wcThread このクラスの進行状況を監視するためのクラスオブジェクト
	 */
	public void setComparisonThread(CompareLogFileThread cLogFile){
		this.cLogFile = cLogFile;
	}

	@Override
	public void run(){
		SmbFile rootDirectory = null;
		try {
			rootDirectory = new SmbFile(UploadLog.URLPart);
		} catch (MalformedURLException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
			return;
		}

		do{
			try {
				sleep(2 * 60 * 1000);
				rootDirectory.exists();
				if(!watchComparisonThread()){
					if(!isWorkingComparisonThread){
						lLog.writeActionLog("監視スレッド:照合スレッドが動作していません(2回目)");
						lLog.writeActionLog("監視スレッド:UploadLogコンポーネントを終了します");
						uLog.errorNumber = UploadLog.ERROR_COMPARISON_STAGNATION;
						cLogFile.interrupt();
						return;
					}else{
						lLog.writeActionLog("監視スレッド:照合スレッドが動作していません(1回目)");
						lLog.writeActionLog("監視スレッド:もう一度動作が確認できなかった場合はUploadLogコンポーネントを終了します");
						isWorkingComparisonThread = false;
					}
				}
				else
				{
					lLog.writeActionLog("監視スレッド:照合スレッドは正常に動作しています");
				}
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				lLog.writeActionLog("監視スレッド:照合スレッドによってinterruptされました");
				return;
			} catch (SmbException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				lLog.writeActionLog("監視スレッド:通信エラーが発生しました");
				lLog.writeActionLog(e.getMessage());
				uLog.errorNumber = UploadLog.ERROR_NO_CONNECTION;
				cLogFile.interrupt();
				return;
			}
		}while(true);
	}

	/**
	 * ArrayListクラスのコピーを取るメソッド
	 * @param copyList コピー元のリスト
	 * @return コピーされたリスト
	 */
	private static ArrayList<FileAndLength> copyArrayList(ArrayList<FileAndLength> copyList)//修正
	{
		ArrayList<FileAndLength> newList = new ArrayList<FileAndLength>(copyList.size());

		synchronized (copyList)
		{
			for(int i = 0; i < copyList.size();i++)
			{
				FileAndLength f = copyList.get(i);
				newList.add(i,new FileAndLength(f.file, f.serverFileLength));
			}
		}

		return newList;
	}

	/**
	 * リストの更新のためにArrayListのコピーを取るメソッド
	 * @param copyFromList コピー元のリスト
	 * @param copyToList コピーされるリスト
	 */
	private static void resetArrayList(ArrayList<FileAndLength> copyFromList,ArrayList<FileAndLength> copyToList)//修正
	{
		copyToList.clear();

		for(int i = 0; i < copyFromList.size();i++)
		{
			FileAndLength f = copyFromList.get(i);
			copyToList.add(i,new FileAndLength(f.file, f.serverFileLength));
		}
	}

	/**
	 * 照合スレッドが正しく動作しているかを検証するメソッド
	 * 正しく動作しているならtrueを、そうでないならfalseを返す
	 * @return 正常動作ならtrue 更新が見られないときfalse
	 */
	private boolean watchComparisonThread(){
		boolean isWorkUnsent = _watchComparisonThread(uLog.unsentLogList, previousUnsentLogList);
		boolean isWorkTemp = _watchComparisonThread(uLog.tempLoggingList, previousTempLoggingList);
		boolean isWorkApp = _watchComparisonThread(uLog.appLogList, previousAppLogList);

		return (isWorkUnsent || isWorkTemp || isWorkApp);
	}

	private boolean _watchComparisonThread(ArrayList<FileAndLength> tempList, ArrayList<FileAndLength> previousList){//修正
		try
		{
			synchronized (tempList)
			{
				for(FileAndLength f1 : previousList)
				{
					int index = tempList.indexOf(f1);

					//ファイルが存在しているかを確認 存在しない場合は送信されたと判断
					if(index != -1)
					{
						FileAndLength f2 = tempList.get(index);

						//ファイルが存在するがサイズが異なる場合は送信中と判断
						if(f1.serverFileLength != f2.serverFileLength)
						{
							lLog.writeActionLog("監視スレッド:" + f1.file.getName() + "は送信中です");
							//previousList = copyArrayList(tempList);
							resetArrayList(tempList,previousList);
							return true;
						}
						else
						{
							if(f1.serverFileLength == 0)
							{
								lLog.writeActionLog("監視スレッド:" + f1.file.getName() + "は未送信です");
							}
							else
							{
								lLog.writeActionLog("監視スレッド:" + f1.file.getName() + "はrenameに失敗したor通信が途中で停止した可能性があります");
							}
						}
					}
					else
					{
						lLog.writeActionLog("監視スレッド:" + f1.file.getName() + "は送信されました");
						//previousList = copyArrayList(tempList);
						resetArrayList(tempList,previousList);
						return true;
					}
				}
			}
		}
		catch(IndexOutOfBoundsException iobe)
		{
			lLog.writeActionLog("監視スレッド:IndexOutOfBoundsException発生");
			lLog.writeActionLog(iobe.getMessage());
		}
		catch(Exception e)
		{
			lLog.writeActionLog("監視スレッド:エラー発生");
			lLog.writeActionLog(e.getMessage());
		}

		return false;
	}
}
