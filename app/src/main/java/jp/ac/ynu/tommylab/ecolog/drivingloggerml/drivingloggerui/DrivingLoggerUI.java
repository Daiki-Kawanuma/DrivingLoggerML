package jp.ac.ynu.tommylab.ecolog.drivingloggerml.drivingloggerui;


import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.R;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Action;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.Key;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog.GetDrivingLog;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.setuserandcarid.SetUserAndCarID;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog.UploadLog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * メインUIを表すクラス
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto修正 IDにSensorIDを追加、アップロードの状況を表示するためのBroadcastReceiverを追加
 * @version 1.1  
 */
public class DrivingLoggerUI extends Activity {
	//レイアウト関連
	private TextView versionView;
	private TextView userNameView;
	private TextView carIDView;
	private TextView sensorIDView;
	private TextView uploadView;
	private TextView temperatureView;
	private Button controlButton;
	private static final String textStartLogging	= "ロギング開始";
	private static final String textEndLogging	= "ロギング終了";
	private static final String textWaitLogging	= "ロギング停止待ち";
	private SetLayoutReceiver slReceiver;
	private UploadViewReceiver uiReceiver;
	private static final IntentFilter slFilter = new IntentFilter(Action.ACTION_SET_LAYOUT);
	private static final IntentFilter ulFilter = new IntentFilter(Action.ACTION_UPLOADVIEW);
	//コンフィグファイル関連
	private DeviceInfo mInfo;
	//パッケージ関連
	private PackageManager pManager;
	private PackageInfo pInfo;
	private String vName;
	//ハンドラ関連
	public static final String HANDLER = "handler";
	//デバッグ関連
	public static final String tag = "ControlWAA";
	//リクエストコード
	public static final int REQUEST_SELECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	public static final int REQUEST_ENABLE_GPS = 3;
	public static final int REQUEST_INPUT_USERNAME = 4;
	public static final int REQUEST_INPUT_CAR_ID = 5;
	//メッセージコード
	public static final int MESSAGE_SHOW_TOAST = 1;
	public static final int MESSAGE_ENABLE_GPS = 2;
	//ダイアログID
	public static final int DIALOG_SELECT_AUTOCONTROL_GDL = 1;
	public static final int DIALOG_SELECT_AUTOUPLOAD = 2;

	//アップロードログ
	public static final int NOW_UPLOADING_LOG = 1;
	public static final int LATEST_UPLOADED_LOG = 2;


	/**
	 * 生成時に呼び出される
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

        //レイアウトの取得
        setContentView(R.layout.main);
        versionView = (TextView)findViewById(R.id.version);
        userNameView = (TextView)findViewById(R.id.userName);
        carIDView = (TextView)findViewById(R.id.carID);
        sensorIDView = (TextView)findViewById(R.id.sensorName);
        uploadView = (TextView)findViewById(R.id.uploadView);
        temperatureView = (TextView)findViewById(R.id.temperatureView);
        controlButton = (Button)findViewById(R.id.controlButton);

        //パッケージ情報の取得
        pManager = getPackageManager();
        try {
			pInfo = pManager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			Log.e(DrivingLoggerUI.tag, "そのようなパッケージは存在しません");
		}
		vName = pInfo.versionName;

        //センサの状態ファイルの読み込み
		mInfo = (DeviceInfo)this.getApplication();
//		mInfo = AccessDrivingLoggerStateFile.readStateFile();

        //クリックリスナの登録
        controlButton.setOnClickListener(buttonClickListener);

        //GPSの有効化（必要な場合）
        LocationManager lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!lManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        	launchGPSOptions();
    }

    /**
     * アクティビティが表示されたとき
     */
    @Override
	public void onResume(){
    	String tag = "UI.onResume";
    	super.onResume();
    	Log.i(tag, "onResume started");
    	mInfo = (DeviceInfo)this.getApplication();
//		mInfo = AccessDrivingLoggerStateFile.readStateFile();
		setLayout(mInfo);

    	slReceiver = new SetLayoutReceiver();
    	this.registerReceiver(slReceiver, slFilter);

    	uiReceiver = new UploadViewReceiver();
        this.registerReceiver(uiReceiver, ulFilter);


        setUploadView(mInfo.getUploadState(),mInfo.getTemperature(),LATEST_UPLOADED_LOG);

    }
    
    /**
     * アクティビティが非表示になったとき
     */
    @Override
	public void onPause(){
    	super.onPause();

    	this.unregisterReceiver(slReceiver);

    	this.unregisterReceiver(uiReceiver);
    }

    //-----------------------------------------------------------
    // 
    //-----------------------------------------------------------
    /**
     * メニューボタンを押されたときに呼び出される<br>
     * option_menuファイルで指定されたアイテムがメニューの中に現れる
     */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    /**
     * onCreateOptionsMenuメソッドで呼び出されたメニューの<br>
     * アイテムの内どれかが選ばれたときに呼び出される
     */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	mInfo = (DeviceInfo)this.getApplication();
//		mInfo = AccessDrivingLoggerStateFile.readStateFile();
        switch (item.getItemId()) {
        case R.id.sendLogFile:
        	intent = new Intent(this, UploadLog.class);
        	intent.putExtra(Key.KEY_START_TIME, System.currentTimeMillis());
        	intent.putExtra("process","manual");
        	startService(intent);
        	return true;
        case R.id.autoControlGDL:
        	showDialog(DIALOG_SELECT_AUTOCONTROL_GDL);
        	return true;
        case R.id.autoUpload:
        	showDialog(DIALOG_SELECT_AUTOUPLOAD);
        	return true;
        case R.id.defineUserAndCarID:
        	intent = new Intent(this, SetUserAndCarID.class);
        	startActivity(intent);
        	return true;
        }
        return false;
    }

    /**
     * showDialogメソッドで指定されたIDののダイアログを取得するメソッド
     */
    @Override
	protected Dialog onCreateDialog(int id){
    	CharSequence[] items = {"はい", "いいえ"};
    	int checkedItem;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch(id){
		case DIALOG_SELECT_AUTOCONTROL_GDL:
			if(mInfo.isAutoControlGDL())
				checkedItem = 0;
			else
				checkedItem = 1;
			return builder.setTitle("ロギングの自動化をしますか？").setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(which == 0){
						mInfo.setAutoControlGDL(DeviceInfo.yes);
						mHandler.obtainMessage(MESSAGE_SHOW_TOAST, "自動ロギング機能をオンにしました").sendToTarget();
					}else{
						mInfo.setAutoControlGDL(DeviceInfo.no);
						mHandler.obtainMessage(MESSAGE_SHOW_TOAST, "自動ロギング機能をオフにしました").sendToTarget();
					}
				}
			}).create();
		case DIALOG_SELECT_AUTOUPLOAD:
			if(mInfo.isAutoUpload())
				checkedItem = 0;
			else
				checkedItem = 1;

			return builder.setTitle("アップロードの自動化をしますか？")
			.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(which == 0){
						mInfo.setAutoUpload(DeviceInfo.yes);
						mHandler.obtainMessage(MESSAGE_SHOW_TOAST, "自動アップロード機能をオンにしました").sendToTarget();
					}else{
						mInfo.setAutoUpload(DeviceInfo.no);
						mHandler.obtainMessage(MESSAGE_SHOW_TOAST, "自動アップロード機能をオフにしました").sendToTarget();
					}
				}
			}).create();
		}
		return null;
    }

    /**
     * DeviceInfoインスタンスの設定に従ってレイアウトを更新するメソッド
     * @param di DeviceInfoのオブジェクト
     */
    private void setLayout(DeviceInfo di){
    	//mInfo = (DeviceInfo)this.getApplication();
    	String tag = "UI.setLayout";
    	Log.i(tag, "setLayout started");
    	versionView.setText("Version:" + vName);
    	userNameView.setText("UserID:" + di.getUserID());
    	carIDView.setText("CarID:" + di.getCarID());
    	sensorIDView.setText("SensorID:" + di.getSensorID());
    	//uploadView.setText(mInfo.getUploadState());
    	//temperatureView.setText(mInfo.getTemperature());

    	if(di.getState().equals(DeviceInfo.noLogging)){
    		Log.i(tag, "ボタンのテキストを「ロギング開始」に設定しました");
    		controlButton.setText(textStartLogging);
    	}else if(di.getState().equals(DeviceInfo.underLogging)){
    		Log.i(tag, "ボタンのテキストを「ロギング終了」に設定しました");
    		controlButton.setText(textEndLogging);
    	}else if(di.getState().equals(DeviceInfo.waitLogging)){
    		//di.setState(DeviceInfo.noLogging);
    		controlButton.setText(textWaitLogging);
    	}else if(controlButton.getText().equals("Button")){
    		di.setState(DeviceInfo.noLogging);
    		controlButton.setText(textStartLogging);
    	}else{
    		Log.e(tag, "Stateの値にエラーがあります");
    	}
    }

    /**
     * アップロード状況を画面に表示する
     * @param str アップロードの状況(ネットワーク接続中やアップロード完了など)
     * @param temperature その時点における端末内部温度
     * @param state 呼び出された時の状況
     */
	private void setUploadView(String str, String temperature,int state)
	{
		if(state == NOW_UPLOADING_LOG){
			uploadView.setText("現在のアップロード状況:" + TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + str);
			temperatureView.setText("現在の電池温度:" + TimeStamp.getSplitedTimeStringFromMonthToSecond() + "  " + temperature);
		}else if(state == LATEST_UPLOADED_LOG){
			uploadView.setText("最近のアップロード状況:" + str);
			temperatureView.setText("アップロード時の電池温度:" + temperature);
		}
	}

	/**
	 * ロギングボタンを押したときの挙動を示すインスタンス
	 */
    private OnClickListener buttonClickListener = new OnClickListener() {
		public void onClick(View v) {
			Intent GDLIntent = new Intent(DrivingLoggerUI.this, GetDrivingLog.class);

			if(mInfo.getState().equals(DeviceInfo.noLogging)){
				long startTime = System.currentTimeMillis();
				//mInfo.setStartTime(startTime);

				GDLIntent.putExtra(Key.KEY_START_TIME, startTime);
				GDLIntent.putExtra(Key.KEY_LOGGING_STATE, mInfo.getState());
				startService(GDLIntent);

				//状態ファイルの更新
				mInfo.setState(DeviceInfo.underLogging);
				sendBroadcast(new Intent(Action.ACTION_SET_LAYOUT));
			}else{
				stopService(GDLIntent);

				//状態ファイルの更新
				mInfo.setState(DeviceInfo.noLogging);
				sendBroadcast(new Intent(Action.ACTION_SET_LAYOUT));
			}
		}
	};

	/**
	 * 別クラスからトーストを出現させたいときに<br>
	 * そのメッセージを受け取るインスタンス
	 */
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
				case MESSAGE_SHOW_TOAST:
					Toast.makeText(DrivingLoggerUI.this, (String)msg.obj, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_ENABLE_GPS:
					startActivityForResult((Intent) msg.obj, REQUEST_ENABLE_GPS);
					break;
			}
		}
	};

	/**
	 * GPS設定画面を呼び出すメソッド
	 */
	private void launchGPSOptions() {
        final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, REQUEST_ENABLE_GPS);
    }


	/**
	 * PowerConnectedReceiverクラスから送信されるブロードキャストを受信するクラス<br>
	 * レイアウト更新を実行する
	 * @author kouno 1.0
	 * @version 1.0 初期実装
	 */
	public class SetLayoutReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
//			mInfo = AccessDrivingLoggerStateFile.readStateFile();

			setLayout(mInfo);
		}
	}

	/**
	 * UploadLogクラスからのブロードキャストを受信するクラス
	 * @author hagimoto 1.0
	 * @version 1.0 初期実装
	 */
	public class UploadViewReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
		    String message = bundle.getString("message");
		    String temperature = bundle.getString("temperature");

			setUploadView(message,temperature,NOW_UPLOADING_LOG);
		}
	}

	/*public static boolean ping(InetAddress add){
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        String addstr = add.toString().substring(1);
        try{
            proc = runtime.exec("ping -c 5 " + addstr);
            proc.waitFor();
        }catch(Exception e){
        	e.printStackTrace();
        }
        int exitVal = proc.exitValue();
        if(exitVal == 0)return true;
        else return false;
    }*/
}