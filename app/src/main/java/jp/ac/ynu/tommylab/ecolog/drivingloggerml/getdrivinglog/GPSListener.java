package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * GPSの値を取得するためのListenerを用意したクラス
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto追加 出力項目の追加
 * @version 1.1
 */
public class GPSListener implements LocationListener {
	GetDrivingLog gdl;

	/**
	 * コンストラクタ
	 * @param gdl
	 */
	public GPSListener(GetDrivingLog gdl){
		this.gdl = gdl;
	}
	
	/**
	 * GPSの値が変化したときに呼び出される実際には最少設定にとき1Hzの間隔で出力
	 */
	public void onLocationChanged(Location location) {
		long androidTime = System.currentTimeMillis();
		//gdl.addGPSLog(location.getTime(), androidTime, location.getLatitude(), location.getLongitude(), location.getAltitude());
		gdl.addGPSLog(location.getTime(), androidTime, location.getLatitude(), location.getLongitude(), location.getAltitude(),location.getAccuracy());
	}

	public void onProviderDisabled(String provider) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public void onProviderEnabled(String provider) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO 自動生成されたメソッド・スタブ

	}

}
