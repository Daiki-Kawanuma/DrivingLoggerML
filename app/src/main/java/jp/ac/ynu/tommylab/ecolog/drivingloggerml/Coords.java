package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

/**
 * 緯度、経度の距離計算用のクラス
 * @author 1.0 kouno作成
 * @version 1.0
 */
public class Coords {
	  //ベッセル楕円体(旧日本測地系)
	  public static final double BESSEL_A = 6377397.155;
	  public static final double BESSEL_E2 = 0.00667436061028297;
	  public static final double BESSEL_MNUM = 6334832.10663254;

	  //GRS80(世界測地系)
	  public static final double GRS80_A = 6378137.000;
	  public static final double GRS80_E2 = 0.00669438002301188;
	  public static final double GRS80_MNUM = 6335439.32708317;

	  //WGS84(GPS)
	  public static final double WGS84_A = 6378137.000;
	  public static final double WGS84_E2 = 0.00669437999019758;
	  public static final double WGS84_MNUM = 6335439.32729246;

	  public static final int BESSEL = 0;
	  public static final int GRS80 = 1;
	  public static final int WGS84 = 2;

	  /**
	   * 角度をラジアン値に変換
	   * @param deg 角度
	   * @return ラジアン値
	   */
	  public static double deg2rad(double deg){
		return deg * Math.PI / 180.0;
	  }

	  /**
	   * ヒュベニの計算法による緯度経度の距離計算
	   * @param lat1 地点1の緯度
	   * @param lng1 地点1の経度
	   * @param lat2 地点2の緯度
	   * @param lng2 地点2の経度
	   * @param a 長半径(赤道半径)(定数)
	   * @param e2 第1離心率の2乗(定数)
	   * @param mnum a(1-e2)の値(定数)
	   * @return 2点間の距離
	   */
	  public static double calcDistHubeny(double lat1, double lng1,
	                                      double lat2, double lng2,
	                                      double a, double e2, double mnum){
	    double my = deg2rad((lat1 + lat2) / 2.0);
	    double dy = deg2rad(lat1 - lat2);
	    double dx = deg2rad(lng1 - lng2);

	    double sin = Math.sin(my);
	    double w = Math.sqrt(1.0 - e2 * sin * sin);
	    double m = mnum / (w * w * w);
	    double n = a / w;

	    double dym = dy * m;
	    double dxncos = dx * n * Math.cos(my);

	    return Math.sqrt(dym * dym + dxncos * dxncos);
	  }

	  /**
	   * ヒュベニの計算法による緯度経度の距離計算
	   * @param lat1 地点1の緯度
	   * @param lng1 地点1の経度
	   * @param lat2 地点2の緯度
	   * @param lng2 地点2の経度
	   * @return 2点間の距離
	   */
	  public static double calcDistHubeny(double lat1, double lng1,
	                                      double lat2, double lng2){
	    return calcDistHubeny(lat1, lng1, lat2, lng2,
	                          GRS80_A, GRS80_E2, GRS80_MNUM);
	  }

	  /**
	   * ヒュベニの計算法による緯度経度の距離計算
	   * @param lat1 地点1の緯度
	   * @param lng1 地点1の経度
	   * @param lat2 地点2の緯度
	   * @param lng2 地点2の経度
	   * @param type 計算に使用する地球の楕円体モデル
	   * @return 2点間の距離
	   */
	  public static double calcDistHubery(double lat1, double lng1,
	                                      double lat2, double lng2, int type){
	    switch(type){
	      case BESSEL:
	        return calcDistHubeny(lat1, lng1, lat2, lng2,
	                              BESSEL_A, BESSEL_E2, BESSEL_MNUM);
	      case WGS84:
	        return calcDistHubeny(lat1, lng1, lat2, lng2,
	                              WGS84_A, WGS84_E2, WGS84_MNUM);
	      default:
	        return calcDistHubeny(lat1, lng1, lat2, lng2,
	                              GRS80_A, GRS80_E2, GRS80_MNUM);
	     }
	  }
}
