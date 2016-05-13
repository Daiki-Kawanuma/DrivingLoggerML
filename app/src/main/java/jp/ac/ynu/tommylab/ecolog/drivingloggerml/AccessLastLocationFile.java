package jp.ac.ynu.tommylab.ecolog.drivingloggerml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import jp.ac.ynu.tommylab.ecolog.drivingloggerml.constant.DirectoryTree;
import android.location.Location;
import android.location.LocationManager;

/**
 * トリップの到着地点のログを管理するクラス
 * 
 * 現在は総合研究等の前の駐車場でロギングを停止したときのみ<br>
 * ログのアップロードを行う<br>
 * そのときの到着地点を判断するためのログをの残す
 * @author 1.0 kouno作成<br>
 * 1.1 hagimoto修正 resetLocationFile()を追加
 * @version 1.1 <br>
 */
public class AccessLastLocationFile {

	/**
	 * トリップの終着地点の緯度、経度を読み込む
	 * @return 緯度、経度を含むLocation型を返す
	 */
	public static Location readLastLocationFile(){
		//String tag = "AccessLastLocationFile.readLastLocationFile";
		Location location = new Location(LocationManager.GPS_PROVIDER);

		try {
			BufferedReader bReader = new BufferedReader(new FileReader(DirectoryTree.FILE_LASTLOCATIONFILE));

			String latitude = bReader.readLine();
			String longitude = bReader.readLine();
			bReader.close();

			if(latitude != null){
				location.setLatitude(Double.parseDouble(latitude));
			}
			if(longitude != null){
				location.setLongitude(Double.parseDouble(longitude));
			}

		} catch (FileNotFoundException e) {
			//状態ファイルが存在しない場合は新しくファイルを作って、仮のデバイス情報を書き込む
			//Log.e(tag, "状態ファイルが見つかりません");

			DirectoryTree.DIRECTORY_ECOLOG_CONFIG.mkdirs();
			writeLastLocationFile(location);
		} catch (IOException e) {
			//Log.e(tag, "状態ファイルが読み込めません");

			writeLastLocationFile(location);
		}
		return location;
	}

	/**
	 * 引数のLocation型の中の緯度、経度をファイルに書き込む
	 * @param location ログ取得終了時の緯度、経度
	 * @return 書き込みの成否
	 */
	public static boolean writeLastLocationFile(Location location){
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(DirectoryTree.FILE_LASTLOCATIONFILE));

			bWriter.write(location.getLatitude() + "\r\n");
			bWriter.write(location.getLongitude() + "\r\n");

			bWriter.flush();
			bWriter.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * 到着地点のデータをリセットする
	 * (最後の地点情報を取得するメソッドLocationManager.getLastKnownLocationはremoveUpdatesをしても初期化されないため
	 * このメソッドの意味はなくなっている)
	 * @return リセットの成否
	 */
	public static boolean resetLocationFile(){
		try {
			double defaltLatitude = 0;
			double defaltLongitude = 0;

			BufferedWriter bWriter = new BufferedWriter(new FileWriter(DirectoryTree.FILE_LASTLOCATIONFILE));

			bWriter.write(defaltLatitude + "\r\n");
			bWriter.write(defaltLongitude + "\r\n");

			bWriter.flush();
			bWriter.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
