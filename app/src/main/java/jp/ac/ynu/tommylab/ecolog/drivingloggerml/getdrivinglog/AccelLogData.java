package jp.ac.ynu.tommylab.ecolog.drivingloggerml.getdrivinglog;

import java.util.Calendar;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.TimeStamp;

/**
 * 加速度センサのデータを時刻、X軸、Y軸、Z軸の構造体として扱うためのクラス
 * @author 1.0 kouno作成
 * @version 1.0
 */
public class AccelLogData {
	private Calendar tempTime;
	private float accelX;
	private float accelY;
	private float accelZ;

	/**
	 * コンストラクタ
	 * @param d
	 * @param x
	 * @param y
	 * @param z
	 */
	public AccelLogData(long d, float x, float y, float z){
		tempTime = Calendar.getInstance();
		tempTime.setTimeInMillis(d);
		accelX = x;
		accelY = y;
		accelZ = z;
	}

	/**
	 * toStringの拡張
	 * @return temptime,accelX,accelY,accelZ\r\n
	 */
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder(100);

		s.append(TimeStamp.getSplitedTimeStringFromYearTilllMillis(tempTime));
		s.append(",");
		s.append(accelX);
		s.append(",");
		s.append(accelY);
		s.append(",");
		s.append(accelZ);
		s.append("\r\n");

		return s.toString();
	}
}
