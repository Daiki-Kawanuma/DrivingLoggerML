package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import java.util.Calendar;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;

/**
 * GPSのデータをGPS時刻、内部時刻、緯度、経度、高度、精度の構造体として扱うためのクラス
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto追加 項目の追加
 * @version 1.1
 */
public class GPSLogData {
	private Calendar tempGPSTime;
	private Calendar tempPhoneTime;
	private double latitude;
	private double longitude;
	private double altitude;
	private double accuracy;
	private int satellites;

	private boolean accuracyFlag = false;
	private boolean satellitesFlag = false;

	/**
	 * コンストラクタ
	 * @param GPSTime GPS時刻
	 * @param PhoneTime 内部時刻
	 * @param latitude 緯度
	 * @param longitude 経度
	 * @param altitude 高度
	 */
	public GPSLogData(long GPSTime, long PhoneTime, double latitude, double longitude, double altitude){
		tempGPSTime = Calendar.getInstance();
		tempGPSTime.setTimeInMillis(GPSTime);
		tempPhoneTime = Calendar.getInstance();
		tempPhoneTime.setTimeInMillis(PhoneTime);
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	/**
	 * コンストラクタ
	 * @param GPSTime GPS時刻
	 * @param PhoneTime 内部時刻
	 * @param latitude 緯度
	 * @param longitude 経度
	 * @param altitude 高度
	 * @param accuracy 精度
	 */
	public GPSLogData(long GPSTime, long PhoneTime, double latitude, double longitude, double altitude, double accuracy){
		tempGPSTime = Calendar.getInstance();
		tempGPSTime.setTimeInMillis(GPSTime);
		tempPhoneTime = Calendar.getInstance();
		tempPhoneTime.setTimeInMillis(PhoneTime);
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.accuracy = accuracy;

		accuracyFlag = true;
	}

	/**
	 * コンストラクタ
	 * @param GPSTime GPS時刻
	 * @param PhoneTime 内部時刻
	 * @param latitude 緯度
	 * @param longitude 経度
	 * @param altitude 高度
	 * @param accuracy 精度
	 * @param satellites 衛星数(常に255が出力される)
	 */
	public GPSLogData(long GPSTime, long PhoneTime, double latitude, double longitude, double altitude, double accuracy, int satellites){
		tempGPSTime = Calendar.getInstance();
		tempGPSTime.setTimeInMillis(GPSTime);
		tempPhoneTime = Calendar.getInstance();
		tempPhoneTime.setTimeInMillis(PhoneTime);
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.accuracy = accuracy;
		this.satellites = satellites;

		accuracyFlag = true;
		satellitesFlag = true;
	}

	/**
	 * toStringの拡張
	 * @return GPS時刻,内部時刻,緯度,経度,高度(,精度,衛星数)\r\n ()内はどのコンストラクタで生成したかで変化する
	 */
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder(100);

		s.append(TimeStamp.getSplitedTimeStringFromYearTilllMillis(tempGPSTime));
		s.append(",");
		s.append(TimeStamp.getSplitedTimeStringFromYearTilllMillis(tempPhoneTime));
		s.append(",");
		s.append(latitude);
		s.append(",");
		s.append(longitude);
		s.append(",");
		s.append(altitude);

		if(accuracyFlag == true)
		{
			s.append(",");
			s.append(accuracy);
		}

		if(satellitesFlag == true)
		{
			s.append(",");
			s.append(satellites);
		}

		s.append("\r\n");

		return s.toString();
	}
}
