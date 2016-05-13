package jp.ac.ynu.tommylab.ecolog.drivingloggerml.setuserandcarid;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.DeviceInfo;
import jp.ac.ynu.tommylab.ecolog.drivingloggerml.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * ユーザのIDを設定するためのアクティビティを提供するクラス
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto修正 センサの種類をIDとして追加、空白文字の入力を許可しない(Windows環境でバグが起こる)
 * @version 1.1
 */
public class SetUserAndCarID extends Activity {
	public static final String KEY_TITLE = "TITLE";
	public static final String KEY_ID = "ID";
	public static final String KEY_TEMP_ID = "TEMP_ID";
	public static final int DIALOG_INVALID_CHAR_IN_USER_ID = 1;
	public static final int DIALOG_INVALID_CHAR_IN_CAR_ID = 2;
	public static final int DIALOG_INVALID_CHAR_IN_SENSOR_ID = 3;

	EditText edUserID;
	EditText edCarID;
	EditText edSensorID;
	DeviceInfo dInfo;

	/**
	 * クラス生成時に呼び出される
	 */
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.input_dialog);

		dInfo = (DeviceInfo)this.getApplication();

		edUserID = (EditText) findViewById(R.id.userid_input);
		edUserID.setText(dInfo.getUserID());
		edUserID.setOnFocusChangeListener(ofcListener);

		edCarID = (EditText) findViewById(R.id.carid_input);
		edCarID.setText(dInfo.getCarID());
		edCarID.setOnFocusChangeListener(ofcListener);

		edSensorID = (EditText) findViewById(R.id.sensorid_input);
		edSensorID.setText(dInfo.getSensorID());
		edSensorID.setOnFocusChangeListener(ofcListener);

		Button button = (Button) findViewById(R.id.finish_button);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean isError = false;
				if(!checkForbiddenCharacter(edUserID)){
					showDialog(DIALOG_INVALID_CHAR_IN_USER_ID);
					isError = true;
				}
				if(!checkForbiddenCharacter(edCarID)){
					showDialog(DIALOG_INVALID_CHAR_IN_CAR_ID);
					isError = true;
				}
				if(!checkForbiddenCharacter(edSensorID)){
					showDialog(DIALOG_INVALID_CHAR_IN_SENSOR_ID);
					isError = true;
				}

				if(!isError){
					dInfo.setUserID(edUserID.getText().toString());
					dInfo.setCarID(edCarID.getText().toString());
					dInfo.setSensorID(edSensorID.getText().toString());
					finish();
				}
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(id == DIALOG_INVALID_CHAR_IN_USER_ID){
			builder.setMessage("UserIDにディレクトリ名として使用不可能な文字が入っています。修正してください")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
		}
		else if(id == DIALOG_INVALID_CHAR_IN_CAR_ID){
			builder.setMessage("CarIDにディレクトリ名として使用不可能な文字が入っています。修正してください")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
		}
		else if(id == DIALOG_INVALID_CHAR_IN_SENSOR_ID){
			builder.setMessage("SensorIDにディレクトリ名として使用不可能な文字が入っています。修正してください")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
		}

		return builder.create();
	}

	private View.OnFocusChangeListener ofcListener = new View.OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			InputMethodManager imManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if(hasFocus)
				imManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
			else
				imManager.hideSoftInputFromWindow(v.getWindowToken(),0);
		}
	};

	/**
	 * Windowsのフォルダ名として使用できない文字を検出する
	 * フォルダ名として使用できるならtrue, できないならfalseを返す
	 * @param v ユーザの入力項目
	 * @return bool 正否の結果
	 */
	private boolean checkForbiddenCharacter(TextView v){
		String s = v.getText().toString();
		return ((s.indexOf(":") == -1) &&
						(s.indexOf(";") == -1) &&
						(s.indexOf("/") == -1) &&
						(s.indexOf("\\") == -1) &&
						(s.indexOf("|") == -1) &&
						(s.indexOf(",") == -1) &&
						(s.indexOf("*") == -1) &&
						(s.indexOf("?") == -1) &&
						(s.indexOf("\"") == -1) &&
						(s.indexOf("<") == -1) &&
						(s.indexOf(">") == -1) &&
						!s.startsWith(".")) &&
						!s.endsWith(" ") &&
						!s.endsWith("　");
	}
}
