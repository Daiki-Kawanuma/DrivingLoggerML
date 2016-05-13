package jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.LoggingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * アップロードが成功しなかった場合にAlarmManagerにより一定時間後に呼び出されるBroadcastReceiver
 * @author 1.0 kouno作成 
 * @version 1.0
 *
 */
public class ReceiverToStartUploadLog extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent uploadLog = new Intent(context, UploadLog.class);
		LoggingLog lLog = new LoggingLog(DirectoryTree.DIRECTORY_APPLOG, DirectoryTree.FILENAME_RECEIVERTOSTARTUPLOADLOG, true);

		long startTime = System.currentTimeMillis();
		uploadLog.putExtra(Key.KEY_START_TIME, startTime);
		uploadLog.putExtra("process","retry");

		lLog.writeActionLog("ログのアップロードを開始します");
		context.startService(uploadLog);
	}
}
