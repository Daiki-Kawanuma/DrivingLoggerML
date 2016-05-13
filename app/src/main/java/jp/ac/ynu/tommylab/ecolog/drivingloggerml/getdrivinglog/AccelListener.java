package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * 加速度センサの値を取得するためのListenerを用意したクラス
 * @author 1.0 kouno作成 
 * @version 1.0
 */
public class AccelListener implements SensorEventListener {
	GetDrivingLog gdl;

	/**
	 * コンストラクタ
	 * @param gdl
	 */
	public AccelListener(GetDrivingLog gdl){
		this.gdl = gdl;
	}
	
	/**
	 * 精度が変化したとき(どのタイミングで呼び出されるか不明)
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO 自動生成されたメソッド・スタブ

	}

	/**
	 * センサ値が変化したとき(加速度のmanager生成時に指定した変数や機種によって出力間隔が変わる)
	 */
	public void onSensorChanged(SensorEvent event) {
		gdl.addAccelLog(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]);
		//gdl.addAccelLog(event.timestamp/1000, event.values[0], event.values[1], event.values[2]);
	}
}
