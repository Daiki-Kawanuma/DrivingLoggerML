package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

import java.util.Calendar;

/**
 * 日付・時刻をString型で提供する
 * @author 1.0 kouno作成<br>
 * @version 1.0
 */
public class TimeStamp {

	/**
	 * 引数のlong値からyyyymmddhhMMssの形式でStringを抜き出すメソッド
	 * @param tempTime long型の日付・時刻
	 * @return String型のyyyymmddhhMMss
	 */
	public static String getTimeString(long tempTime){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(tempTime);

		StringBuilder s = new StringBuilder();

		s.append(Integer.toString(cal.get(Calendar.YEAR)));
		if(cal.get(Calendar.MONTH) + 1 < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MONTH) + 1));
		if(cal.get(Calendar.DATE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.DATE)));
		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));

		return s.toString();
	}

	/**
	 * 引数のCalenderオブジェクトからyyyymmddhhMMssの形式でStringを抜き出すメソッド
	 * @param cal 時刻情報のCalenderオブジェクト
	 * @return String型のyyyymmddhhMMss
	 */
	public static String getTimeString(Calendar cal){
		StringBuilder s = new StringBuilder();

		s.append(Integer.toString(cal.get(Calendar.YEAR)));
		if(cal.get(Calendar.MONTH) + 1 < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MONTH) + 1));
		if(cal.get(Calendar.DATE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.DATE)));
		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));

		return s.toString();
	}

	//-----------------------------------------------------
	// public static String getTimeString()
	// 現在時刻から
	// yyyymmddhhMMssの形式でStringインスタンスを取得するメソッド
	//-----------------------------------------------------
	//public static String getTimeString(){
	//	return getTimeString(System.currentTimeMillis());
	//}

	/**
	 * 現在時刻からyyyy-mm-dd hh:MM:ss.SSS形式でStringインスタンスを取得するメソッド<br>
	 * 23文字
	 * @return String型のyyyy-mm-dd hh:MM:ss.SSS
	 */
	public static String getSplitedTimeStringFromYearTilllMillis(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());

		StringBuilder s = new StringBuilder();

		s.append(Integer.toString(cal.get(Calendar.YEAR)));
		s.append("-");

		if(cal.get(Calendar.MONTH) + 1 < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MONTH) + 1));
		s.append("-");

		if(cal.get(Calendar.DATE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.DATE)));
		s.append(" ");

		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		s.append(":");

		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		s.append(":");

		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));
		s.append(".");

		if(cal.get(Calendar.MILLISECOND) < 10)
			s.append("00");
		else if(cal.get(Calendar.MILLISECOND) < 100)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MILLISECOND)));

		return s.toString();
	}

	/**
	 * 引数のCalendarオブジェクトからyyyy-mm-dd hh:MM:ss.SSS形式でStringインスタンスを取得するメソッド<br>
	 * 23文字
	 * @param cal 時刻を持つCalendarオブジェクト
	 * @return String型のyyyy-mm-dd hh:MM:ss.SSS
	 */
	public static String getSplitedTimeStringFromYearTilllMillis(Calendar cal){
		StringBuilder s = new StringBuilder();

		s.append(Integer.toString(cal.get(Calendar.YEAR)));
		s.append("-");

		if(cal.get(Calendar.MONTH) + 1 < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MONTH) + 1));
		s.append("-");

		if(cal.get(Calendar.DATE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.DATE)));
		s.append(" ");

		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		s.append(":");

		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		s.append(":");

		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));
		s.append(".");

		if(cal.get(Calendar.MILLISECOND) < 10)
			s.append("00");
		else if(cal.get(Calendar.MILLISECOND) < 100)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MILLISECOND)));

		return s.toString();
	}

	/**
	 * 現在時刻からhhmmssMMMの形式でStringインスタンスを取得するメソッド
	 * @return String型のhhmmssMMM
	 */
	public static String getTimeStringTillMillis(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());

		StringBuilder s = new StringBuilder();

		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));
		if(cal.get(Calendar.MILLISECOND) < 10)
			s.append("00");
		else if(cal.get(Calendar.MILLISECOND) < 100)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MILLISECOND)));

		return s.toString();
	}

	/**
	 * 現在時刻からmm/dd hh:MM:ss形式でStringインスタンスを取得するメソッド
	 * @return String型のmm/dd hh:MM:ss
	 */
	public static String getSplitedTimeStringFromMonthToSecond(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());

		StringBuilder s = new StringBuilder();

		if(cal.get(Calendar.MONTH) + 1 < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MONTH) + 1));
		s.append("/");

		if(cal.get(Calendar.DATE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.DATE)));
		s.append(" ");

		if(cal.get(Calendar.HOUR_OF_DAY) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		s.append(":");

		if(cal.get(Calendar.MINUTE) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.MINUTE)));
		s.append(":");

		if(cal.get(Calendar.SECOND) < 10)
			s.append("0");
		s.append(Integer.toString(cal.get(Calendar.SECOND)));

		return s.toString();
	}
}